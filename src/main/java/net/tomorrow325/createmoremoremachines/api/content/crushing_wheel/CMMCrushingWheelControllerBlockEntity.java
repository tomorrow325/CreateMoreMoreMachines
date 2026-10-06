package net.tomorrow325.createmoremoremachines.api.content.crushing_wheel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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
 * <p>Two tier behaviours are layered on top of the Create controller mechanism (tick
 * countdown, recipe application and belt export are all inherited; entity intake is extended
 * by {@link #cmmMergeFromEntity(ItemEntity)} below):
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
 * as the slot 0 capacity: {@code min(processingMultiple, MAX_BATCH) &times; the item's max
 * stack size} on the belt/hopper path (unstackable items take the literal formula result
 * {@code min(processingMultiple, MAX_BATCH)} per cycle); tiers without a configured multiple
 * (field default 1) keep the vanilla one-stack baseline. Two members of the anonymous
 * {@link ProcessingInventory} below implement that capacity: {@code getStackLimit} is the only
 * capacity gate of {@code ItemStackHandler#insertItem} and lets slot 0 hold
 * {@code min(multiple, MAX_BATCH)} stacks of one kind, and the whole inventory is physically
 * enlarged to {@link #minOutputSlots()} slots because Create's private {@code applyRecipe}
 * (official sources :302-323) writes the rolled results back into slots
 * {@code 1..getSlots() - 1} - a private call is never dispatched to a subclass, so enlarging
 * {@code getSlots()} is the only lossless way to keep that loop from silently dropping the
 * output entries of oversized batches. The enlargement fully covers recipes whose inputs and
 * outputs are 64-stackable and whose per-cycle output volume is at most
 * {@link #OUTPUT_ENTRIES_PER_MULTIPLE} (all Create-shipped ones, worst case
 * {@code raw_gold_block} with {@code 9 + 18 = 27}); the derivation is on that constant and the
 * third-party residual risk is stated on {@link #MAX_BATCH}. Dropped item entities join the
 * same batch: {@link #cmmMergeFromEntity(ItemEntity)} - reached from the tier controller
 * block's {@code checkEntityForProcessing} override, which runs ahead of the vanilla
 * idle-gated capture ({@code !isOccupied()}) - grows slot 0 by what fits of the entity's
 * stack and resets the countdown, so a thrown-in pile forms batches of the same
 * {@code min(multiple, MAX_BATCH) &times; max stack size} size as the belt path. An entity
 * that does not fully fit keeps its remainder and retries on the next {@code entityInside}
 * tick; one carrying a different item, arriving while the outputs wait for export or while a
 * captured entity is in flight, or hitting an unconfigured tier ({@code multiple <= 1}) falls
 * through uncaptured, exactly as in vanilla.</li>
 * </ul>
 *
 * <p>Both multiples (the batch size above and the speed division in the bullet before it) are
 * resolved through {@code CMMMConfig} instead of being read straight from the tier: its
 * per-tier addon entries default to the processing multiple of the same tier's CMM deployer,
 * an explicit value of 2 or above overrides it, and an explicit {@code -1} follows the tier's
 * CMM processing multiple.
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
 * private {@code intakeItem} entity path on tiers resolving to {@code multiple <= 1}, where
 * {@link #cmmMergeFromEntity} declines) produces the same values because it routes through
 * this override of {@code findRecipe()}.
 */
public class CMMCrushingWheelControllerBlockEntity extends CrushingWheelControllerBlockEntity {
    /**
     * Upper bound of the parallel multiple, mirroring CMM's own clamp of the deployer
     * processing multiple ({@code CMMBeltDeployerCallbacks} uses {@code Math.min(multiple, 64)}).
     * The tier batch on the belt/hopper path is {@code min(processingMultiple, MAX_BATCH) &times;
     * the item's max stack size} (unstackable items: the literal {@code min(processingMultiple,
     * MAX_BATCH)}); tiers resolving to a multiple &le; 1 keep the vanilla whole-stack baseline.
     *
     * <p>The enlarged inventory ({@link #minOutputSlots()}) holds
     * {@link #OUTPUT_ENTRIES_PER_MULTIPLE} output entries per parallel unit plus two fixed
     * slots - one input slot and one margin for the crafting remainder, which vanilla's private
     * {@code applyRecipe} (official sources :302-323) adds to the result list once (:317-318)
     * and writes back into slots {@code 1..getSlots() - 1} (:320-321). That capacity fully
     * covers recipes whose inputs and outputs are 64-stackable with a per-cycle output volume
     * {@code sum(c_i) &le; 49} - every Create-shipped crushing/milling recipe qualifies (worst
     * crushing {@code raw_gold_block}: {@code 9 + 18 = 27}; worst milling 7; most results 5),
     * and simulating the official sources semantics of {@code applyRecipe} +
     * {@code ItemHelper.addToList} (:62-72) confirms zero overflow ({@code raw_gold_block} at
     * rolls 4096/256/64 &rarr; 1728/108/27 entries against write-back capacities
     * 3137/197/197; {@code golden_horse_armor} at rolls 4096 &rarr; 1024).
     *
     * <p>Serialization of an oversized count is handled inside this block entity: the anonymous
     * inventory below clamps every slot past {@link Item#ABSOLUTE_MAX_STACK_SIZE} through the
     * {@link #CMM_OVERFLOW_TAG} bypass while its NBT is written, so chunk saves and the per-tick
     * client sync stay exception-free whatever the batch size. Residual risk for third-party
     * datapack recipes outside that bound - an output volume above 49, low-stack outputs, or
     * per-result counts near the codec limit of 99 ({@code ProcessingOutput} uses
     * {@code ExtraCodecs.intRange(1, 99)}, official sources :115/:133): the vanilla write-back
     * loop then drops the surplus entries silently - the same class of loss vanilla itself has
     * with its fixed 31 output slots, just less of it. Two related vanilla behaviours worth
     * stating precisely: "every output entry fits one stack" only holds while a single roll's
     * single result count stays at or below the output's max stack size (all Create-shipped
     * recipes do, at most 18) - a count above 64 makes {@code ItemHelper.addToList} compute a
     * negative {@code transferred} (:66), so grow/shrink (:67-68) run backwards and one
     * {@code count > maxStackSize} entry per item survives; that is vanilla's own aggregation
     * behaviour and stays lossless end to end (belt export accepts whole stacks, non-belt
     * leftovers stay in their slot via {@code setStackInSlot}, ItemEntity pops and player pickup
     * keep the surplus in the entity). One path the bypass cannot reach: an output entity popped
     * with a count above the encoder limit (third-party recipes only, per the residual risk
     * above) is written by the popped {@code ItemEntity}'s own save data, which the same
     * {@code [1;99]} count codec constrains outside this class - a known residual, untouched
     * here.
     *
     * <p>Entity intake merges into the same batch: {@link #cmmMergeFromEntity} grows slot 0 up
     * to the capacity above and discards an item entity only once fully absorbed, so a
     * thrown-in pile forms the same batches as the belt path (vanilla's own capture stays
     * responsible for mobs and players, and for items on tiers resolving to
     * {@code multiple <= 1}). Breaking a controller mid-batch drops the inventory per slot
     * ({@code onRemove} &rarr; {@code ItemHelper.dropContents},
     * official sources {@code CrushingWheelControllerBlock.java:193-197}), so a full
     * creative-tier batch of the worst Create recipe pops thousands of item entities where
     * vanilla's fixed 31 output slots popped at most 31 - no data is lost, the transient burst
     * is just larger.
     */
    public static final int MAX_BATCH = 64;

    /**
     * Output entries provided per unit of the capped parallel multiple, and the coverage bound
     * of the enlarged output area: every recipe whose inputs and outputs are 64-stackable and
     * whose per-cycle output volume {@code sum(c_i)} is at most 49 is written out completely on
     * every tier. A batch of {@code rolls = 64 * clamp} - the capacity ceiling, reached on the
     * belt/hopper path and by merged item entities alike - produces
     * {@code ceil(rolls * c_i / maxStackSize_i) = clamp * c_i} entries per result ({@code
     * ItemHelper.addToList} aggregates up to each output's max stack size), so
     * {@code clamp * sum(c_i) <= 49 * clamp}, below the {@code getSlots() - 1} write-back capacity
     * of {@link #minOutputSlots()} including the one crafting remainder. All Create-shipped
     * crushing/milling recipes qualify (worst crushing {@code raw_gold_block}: {@code
     * crushed_raw_gold * 9 + experience_nugget * 18 = 27}; worst milling 7; most results 5;
     * every shipped output id 64-stackable), and simulating the official sources semantics of
     * {@code applyRecipe} + {@code addToList} confirms it: {@code raw_gold_block} at rolls
     * 4096/256/64 yields 1728/108/27 entries against capacities 3137/197/197,
     * {@code golden_horse_armor} at rolls 4096 yields 1024 - no overflow, no oversized entry.
     * Third-party recipes outside the bound (output volume above 49, low-stack outputs, or
     * single-result counts near the codec limit of 99) are covered by the residual-risk note
     * on {@link #MAX_BATCH}.
     */
    private static final int OUTPUT_ENTRIES_PER_MULTIPLE = 49;

    /**
     * Lower bound of the divided cycle duration. The controller applies the recipe once
     * {@code remainingTime < 20} and the countdown loses up to 20 per tick (the clamp maximum),
     * so 60 leaves at least two ticks of batch-accumulation window before the recipe fires
     * while still running visibly faster than the vanilla baseline of 100. The dividing
     * multiple is the {@code CMMMConfig} speed resolver (default = the tier's CMM deployer
     * processing multiple); this floor itself stays fixed.
     */
    public static final int MIN_CYCLE_DURATION = 60;

    /**
     * NBT key of the count-overflow bypass written by the anonymous {@link ProcessingInventory}
     * below. Batch counts reach {@code min(multiple, MAX_BATCH) &times; maxStackSize} (up to
     * 4096) while the ItemStack encoder clamps every serialized count to {@code [1;99]} -
     * {@code ItemStack.save} on a larger stack throws
     * {@code IllegalStateException: Value must be within range [1;99]}, which used to crash the
     * per-tick client sync in {@code ChunkHolder#broadcastChanges} and silently drop the whole
     * BE NBT on chunk save (crash report {@code crash-2026-10-06_10.33.27-server.txt}, slot 0
     * count 121).
     *
     * <p>Structure: a {@link CompoundTag} nested at the top level of the inventory's own NBT -
     * the vanilla {@code write} stores the inventory under the {@code "Inventory"} key, so the
     * full path is {@code Inventory.CMMOverflow}. Its keys are decimal slot-number strings, its
     * values are the real counts as ints. Slots within {@code [1;99]} serialize as-is and never
     * appear in the tag, so ordinary play produces no entry; a save without the tag loads with
     * zero overflow (old-save compatibility, both directions - the vanilla readers ignore
     * unknown keys). The tag travels inside the inventory NBT through the {@code getUpdateTag}
     * client sync as well and is kept there, so the client-side inventory restores the same
     * batch count as the server.
     */
    private static final String CMM_OVERFLOW_TAG = "CMMOverflow";

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
        // with the same-item merge that lets a stream of single belt items form a batch. The
        // serializeNBT/deserializeNBT pair below adds the CMMOverflow bypass (see
        // CMM_OVERFLOW_TAG): batch counts above the ItemStack encoder's [1;99] range are clamped
        // for the duration of the write and restored losslessly on read, so every serialization
        // path (vanilla write :318 / read :331 - chunk save/load and the per-tick getUpdateTag
        // client sync converge there) stays exception-free and keeps the real batch count.
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
            protected int getStackLimit(int slot, ItemStack stack) {
                // The only capacity gate of ItemStackHandler#insertItem; slot 0 carries the
                // tier batch (see cmmSlot0Capacity), every other slot keeps the vanilla
                // min(getSlotLimit, maxStackSize) baseline for the output area.
                return slot == 0 ? cmmSlot0Capacity(stack) : super.getStackLimit(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                // Stack count matching the slot 0 capacity above (ItemStackHandler computes
                // getStackLimit as min(getSlotLimit, maxStackSize)); a multiple <= 1 keeps the
                // vanilla baseline - the vanilla controller never calls withSlotLimit, so its
                // slot 0 limit is the ItemStackHandler default of 99, still capped by the
                // item's max stack size through getStackLimit.
                //
                // Why a larger getSlotLimit(0) is safe: the only reader that derives behaviour
                // from it is the hopper output hook - VanillaInventoryCodeHooks#insertHook
                // resolves the handler of the block the hopper faces into (which can be this
                // controller) and its isFull check requires every slot of that handler to be
                // non-empty with count >= getSlotLimit(slot). The enlarged output area keeps
                // empty slots around, so the is-full condition stays false and hoppers keep
                // inserting exactly as before; a larger getSlotLimit(0) only makes a false
                // "full" verdict even less likely. The vanilla controller block has no
                // comparator output either, so nothing else observes the limit value.
                int multiple = CMMMConfig.crushingWheelProcessingMultiple(tier);
                if (slot == 0 && multiple > 1)
                    return Math.min(multiple, MAX_BATCH) * Item.ABSOLUTE_MAX_STACK_SIZE;
                return super.getSlotLimit(slot);
            }

            @Override
            public void setSize(int size) {
                // Lower-bound guard: ItemStackHandler#deserializeNBT feeds the saved "Size" tag
                // through setSize, and an old save written with the vanilla-sized 32-slot
                // inventory (or a client whose config snapshot differs from the server's) must
                // never shrink the output area below the capacity the applyRecipe write-back
                // loop relies on. The overflow restore in deserializeNBT below runs after that
                // whole sequence (setSize included) and writes counts in place via setCount, so
                // it never passes through setSize and this guard's semantics are unchanged.
                super.setSize(Math.max(size, minOutputSlots()));
            }

            @Override
            public CompoundTag serializeNBT(HolderLookup.Provider registries) {
                // The ItemStack encoder clamps counts to [1;99] and throws on anything larger
                // (crash report crash-2026-10-06_10.33.27-server.txt, slot 0 count 121, hit from
                // ChunkHolder#broadcastChanges every tick and from chunk save on the same path -
                // both converge on the vanilla write's single inventory.serializeNBT call). Scan
                // every slot for counts past Item.ABSOLUTE_MAX_STACK_SIZE, clamp them in place
                // for the duration of super's write and record the real counts in the
                // CMM_OVERFLOW_TAG bypass; super itself (ProcessingInventory :60-67) first writes
                // Items/Size via ItemStackHandler#serializeNBT (:122-137, one ItemStack.save per
                // non-empty slot) and then ProcessingTime/RecipeTime/AppliedRecipe unchanged.
                // setCount mutates the very reference held in the protected stacks list
                // (ItemStackHandler#getStackInSlot returns it), so the finally-restore below
                // brings the live inventory back to the exact pre-write state - components
                // included - even if super throws. All of this runs on the server thread only
                // (both the broadcast and the save chain do), so the clamped window has no
                // concurrent reader. Slots within [1;99] are written untouched and produce no
                // bypass entry, keeping output areas and vanilla-tier saves byte-identical.
                Map<Integer, Integer> overflow = new HashMap<>();
                for (int slot = 0; slot < getSlots(); slot++) {
                    ItemStack stack = getStackInSlot(slot);
                    if (!stack.isEmpty() && stack.getCount() > Item.ABSOLUTE_MAX_STACK_SIZE) {
                        overflow.put(slot, stack.getCount());
                        stack.setCount(Item.ABSOLUTE_MAX_STACK_SIZE);
                    }
                }
                CompoundTag nbt;
                try {
                    nbt = super.serializeNBT(registries);
                } finally {
                    for (Map.Entry<Integer, Integer> entry : overflow.entrySet())
                        getStackInSlot(entry.getKey()).setCount(entry.getValue());
                }
                if (!overflow.isEmpty()) {
                    CompoundTag bypass = new CompoundTag();
                    for (Map.Entry<Integer, Integer> entry : overflow.entrySet())
                        bypass.putInt(String.valueOf(entry.getKey()), entry.getValue());
                    nbt.put(CMM_OVERFLOW_TAG, bypass);
                }
                return nbt;
            }

            @Override
            public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
                // Let the whole vanilla read run first: ProcessingInventory (:69-77) reads
                // ProcessingTime/RecipeTime/AppliedRecipe, ItemStackHandler#deserializeNBT
                // (:139-152) then rebuilds the list through setSize (guarded above, grow-only)
                // and ItemStack.parse per slot, and the closing isEmpty check clears
                // appliedRecipe. Restoring before any of that would lose the counts to the
                // setSize list rebuild; restoring after changes none of those verdicts - isEmpty
                // only inspects non-emptiness (:41-49), which a count beyond 99 does not affect.
                // A save without the bypass tag (any old save, or no oversized slot at write
                // time) leaves an empty compound and zero restores; on the client the same tag
                // arrives through getUpdateTag and restores the server's batch count so both
                // sides agree. setCount is used instead of setStackInSlot on purpose: it does
                // not fire onContentsChanged (a no-op here anyway), does not re-enter setSize,
                // and keeps the parsed stack and its components as-is. The restore is not
                // re-clamped against cmmSlot0Capacity - that capacity only gates insertItem
                // feed and cmmMergeFromEntity, never the stored value, and applyRecipe rolls
                // once per actual count. Defensive skips (unparseable key, out-of-range slot,
                // already-restored count) are unreachable for a tag written above.
                super.deserializeNBT(registries, nbt);
                CompoundTag bypass = nbt.getCompound(CMM_OVERFLOW_TAG);
                if (bypass.isEmpty())
                    return;
                for (String key : bypass.getAllKeys()) {
                    int slot;
                    try {
                        slot = Integer.parseInt(key);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (slot < 0 || slot >= getSlots())
                        continue;
                    int count = bypass.getInt(key);
                    ItemStack stack = getStackInSlot(slot);
                    if (!stack.isEmpty() && count > Item.ABSOLUTE_MAX_STACK_SIZE
                        && stack.getCount() <= Item.ABSOLUTE_MAX_STACK_SIZE)
                        stack.setCount(count);
                }
            }
        };
        // Physically enlarge the inventory so the vanilla private applyRecipe write-back loop
        // (bounded by getSlots()) cannot silently drop output entries of oversized batches:
        // applyRecipe is private and a private call is never dispatched to a subclass, so the
        // only lossless route is a larger getSlots().
        this.inventory.setSize(this.minOutputSlots());
        // Must be created after the replacement: the vanilla private wrapper still points at
        // the discarded inventory instance.
        this.cmmWrapper = new RecipeWrapper(this.inventory);
    }

    /**
     * Total inventory size: one input slot plus {@link #OUTPUT_ENTRIES_PER_MULTIPLE} output
     * entries per capped parallel unit, plus one spare slot for the at-most-one crafting
     * remainder ({@code applyRecipe} adds it to the result list once and its write-back loop
     * fills {@code getSlots() - 1} slots). The five defaults 4/8/16/32/64 yield
     * 198/394/786/1570/3138 slots; tiers resolving to a multiple &le; 1 are uniformly enlarged
     * to 51 slots as well - only empty slots are added, which keeps the NBT "Size" tag uniform
     * across tiers and lets old saves load unchanged. The same holds for the count-overflow
     * bypass: a save without the {@link #CMM_OVERFLOW_TAG} tag (every pre-bypass save, and any
     * save whose slots all stayed within the encoder range) deserializes with zero overflow.
     */
    private int minOutputSlots() {
        int multiple = CMMMConfig.crushingWheelProcessingMultiple(tier);
        return 2 + OUTPUT_ENTRIES_PER_MULTIPLE * Math.min(Math.max(multiple, 1), MAX_BATCH);
    }

    /**
     * Slot 0 capacity of one batch: {@code min(multiple, MAX_BATCH)} stacks of the given item
     * on a configured tier ({@code multiple > 1}), or the vanilla baseline of one whole stack
     * ({@code min(getSlotLimit(0) = 99, maxStackSize)}) otherwise. Single source of truth for
     * the anonymous inventory's {@code getStackLimit} - the only capacity gate of
     * {@code ItemStackHandler#insertItem} - and for {@link #cmmMergeFromEntity(ItemEntity)}.
     */
    private int cmmSlot0Capacity(ItemStack stack) {
        int multiple = CMMMConfig.crushingWheelProcessingMultiple(tier);
        if (multiple > 1)
            return Math.min(multiple, MAX_BATCH) * stack.getMaxStackSize();
        return Math.min(Item.ABSOLUTE_MAX_STACK_SIZE, stack.getMaxStackSize());
    }

    /**
     * Entity-path batch intake, called from the tier controller block's
     * {@code checkEntityForProcessing} override for every server-side {@code ItemEntity}
     * intersecting the controller cell. Vanilla only reaches its private {@code intakeItem}
     * through a capture gated on {@code !isOccupied()}, whose {@code clear()} + whole-stack
     * {@code setStackInSlot} would pin the entity path to one {@code maxStackSize} per cycle
     * (item entities merge at max stack size), so this method grows slot 0 by what fits of the
     * entity's stack instead - the same {@link ItemStack#isSameItemSameComponents} merge the
     * belt path gets through {@code isItemValid} - and resets the countdown through
     * {@link #cmmItemInserted}, syncing the block state exactly like the vanilla intake does
     * ({@code sendBlockUpdated} flag 18). The entity is discarded only once fully absorbed; a
     * remainder survives and retries on the next {@code entityInside} tick. A {@code false}
     * return leaves every vanilla decision in place: a different item, a full or already
     * applied batch, an entity in flight ({@code processingEntity != null}, whose private
     * intake would {@code clear()} the waiting batch), an unconfigured tier
     * ({@code multiple <= 1} keeps vanilla semantics wholesale), and the caller's bypass and
     * speed checks.
     */
    public boolean cmmMergeFromEntity(ItemEntity entity) {
        if (CMMMConfig.crushingWheelProcessingMultiple(tier) <= 1)
            return false;
        if (inventory.appliedRecipe || processingEntity != null)
            return false;
        ItemStack incoming = entity.getItem();
        if (incoming.isEmpty()) {
            entity.discard();
            return true;
        }
        int capacity = cmmSlot0Capacity(incoming);
        ItemStack current = inventory.getStackInSlot(0);
        int merged;
        if (current.isEmpty()) {
            if (!inventory.isEmpty())
                return false;
            merged = Math.min(incoming.getCount(), capacity);
            inventory.setStackInSlot(0, incoming.copyWithCount(merged));
        } else {
            if (!ItemStack.isSameItemSameComponents(incoming, current))
                return false;
            merged = Math.min(incoming.getCount(), capacity - current.getCount());
            if (merged <= 0)
                return false;
            current.grow(merged);
        }
        incoming.shrink(merged);
        cmmItemInserted(inventory.getStackInSlot(0));
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 18);
        if (incoming.isEmpty()) {
            entity.discard();
            return true;
        }
        return false;
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

        // Resolved speed multiple: CMMMConfig's default is the tier's CMM deployer processing
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
