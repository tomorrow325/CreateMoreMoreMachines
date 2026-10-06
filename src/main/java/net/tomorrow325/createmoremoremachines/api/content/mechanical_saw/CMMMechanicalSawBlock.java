package net.tomorrow325.createmoremoremachines.api.content.mechanical_saw;

import java.util.List;

import com.simibubi.create.content.kinetics.saw.SawBlock;

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
 * Tiered mechanical saw. Mirrors Create More Machines' {@code CMMSawBlock} pattern:
 * the whole Create saw mechanism (cutting + stonecutting recipe lookup, filter, item
 * handling, tree felling) is inherited untouched, only the block entity type, the
 * description id and the tier tooltip differ.
 */
public class CMMMechanicalSawBlock extends SawBlock implements IHaveTierInformation {
    private final CMMTier tier;

    public CMMMechanicalSawBlock(CMMTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }

    @Override
    public @NotNull BlockEntityType<? extends com.simibubi.create.content.kinetics.saw.SawBlockEntity> getBlockEntityType() {
        return CMMMRegistryEntry.getMechanicalSawEntities().get(tier.getId())
            .get();
    }

    @Override
    public @NotNull String getDescriptionId() {
        return ChatFormatting.YELLOW +
            Component.translatable(Util.makeDescriptionId("tier", tier.getId())).getString() +
            ChatFormatting.WHITE +
            Component.translatable("block.create.mechanical_saw").getString();
    }

    @Override
    public void addTierInformation(List<Component> tooltips) {
        // Not CMMTierTooltip.PROCESSING_MULTIPLE: that renders the raw CMM tier multiple and
        // never sees this addon's per-machine config override. The number below goes through
        // the same CMMMConfig resolver as the TieredSawInventory slot-0 capacity and the
        // block-breaking speed, so the tooltip shows the parallel count that is actually in
        // effect everywhere.
        tooltips.add(Component.translatable(CMMMConfig.TOOLTIP_PROCESSING_MULTIPLE,
            CMMMConfig.clampProcessingMultiple(CMMMConfig.mechanicalSawProcessingMultiple(tier))));
    }
}
