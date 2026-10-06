package net.tomorrow325.createmoremoremachines.api.content.crushing_wheel;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * Tiered crushing wheel controller. {@link #updateSpeed} follows the vanilla
 * {@link CrushingWheelControllerBlock#updateSpeed} implementation with the
 * {@code AllBlocks.CRUSHING_WHEEL} neighbour check replaced by this tier's wheel block.
 * {@link #checkEntityForProcessing} intercepts server-side item entities ahead of the vanilla
 * idle-gated capture so they join the tier batch instead of entering one
 * {@code maxStackSize} per cycle (see
 * {@link CMMCrushingWheelControllerBlockEntity#cmmMergeFromEntity}); everything the merge
 * declines falls through to the vanilla decision unchanged.
 */
public class CMMCrushingWheelControllerBlock extends CrushingWheelControllerBlock {
    private final CMMTier tier;

    public CMMCrushingWheelControllerBlock(CMMTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }

    private Block getWheelBlock() {
        return CMMMRegistryEntry.getCrushingWheels().get(tier.getId())
            .get();
    }

    @Override
    public void checkEntityForProcessing(Level world, BlockPos pos, Entity entityIn) {
        // Absorb server-side item entities into the in-progress tier batch before vanilla's
        // !isOccupied()-gated capture can route them into its one-stack-per-cycle intake. The
        // bypass-tag check mirrors the vanilla one so freshly popped outputs of this controller
        // are never re-swallowed; every case the merge declines (mobs, players, other items,
        // speed 0, unconfigured tiers, occupied states) falls through to the vanilla decision.
        if (!world.isClientSide && entityIn instanceof ItemEntity itemEntity
            && world.getBlockEntity(pos) instanceof CMMCrushingWheelControllerBlockEntity cmm
            && cmm.crushingspeed != 0) {
            CompoundTag data = entityIn.getPersistentData();
            if (!data.contains("BypassCrushingWheel")
                || !pos.equals(NBTHelper.readBlockPos(data, "BypassCrushingWheel"))) {
                if (cmm.cmmMergeFromEntity(itemEntity))
                    return;
            }
        }
        super.checkEntityForProcessing(world, pos, entityIn);
    }

    @Override
    public BlockEntityType<? extends CMMCrushingWheelControllerBlockEntity> getBlockEntityType() {
        return CMMMRegistryEntry.getCrushingWheelControllerEntities().get(tier.getId())
            .get();
    }

    @Override
    public void updateSpeed(BlockState state, LevelAccessor world, BlockPos pos) {
        withBlockEntityDo(world, pos, be -> {
            if (!state.getValue(CrushingWheelControllerBlock.VALID)) {
                if (be.crushingspeed != 0) {
                    be.crushingspeed = 0;
                    be.sendData();
                }
                return;
            }

            for (Direction d : Iterate.directions) {
                BlockState neighbour = world.getBlockState(pos.relative(d));
                if (!neighbour.is(getWheelBlock()))
                    continue;
                if (neighbour.getValue(BlockStateProperties.AXIS) == d.getAxis())
                    continue;
                BlockEntity adjBE = world.getBlockEntity(pos.relative(d));
                if (!(adjBE instanceof CrushingWheelBlockEntity cwbe))
                    continue;
                be.crushingspeed = Math.abs(cwbe.getSpeed() / 50f);
                be.sendData();

                cwbe.award(AllAdvancements.CRUSHING_WHEEL);
                if (Math.abs(cwbe.getSpeed()) > AllConfigs.server().kinetics.maxRotationSpeed.get() - 1)
                    cwbe.award(AllAdvancements.CRUSHER_MAXED);

                break;
            }
        });
    }
}
