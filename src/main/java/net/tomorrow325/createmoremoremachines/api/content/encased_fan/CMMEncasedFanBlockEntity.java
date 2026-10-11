package net.tomorrow325.createmoremoremachines.api.content.encased_fan;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * Tiered encased fan block entity. The whole Create fan mechanism is inherited untouched;
 * the only change is the constructor swap of the vanilla {@link #airCurrent} (a public,
 * non-final field that every Create code path reads through the field) with the
 * {@link TieredAirCurrent}, which shortens the processing countdown of the belt and the
 * loose item paths by the tier's configured speed multiple. The replacement happens before
 * any tick can run, and nothing else holds a reference to the vanilla instance
 * (rebuild/visual lookups all go through the block entity's field), so the swap is picked
 * up everywhere.
 */
public class CMMEncasedFanBlockEntity extends EncasedFanBlockEntity {
    private final CMMTier tier;

    public CMMEncasedFanBlockEntity(CMMTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
        this.airCurrent = new TieredAirCurrent(this);
    }

    public CMMTier getTier() {
        return tier;
    }
}
