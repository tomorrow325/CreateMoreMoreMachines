package net.yxiao233.createmoremoremachines.common.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.yxiao233.createmoremoremachines.CreateMoreMoreMachines;
import net.yxiao233.createmoremachines.api.content.IHaveTierInformation;

import java.util.List;

/**
 * Appends the tier information line (yellow tier prefix + processing multiple) to the
 * tooltips of this addon's machines. Written exactly like CMM's own ItemTooltipHandler:
 * while ItemTooltipEvent is fired on the game bus, FML 4.x dispatches
 * {@code @SubscribeEvent} methods by parameter type, so this annotation-based registration
 * works regardless of the {@code bus} value. It must never be registered through
 * {@code modEventBus.addListener(ItemTooltipEvent.class, ...)}, which would silently never
 * fire (the mod bus only marker-checks IModBusEvent).
 */
@SuppressWarnings({"removal", "unused"})
@EventBusSubscriber(modid = CreateMoreMoreMachines.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CMMMItemTooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack()
            .getItem();
        List<net.minecraft.network.chat.Component> tooltips = event.getToolTip();
        Block block = Block.byItem(item);
        if (BuiltInRegistries.BLOCK.getKey(block)
            .getNamespace()
            .equals(CreateMoreMoreMachines.MODID)) {
            if (block instanceof IHaveTierInformation informationBlock) {
                informationBlock.addTierInformation(tooltips);
            }
        }
    }
}
