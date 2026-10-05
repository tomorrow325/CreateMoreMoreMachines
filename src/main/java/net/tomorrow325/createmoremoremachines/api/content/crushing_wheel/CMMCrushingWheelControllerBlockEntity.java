package net.tomorrow325.createmoremoremachines.api.content.crushing_wheel;

import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.tomorrow325.createmoremoremachines.CMMMConfig;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * Tiered crushing wheel controller block entity.
 *
 * <p>Two tier behaviours are layered on top of the untouched Create controller mechanism (tick
 * countdown, recipe application, belt export and entity intake are all inherited):
 * <ul>
 * <li><b>Speed</b> - {@link #findRecipe()} wraps the vanilla recipe in an anonymous
 * {@link StandardProcessingRecipe} whose {@code getProcessingDuration()} is divided by the
 * tier's processing multiple, shortening the crushing cycle while keeping outputs identical.
 * The wrapper must additionally implement the four abstract members left unimplemented by
 * {@code StandardProcessingRecipe} itself (Create 6.0.10): {@link #matches(RecipeWrapper, Level)}
 * (declared abstract on {@code Recipe}), {@link #getMaxInputCount()} and
 * {@link #getMaxOutputCount()} (declared protected abstract on {@code ProcessingRecipe}, only
 * implemented in the crushing subtypes), plus the {@code getProcessingDuration()} override.
 * The two protected counts are safe as dead-code constants: the Create 6.0.10
 * {@code StandardProcessingRecipe} constructor is a plain field copy that never invokes them,
 * and the controller path only calls {@code getProcessingDuration} and {@code rollResults}.</li>
 * <li><b>Parallel batches</b> - the vanilla controller already consumes the whole slot 0 stack
 * in one cycle ({@code applyRecipe} rolls once per input item), so the tier batch is expressed
 * as the slot 0 capacity: {@code min(processingMultiple, MAX_BATCH)} items of one kind
 * (further limited by the item's max stack size through
 * {@code ItemStackHandler#getStackLimit}); tiers without a configured multiple (field default
 * 1) keep the vanilla capacity baseline. Dropped item entities are only tracked while the
 * controller is idle ({@code checkEntityForProcessing} gates on {@code !isOccupied()}), so a
 * batch sitting in slot 0 can never be destroyed by an entity intake - the entity waits until
 * the outputs have been exported. Such an entity stack enters whole via
 * {@code setStackInSlot} (vanilla semantics), bypassing the tier cap, so the tier batch size
 * describes the belt/hopper input path.</li>
 * </ul>
 *
 * <p>Both multiples (the batch size above and the speed division in the bullet before it) are
 * resolved through {@code CMMMConfig} instead of being read straight from the tier: its
 * per-tier addon entries default to {@code -1} = follow the tier's CMM processing multiple
 * (the un-overridden behaviour), while an explicit value of 2 or above overrides it.
 *
 * <p>The vanilla apply threshold is {@code remainingTime < 20} while the countdown loses up to
 * 20 per tick (the clamp maximum of {@code speed * 4 / log2(count)}), so a divided duration of
 * {@code &le; 20} would fire {@code applyRecipe} on the tick right after the insertion and
 * reduce every batch to whatever arrived in a single tick. {@link #MIN_CYCLE_DURATION} floors
 * the divided duration so a batch always keeps a countdown window of
 * {@code (MIN_CYCLE_DURATION - 20) / 20} ticks during which further same-item inputs keep
 * merging into slot 0.
 *
 * <p>The inventory field is replaced with a tier-aware {@link ProcessingInventory} in the
 * constructor. Because Create's private {@code wrapper} field binds the inventory instance
 * created by the superclass field initializer, it would keep wrapping the discarded instance
 * (always empty, so {@code findRecipe} would return empty and {@code applyRecipe}'s else branch
 * would silently destroy the input); {@link #findRecipe()} therefore queries through a
 * self-managed {@link #cmmWrapper} that wraps the live inventory. The insertion callback cannot
 * reference the private vanilla {@code itemInserted} either, so {@link #cmmItemInserted}
 * replicates its three statements verbatim; the vanilla method itself (still called by the
 * private {@code intakeItem} entity path) produces the same values because it routes through
 * this override of {@code findRecipe()}.
 */
public class CMMCrushingWheelControllerBlockEntity extends CrushingWheelControllerBlockEntity {
    /**
     * Upper bound of one processing batch, mirroring CMM's own clamp of the deployer processing
     * multiple ({@code CMMBeltDeployerCallbacks} uses {@code Math.min(multiple, 64)}), and equal
     * to the worst-case batch vanilla itself ships with {@code bulkCutting = true}. The merged
     * results fit the 31 output slots of {@code ProcessingInventory} for every recipe whose
     * per-result volume satisfies {@code ceil(rolls * count_i / maxStackSize_i)} summed over all
     * results (plus crafting-remainder stacks) &le; 31 - true for every Create-shipped crushing
     * recipe (at most 7 results, count 1-2). Datapack recipes with many large-count results can
     * exceed 31 stacks; vanilla's output-writing loop then drops the surplus silently, exactly
     * as it does for its own {@code bulkCutting} batches.
     */
    public static final int MAX_BATCH = 64;

    /**
     * Lower bound of the divided cycle duration. The controller applies the recipe once
     * {@code remainingTime < 20} and the countdown loses up to 20 per tick (the clamp maximum),
     * so 60 leaves at least two ticks of batch-accumulation window before the recipe fires
     * while still running visibly faster than the vanilla baseline of 100. The dividing
     * multiple is the {@code CMMMConfig} speed resolver (default {@code -1} = the tier's CMM
     * processing multiple); this floor itself stays fixed.
     */
    public static final int MIN_CYCLE_DURATION = 60;

    private final CMMTier tier;
    private final RecipeWrapper cmmWrapper;

    public CMMCrushingWheelControllerBlockEntity(CMMTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
        // Replace the vanilla inventory (anonymous ProcessingInventory bound to the private
        // itemInserted and to the original field initializer order) with the tier-aware one.
        // The field is public and every Create code path (capability lookup, belt insertion,
        // entity intake, tick, NBT) reads it through the field, so the replacement is picked up
        // everywhere. isItemValid and the callback replicate the vanilla behaviours, extended
        // with the same-item merge that lets a stream of single belt items form a batch.
        this.inventory = new ProcessingInventory(this::cmmItemInserted) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot != 0 || processingEntity != null)
                    return false;
                ItemStack current = getStackInSlot(0);
                if (!current.isEmpty())
                    return ItemStack.isSameItemSameComponents(stack, current);
                return isEmpty();
            }

            @Override
            public int getSlotLimit(int slot) {
                // The vanilla controller never calls withSlotLimit, so its slot 0 capacity is
                // the ItemStackHandler default of 99, further capped by the item's max stack
                // size through getStackLimit. A tier resolving to a multiple <= 1 (unconfigured
                // custom tiers, or an explicit addon value below 2) must keep that vanilla
                // baseline instead of being squeezed down to a single item per insertion;
                // configured multiples (>= 2, floored at 64) express the tier batch as the slot
                // 0 capacity. The multiple comes from CMMMConfig's resolver, whose default
                // (-1) follows the tier's CMM processing multiple.
                int multiple = CMMMConfig.crushingWheelProcessingMultiple(tier);
                if (slot == 0 && multiple > 1)
                    return Math.min(multiple, MAX_BATCH);
                return super.getSlotLimit(slot);
            }
        };
        // Must be created after the replacement: the vanilla private wrapper still points at
        // the discarded inventory instance.
        this.cmmWrapper = new RecipeWrapper(this.inventory);
    }

    public CMMTier getTier() {
        return tier;
    }

    /**
     * Verbatim replica of the vanilla private {@code itemInserted}: find the (tiered) recipe
     * and restart the countdown, clearing the applied flag.
     */
    private void cmmItemInserted(ItemStack stack) {
        Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = findRecipe();
        inventory.remainingTime = recipe.isPresent() ? recipe.get().value().getProcessingDuration() : 100.0F;
        inventory.appliedRecipe = false;
    }

    @Override
    public Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> findRecipe() {
        // Mirrors the vanilla lookup (crushing first, milling as fallback), but through the
        // wrapper of the live inventory instead of Create's stale private one.
        Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe =
            AllRecipeTypes.CRUSHING.find(cmmWrapper, level);
        if (recipe.isEmpty())
            recipe = AllRecipeTypes.MILLING.find(cmmWrapper, level);

        // Resolved speed multiple: CMMMConfig's default (-1) follows the tier's CMM processing
        // multiple; values <= 1 keep the vanilla speed and skip the division below.
        int multiple = CMMMConfig.crushingWheelSpeedMultiple(tier);
        if (recipe.isEmpty() || multiple <= 1)
            return recipe;

        RecipeHolder<StandardProcessingRecipe<RecipeWrapper>> holder = recipe.get();
        StandardProcessingRecipe<RecipeWrapper> value = holder.value();
        StandardProcessingRecipe<RecipeWrapper> wrapper = new StandardProcessingRecipe<>(value.getTypeInfo(),
            value.getParams()) {

            @Override
            public int getProcessingDuration() {
                return Math.max(MIN_CYCLE_DURATION, value.getProcessingDuration() / multiple);
            }

            @Override
            public boolean matches(RecipeWrapper input, Level level) {
                return value.matches(input, level);
            }

            @Override
            protected int getMaxInputCount() {
                return 1;
            }

            @Override
            protected int getMaxOutputCount() {
                return 7;
            }
        };
        return Optional.of(new RecipeHolder<>(holder.id(), wrapper));
    }
}
