package net.tomorrow325.createmoremoremachines.api.content.stone_cutter;

import java.util.List;

import com.simibubi.create.content.kinetics.saw.SawBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.CMMTierTooltip;
import net.yxiao233.createmoremachines.api.content.IHaveTierInformation;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import org.jetbrains.annotations.NotNull;

/**
 * Tiered mechanical stone cutter. Mirrors Create More Machines' {@code CMMSawBlock} pattern:
 * the whole Create saw mechanism (cutting + stonecutting recipe lookup, filter, item
 * handling, tree felling) is inherited untouched, only the block entity type, the
 * description id and the tier tooltip differ.
 */
public class CMMStoneCutterBlock extends SawBlock implements IHaveTierInformation {
    private final CMMTier tier;

    public CMMStoneCutterBlock(CMMTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CMMTier getTier() {
        return tier;
    }

    @Override
    public @NotNull BlockEntityType<? extends com.simibubi.create.content.kinetics.saw.SawBlockEntity> getBlockEntityType() {
        return CMMMRegistryEntry.getStoneCutterEntities().get(tier.getId())
            .get();
    }

    @Override
    public @NotNull String getDescriptionId() {
        return ChatFormatting.YELLOW +
            Component.translatable(Util.makeDescriptionId("tier", tier.getId())).getString() +
            ChatFormatting.WHITE +
            Component.translatable("block.createmoremoremachines.stone_cutter").getString();
    }

    @Override
    public void addTierInformation(List<Component> tooltips) {
        CMMTierTooltip.byTypes(tooltips, tier, CMMTierTooltip.Type.PROCESSING_MULTIPLE);
    }
}
