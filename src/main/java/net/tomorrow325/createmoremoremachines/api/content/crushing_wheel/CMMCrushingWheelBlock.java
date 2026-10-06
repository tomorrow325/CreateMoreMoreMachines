package net.tomorrow325.createmoremoremachines.api.content.crushing_wheel;

import java.util.List;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tomorrow325.createmoremoremachines.CMMMConfig;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.content.IHaveTierInformation;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import org.jetbrains.annotations.NotNull;

/**
 * Tiered crushing wheel. Method bodies of {@link #onRemove}, {@link #updateControllers} and
 * {@link #canSurvive} follow the vanilla {@link CrushingWheelBlock} implementation, with the
 * {@code AllBlocks.CRUSHING_WHEEL} / {@code AllBlocks.CRUSHING_WHEEL_CONTROLLER} identity
 * checks replaced by same-tier checks: the other wheel must be the very same block instance
 * (no mixing with the vanilla wheel or with wheels of other tiers) and the controller must be
 * this tier's controller.
 */
public class CMMCrushingWheelBlock extends CrushingWheelBlock implements IHaveTierInformation {
    private final CMMTier tier;

    public CMMCrushingWheelBlock(CMMTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }

    private Block getControllerBlock() {
        return CMMMRegistryEntry.getCrushingWheelControllers().get(tier.getId())
            .get();
    }

    private boolean isController(BlockState state) {
        return CMMMRegistryEntry.getCrushingWheelControllers().get(tier.getId()) != null
            && state.is(CMMMRegistryEntry.getCrushingWheelControllers().get(tier.getId())
                .get());
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        for (Direction d : Iterate.directions) {
            if (d.getAxis() == state.getValue(AXIS))
                continue;
            if (isController(worldIn.getBlockState(pos.relative(d))))
                worldIn.removeBlock(pos.relative(d), isMoving);
        }

        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    public void updateControllers(BlockState state, Level world, BlockPos pos, Direction side) {
        if (side.getAxis() == state.getValue(AXIS))
            return;
        if (world == null)
            return;

        BlockPos controllerPos = pos.relative(side);
        BlockPos otherWheelPos = pos.relative(side, 2);

        boolean controllerExists = isController(world.getBlockState(controllerPos));
        boolean controllerIsValid = controllerExists && world.getBlockState(controllerPos)
            .getValue(CrushingWheelControllerBlock.VALID);
        Direction controllerOldDirection = controllerExists ? world.getBlockState(controllerPos)
            .getValue(CrushingWheelControllerBlock.FACING) : null;

        boolean controllerShouldExist = false;
        boolean controllerShouldBeValid = false;
        Direction controllerNewDirection = Direction.DOWN;

        BlockState otherState = world.getBlockState(otherWheelPos);
        if (otherState.getBlock() == this) {
            controllerShouldExist = true;

            CMMCrushingWheelBlockEntity be = (CMMCrushingWheelBlockEntity) getBlockEntity(world, pos);
            CMMCrushingWheelBlockEntity otherBE = (CMMCrushingWheelBlockEntity) getBlockEntity(world, otherWheelPos);

            if (be != null && otherBE != null && (be.getSpeed() > 0) != (otherBE.getSpeed() > 0)
                && be.getSpeed() != 0 && otherBE.getSpeed() != 0) {
                Axis wheelAxis = state.getValue(AXIS);
                Axis sideAxis = side.getAxis();
                int controllerADO = Math.round(Math.signum(be.getSpeed())) * side.getAxisDirection()
                    .getStep();
                Vec3 controllerDirVec = new Vec3(wheelAxis == Axis.X ? 1 : 0, wheelAxis == Axis.Y ? 1 : 0,
                    wheelAxis == Axis.Z ? 1 : 0).cross(
                        new Vec3(sideAxis == Axis.X ? 1 : 0, sideAxis == Axis.Y ? 1 : 0, sideAxis == Axis.Z ? 1 : 0));

                controllerNewDirection = Direction.getNearest(controllerDirVec.x * controllerADO,
                    controllerDirVec.y * controllerADO, controllerDirVec.z * controllerADO);

                controllerShouldBeValid = true;
            }
            if (otherState.getValue(AXIS) != state.getValue(AXIS))
                controllerShouldExist = false;
        }

        if (!controllerShouldExist) {
            if (controllerExists)
                world.setBlockAndUpdate(controllerPos, Blocks.AIR.defaultBlockState());
            return;
        }

        if (!controllerExists) {
            if (!world.getBlockState(controllerPos)
                .canBeReplaced())
                return;
            world.setBlockAndUpdate(controllerPos, getControllerBlock().defaultBlockState()
                .setValue(CMMCrushingWheelControllerBlock.VALID, controllerShouldBeValid)
                .setValue(CMMCrushingWheelControllerBlock.FACING, controllerNewDirection));
        } else if (controllerIsValid != controllerShouldBeValid || controllerOldDirection != controllerNewDirection) {
            world.setBlockAndUpdate(controllerPos, world.getBlockState(controllerPos)
                .setValue(CMMCrushingWheelControllerBlock.VALID, controllerShouldBeValid)
                .setValue(CMMCrushingWheelControllerBlock.FACING, controllerNewDirection));
        }

        ((CMMCrushingWheelControllerBlock) getControllerBlock()).updateSpeed(world.getBlockState(controllerPos), world,
            controllerPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        for (Direction direction : Iterate.directions) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbourState = worldIn.getBlockState(neighbourPos);
            Axis stateAxis = state.getValue(AXIS);
            if (isController(neighbourState) && direction.getAxis() != stateAxis)
                return false;
            if (neighbourState.getBlock() != this)
                continue;
            if (neighbourState.getValue(AXIS) != stateAxis || stateAxis != direction.getAxis())
                return false;
        }

        return true;
    }

    @Override
    public BlockEntityType<? extends CMMCrushingWheelBlockEntity> getBlockEntityType() {
        return CMMMRegistryEntry.getCrushingWheelEntities().get(tier.getId())
            .get();
    }

    @Override
    public @NotNull String getDescriptionId() {
        return ChatFormatting.YELLOW +
            Component.translatable(Util.makeDescriptionId("tier", tier.getId())).getString() +
            ChatFormatting.WHITE +
            Component.translatable("block.create.crushing_wheel").getString();
    }

    @Override
    public void addTierInformation(List<Component> tooltips) {
        // Not CMMTierTooltip.PROCESSING_MULTIPLE: that renders the raw CMM tier multiple and
        // never sees this addon's per-machine config override. The number below goes through
        // the same CMMMConfig resolver as the controller's slot-0 batch, so the tooltip shows
        // the parallel multiple that is actually in effect - the wheel's actual batch is that
        // multiple x the item's max stack size, which the static hint line below states.
        tooltips.add(Component.translatable(CMMMConfig.TOOLTIP_PROCESSING_MULTIPLE,
            CMMMConfig.clampProcessingMultiple(CMMMConfig.crushingWheelProcessingMultiple(tier))));
        tooltips.add(Component.translatable(CMMMConfig.TOOLTIP_CRUSHING_BATCH_HINT));
    }
}
