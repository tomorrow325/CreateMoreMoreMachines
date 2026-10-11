package net.tomorrow325.createmoremoremachines.api.content.encased_fan;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.tomorrow325.createmoremoremachines.CMMMConfig;

/**
 * The vanilla air current with a tier speed-up layered on top. Create's fan processing is
 * countdown driven and rotation independent: {@code FanProcessing.applyProcessing} initialises
 * the countdown once ({@code fanProcessingTime * ((count - 1) / 16 + 1) + 1}, or the {@code -1}
 * "cannot process" sentinel) and then decrements it by exactly 1 per tick. The two paths differ
 * in when they fire (both confirmed against the Create 6.0.10 bytecode): the belt path processes
 * when the decrement result is {@code <= 0} ({@code ifle}), while the loose item path processes
 * only when it is exactly {@code 0} ({@code ifeq}) - and its {@code decrementProcessingTime}
 * never re-initialises a matching type's counter, so a counter that slips past 0 to a negative
 * value would silence that item forever. This subclass therefore pre-deducts {@code multiple - 1}
 * extra ticks per tick with a floor of 1 before the vanilla pass runs: the vanilla decrement
 * then always lands on exactly 0 at the trigger tick, so both paths fire, a full tick removes
 * {@code multiple} ticks of countdown, and the pre-deduction can never write the {@code -1}
 * sentinel or any negative counter. Identical to vanilla from the recipe's point of view (same
 * items, same results, same advancement awards), just faster. Multiples {@code <= 1} (including
 * an explicitly configured 0/1 and any unexpected negative through the resolver) skip the
 * pre-deduction entirely and leave the vanilla timing.
 *
 * <p>Both paths are guarded to the server side: the client only spawns processing particles,
 * and a stopped fan never reaches these methods at all ({@code EncasedFanBlockEntity#tick}
 * early-returns on {@code getSpeed() == 0} before ticking the air current), so a stalled fan
 * neither accelerates nor corrupts anything.
 *
 * <p>Belt path - {@link #tickAffectedHandlers()}: before the vanilla pass, every item near the
 * fan's belt segment gets its {@code TransportedItemStack#processingTime} reduced by
 * {@code multiple - 1}, floored at 1, by answering {@link TransportedResult#doNothing()} to
 * {@code handleProcessingOnAllItems}; that result is a true no-op application ({@code
 * BeltInventory#applyToEachWithin} compares the result against the untouched stack and skips the
 * belt mutation), the same steady-state shape the vanilla lambda itself returns on every
 * non-trigger tick. The {@code > 0} guard never touches the {@code -1} sentinel (items that
 * cannot be processed by this type must keep it or they would be skipped forever), items whose
 * {@code processedBy} still points at another type are re-initialised by the vanilla pass right
 * after, harmlessly overwriting the pre-deduction, and the floor changes nothing about when the
 * belt path fires (its {@code <= 0} trigger also caught the 0/-1 overshoot of a 0 floor - the
 * pre-deduction values only differ below the floor, never at or above it).
 *
 * <p>Loose item path - {@link #tickAffectedEntities(Level)}: same pre-deduction on the entity's
 * persistent NBT counter ({@code CreateData} / {@code Processing} / {@code Time}), the same
 * counter the vanilla {@code FanProcessing#decrementProcessingTime} owns. Only existing positive
 * counters are touched (a missing tag reads as 0): counters left negative are vanilla's own
 * post-process state ({@code applyProcessing} neither clears nor resets the tag after a
 * successful process), so "healing" them here would corrupt legitimate vanilla data, and a
 * counter left by a different processing type is reset to full by vanilla on the following pass
 * anyway. The floor of 1 is what makes this path work at all: with a 0 floor the pre-deduction
 * could land the counter on 0 before the vanilla pass decrements it to -1, which the {@code == 0}
 * trigger never fires on and a matching type never resets.
 */
public class TieredAirCurrent extends AirCurrent {
    public TieredAirCurrent(IAirCurrentSource source) {
        super(source);
    }

    /**
     * The resolved speed multiple of the fan that owns this air current, or 1 for any other
     * source (the constructor is public, but this addon only ever attaches the subclass to
     * {@link CMMEncasedFanBlockEntity}).
     */
    private int speedMultiple() {
        if (source instanceof CMMEncasedFanBlockEntity fan)
            return CMMMConfig.encasedFanSpeedMultiple(fan.getTier());
        return 1;
    }

    @Override
    public void tickAffectedHandlers() {
        int multiple = speedMultiple();
        if (multiple > 1) {
            Level level = source.getAirCurrentWorld();
            if (level != null && !level.isClientSide) {
                List<Pair<TransportedItemStackHandlerBehaviour, FanProcessingType>> handlers = affectedItemHandlers;
                for (Pair<TransportedItemStackHandlerBehaviour, FanProcessingType> pair : handlers) {
                    // Mirror the vanilla pass, which skips belt segments with no processing type
                    // (a running countdown there must keep vanilla speed, not the tier's).
                    if (pair.getRight() == null)
                        continue;
                    pair.getKey()
                        .handleProcessingOnAllItems(transported -> preDeductProcessingTime(transported, multiple));
                }
            }
        }
        super.tickAffectedHandlers();
    }

    /**
     * The pre-deduction answer for one belt item: subtract {@code multiple - 1} from a positive
     * countdown, floored at 1 so the vanilla pass always lands on exactly 0 at the trigger tick
     * (its {@code <= 0} test then fires) and neither the pre-deduction nor the vanilla decrement
     * can write the {@code -1} sentinel, and always answer {@code doNothing} - the vanilla pass
     * runs right after this one and owns the actual countdown decrement and the process trigger.
     */
    private TransportedResult preDeductProcessingTime(TransportedItemStack transported, int multiple) {
        if (transported.processingTime > 0)
            transported.processingTime = Math.max(1, transported.processingTime - (multiple - 1));
        return TransportedResult.doNothing();
    }

    @Override
    protected void tickAffectedEntities(Level level) {
        int multiple = speedMultiple();
        if (multiple > 1 && level != null && !level.isClientSide) {
            for (Entity entity : caughtEntities) {
                if (entity instanceof ItemEntity itemEntity) {
                    CompoundTag processing = itemEntity.getPersistentData()
                        .getCompound("CreateData")
                        .getCompound("Processing");
                    int time = processing.getInt("Time");
                    // Floor at 1, not 0: the vanilla pass decrements once more and its entity
                    // trigger fires only on an exact 0 - floored to 0 the countdown would
                    // overshoot to -1, which a matching processing type never re-initialises.
                    if (time > 0)
                        processing.putInt("Time", Math.max(1, time - (multiple - 1)));
                }
            }
        }
        super.tickAffectedEntities(level);
    }
}
