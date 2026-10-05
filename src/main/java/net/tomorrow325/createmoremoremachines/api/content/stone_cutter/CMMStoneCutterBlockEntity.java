package net.tomorrow325.createmoremoremachines.api.content.stone_cutter;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tomorrow325.createmoremoremachines.CMMMConfig;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * Tiered mechanical stone cutter block entity.
 *
 * <p>Two tier behaviours are layered on top of the untouched Create saw mechanism (recipe
 * lookup, filter, belt/funnel/entity item handling, tree felling are all inherited):
 * <ul>
 * <li><b>Speed</b> - {@link SawBlockEntity#start(ItemStack)} is public and writes
 * {@code inventory.remainingTime} / {@code inventory.recipeDuration} directly, so the tier
 * speed-up is applied right after the super call by dividing both by the tier's processing
 * multiple. {@code super.start} recomputes both from the recipe baseline on every call, so the
 * division never compounds. The client-side early-out of {@code super.start} is mirrored so a
 * non-virtual client never touches the inventory fields.</li>
 * <li><b>Parallel batches</b> - the vanilla saw consumes the whole slot 0 stack in one cycle
 * ({@code SawBlockEntity#applyRecipe} rolls once per input item), but vanilla only lets slot 0
 * receive a single stack of 1: {@code withSlotLimit(!bulkCutting)} caps every slot at 1 while
 * {@code bulkCutting} is false. The tiered inventory instead allows
 * {@code min(processingMultiple, MAX_BATCH)} items in slot 0 (further limited by the item's own
 * max stack size through {@code ItemStackHandler#getStackLimit}), so one cycle processes a
 * tier-sized batch of the same item and yields the same multiplied results.</li>
 * </ul>
 *
 * <p>Both multiples (the speed division in the first bullet and the batch size in the second)
 * are resolved through {@code CMMMConfig} instead of being read straight from the tier: its
 * per-tier addon entries default to {@code -1} = follow the tier's CMM processing multiple
 * (the un-overridden behaviour), while an explicit value of 2 or above overrides it.
 *
 * <p>The vanilla apply threshold is {@code remainingTime < 5}; after the division a cycle can
 * start below that threshold and fire {@code applyRecipe} on the very next tick, which would
 * reduce every batch to whatever arrived in a single tick. {@link #MIN_CYCLE_DURATION} floors
 * the divided duration so a batch always keeps a countdown window of
 * {@code (MIN_CYCLE_DURATION - 5) / processingSpeed} ticks during which further same-item
 * inputs keep merging into slot 0.
 *
 * <p>A creative-tier multiple of {@link Integer#MAX_VALUE} is floored the same way, so even the
 * creative tier runs visible (batched) cycles instead of instant single-item applies.
 */
public class CMMStoneCutterBlockEntity extends SawBlockEntity {
    /**
     * Upper bound of one processing batch, mirroring CMM's own clamp of the deployer processing
     * multiple ({@code CMMBeltDeployerCallbacks} uses {@code Math.min(multiple, 64)}), and equal
     * to the worst-case batch vanilla itself ships with {@code bulkCutting = true}. The merged
     * results fit the 31 output slots of {@code ProcessingInventory} for every recipe whose
     * per-result volume satisfies {@code ceil(rolls * count_i / maxStackSize_i)} summed over all
     * results (plus crafting-remainder stacks) &le; 31 - true for every Create-shipped cutting /
     * stonecutting recipe (at most 4 results, count 1). Datapack recipes with many large-count
     * results can exceed 31 stacks; vanilla's output-writing loop then drops the surplus
     * silently, exactly as it does for its own {@code bulkCutting} batches.
     */
    private static final int MAX_BATCH = 64;

    /**
     * Lower bound of the divided cycle duration. The saw applies the recipe once
     * {@code remainingTime < 5} and the countdown loses at most {@code |speed| / 24} (clamped
     * 1..128, i.e. 10.7 at Create's default 256 RPM cap) per tick, so 25 leaves at least a
     * couple of ticks of batch-accumulation window before the recipe fires. The floor is
     * intentionally fixed rather than speed-scaled: scaling it would make low-tier saws run
     * slower than vanilla on servers with a raised {@code maxRotationSpeed}. On such servers
     * the window shrinks proportionally (the clamp caps the countdown at 128/tick); batches
     * then form mostly from same-tick arrivals, which degrades throughput but never loses
     * items, since same-item inputs keep merging through {@code isItemValid}.
     */
    private static final float MIN_CYCLE_DURATION = 25.0F;

    private final CMMTier tier;

    public CMMStoneCutterBlockEntity(CMMTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
        // Replace the vanilla inventory (created with withSlotLimit(!bulkCutting)) with the
        // tier-aware one. The field is public and every Create code path (capability lookup,
        // belt insertion, NBT, export loop) reads it through the field, so the replacement is
        // picked up everywhere; the callback stays this::start exactly like vanilla.
        this.inventory = new TieredSawInventory();
        // Vanilla's constructor seeds its instance with the idle sentinel
        // (inventory.remainingTime = -1.0F), which tick() checks to decide between "idle" and
        // "export scan". The replacement starts at 0.0F and would therefore run the export
        // branch every tick while a belt/funnel neighbour is present and never return to the
        // idle branch - carry the sentinel over.
        this.inventory.remainingTime = -1.0F;
    }

    public CMMTier getTier() {
        return tier;
    }

    @Override
    public void start(ItemStack inserted) {
        // Mirror the vanilla early-outs: on a non-virtual client side, when the saw cannot
        // process (e.g. placed sideways for tree felling) or when the inventory is empty,
        // super.start() returns before touching the inventory fields - the division and the
        // floor must be skipped in exactly those cases, or they would rescale stale values.
        boolean untouchedBySuper = level.isClientSide && !isVirtual();
        super.start(inserted);
        if (untouchedBySuper || !canProcess() || inventory.isEmpty())
            return;
        // Resolved speed multiple: CMMMConfig's default (-1) follows the tier's CMM processing
        // multiple; values <= 1 keep the vanilla speed and skip the division below.
        int multiple = CMMMConfig.stoneCutterSpeedMultiple(tier);
        if (multiple <= 1)
            return;
        // Divide, then floor. recipeDuration must stay equal to remainingTime because the saw
        // particles use remainingTime / recipeDuration as their progress ratio. The floor also
        // affects the vanilla "no matching recipe" marker (a 10 tick countdown before the input
        // is spat back out), which just means rejected items are returned in batch-sized chunks
        // a few ticks later.
        inventory.recipeDuration = Math.max(MIN_CYCLE_DURATION, inventory.remainingTime / multiple);
        inventory.remainingTime = inventory.recipeDuration;
    }

    /**
     * Slot 0 accepts up to {@code min(processingMultiple, MAX_BATCH)} items of one kind, where
     * the multiple comes from {@code CMMMConfig}'s resolver (default {@code -1} = the tier's
     * CMM processing multiple); the {@code max(…, 1)} keeps unconfigured or misconfigured
     * custom tiers - and an explicit addon value below 2 - at the vanilla saw baseline of one
     * item instead of accepting nothing; slots 1..31 keep the vanilla saw limit
     * of 1 per slot ({@code withSlotLimit(!bulkCutting)} with the default {@code bulkCutting =
     * false}) rather than the {@code ItemStackHandler} default of 99. Outputs are only ever
     * written through {@code setStackInSlot}, which bypasses slot limits, so the slot 1..31
     * limit is a semantic alignment that costs nothing.
     */
    private class TieredSawInventory extends ProcessingInventory {
        private TieredSawInventory() {
            super(CMMStoneCutterBlockEntity.this::start);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0)
                return Math.min(Math.max(CMMMConfig.stoneCutterProcessingMultiple(tier), 1), MAX_BATCH);
            return 1;
        }

        /**
         * Vanilla only accepts input while the whole inventory is empty, which prevents a
         * stream of single belt items from ever forming a batch. Same item + same components
         * may now merge into the running slot 0 batch (the resulting stack growth still goes
         * through {@code ItemStackHandler#insertItem}, which respects the slot limit and
         * triggers the start callback, resetting the countdown); anything else is rejected and
         * backs up on the belt / stays in the pushing inventory, exactly like vanilla.
         */
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot != 0)
                return false;
            ItemStack current = getStackInSlot(0);
            if (!current.isEmpty())
                return ItemStack.isSameItemSameComponents(stack, current);
            return isEmpty();
        }
    }
}
