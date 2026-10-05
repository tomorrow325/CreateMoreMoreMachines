package net.yxiao233.createmoremoremachines.common.event;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.yxiao233.createmoremoremachines.CreateMoreMoreMachines;
import net.yxiao233.createmoremoremachines.common.registry.CMMMRegistryEntry;

/**
 * Registers the item capabilities of this addon's block entities. Create's own static
 * {@code registerCapabilities} methods only cover Create's own block entity types, so the
 * tiered controllers and stone cutters need their own registration:
 * <ul>
 * <li>controllers expose their public {@code inventory} (ProcessingInventory) on every
 * side, matching {@code CrushingWheelControllerBlockEntity#registerCapabilities};</li>
 * <li>stone cutters expose their inventory on every side except {@link Direction#DOWN},
 * matching {@code SawBlockEntity#registerCapabilities}.</li>
 * </ul>
 */
@SuppressWarnings({"removal", "unused"})
@EventBusSubscriber(modid = CreateMoreMoreMachines.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CMMMRegisterCapabilitiesHandler {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        CMMMRegistryEntry.getCrushingWheelControllerEntities()
            .forEach((id, type) -> {
                event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), (be, context) -> {
                    return be.inventory;
                });
            });
        CMMMRegistryEntry.getStoneCutterEntities()
            .forEach((id, type) -> {
                event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), (be, side) -> {
                    return side == Direction.DOWN ? null : be.inventory;
                });
            });
    }
}
