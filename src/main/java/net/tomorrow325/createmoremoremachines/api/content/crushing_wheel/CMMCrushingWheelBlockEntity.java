package net.tomorrow325.createmoremoremachines.api.content.crushing_wheel;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * Tiered crushing wheel block entity. Deliberately adds no behaviour overrides:
 * the vanilla {@link CrushingWheelBlockEntity#fixControllers()} does
 * {@code ((CrushingWheelBlock) getBlockState().getBlock()).updateControllers(...)},
 * which virtually dispatches into {@link CMMCrushingWheelBlock#updateControllers}.
 */
public class CMMCrushingWheelBlockEntity extends CrushingWheelBlockEntity {
    private final CMMTier tier;

    public CMMCrushingWheelBlockEntity(CMMTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }
}
