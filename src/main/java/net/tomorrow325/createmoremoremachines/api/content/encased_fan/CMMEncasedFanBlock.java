package net.tomorrow325.createmoremoremachines.api.content.encased_fan;

import java.util.List;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.tomorrow325.createmoremoremachines.CMMMConfig;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.content.IHaveTierInformation;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import org.jetbrains.annotations.NotNull;

/**
 * Tiered encased fan. Mirrors Create More Machines' {@code CMMSawBlock} pattern:
 * the whole Create fan mechanism (air current, belt/loose item processing, chute
 * behaviour, wrench handling) is inherited untouched, only the block entity type, the
 * description id and the tier tooltip differ.
 *
 * <p>{@code getBlockEntityClass()} is deliberately not overridden: the {@code IBE}
 * lookup filters by {@code getBlockEntityClass().isInstance(...)} and
 * {@link CMMEncasedFanBlockEntity} is a subclass of {@code EncasedFanBlockEntity}, so
 * the inherited class check passes for this addon's block entities (same reasoning as
 * the mechanical saw).
 */
public class CMMEncasedFanBlock extends EncasedFanBlock implements IHaveTierInformation {
    private final CMMTier tier;

    public CMMEncasedFanBlock(CMMTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }

    @Override
    public @NotNull BlockEntityType<? extends com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity> getBlockEntityType() {
        return CMMMRegistryEntry.getEncasedFanEntities().get(tier.getId())
            .get();
    }

    @Override
    public @NotNull String getDescriptionId() {
        return ChatFormatting.YELLOW +
            Component.translatable(Util.makeDescriptionId("tier", tier.getId())).getString() +
            ChatFormatting.WHITE +
            Component.translatable("block.create.encased_fan").getString();
    }

    @Override
    public void addTierInformation(List<Component> tooltips) {
        // A speed multiple, not a parallel count - the fan has no batch concept, so this is
        // deliberately not CMMConfig.TOOLTIP_PROCESSING_MULTIPLE (whose shipped wording is a
        // parallel multiple) and not clamped at 64 like the parallel lines. The number goes
        // through the same CMMMConfig resolver as the TieredAirCurrent pre-deduction, so the
        // tooltip shows the speed multiple that is actually in effect; 0/1 resolve to the
        // vanilla baseline of 1, matching the config comment ("values below 2 keep the
        // vanilla speed").
        tooltips.add(Component.translatable(CMMMConfig.TOOLTIP_FAN_SPEED_MULTIPLE,
            Math.max(CMMMConfig.encasedFanSpeedMultiple(tier), 1)));
    }
}
