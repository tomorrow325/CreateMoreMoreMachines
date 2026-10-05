package net.tomorrow325.createmoremoremachines.common.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import net.yxiao233.createmoremachines.common.registry.CMMCreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Adds this addon's machine items to the Create More Machines creative tab. CMM's own
 * CreativeModTabContentsHandler only reflects over {@code CMMRegistryEntry} fields, so it
 * never sees this addon's entries.
 */
@SuppressWarnings({"removal", "unused"})
@EventBusSubscriber(modid = CreateMoreMoreMachines.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CMMMCreativeTabHandler {
    @SubscribeEvent
    public static void onBuild(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != CMMCreativeModeTab.TAB.get())
            return;
        CMMTier.getTiers()
            .forEach((id, tier) -> {
                var wheel = CMMMRegistryEntry.getCrushingWheels()
                    .get(id);
                if (wheel != null)
                    event.accept(wheel);
                var mechanicalSaw = CMMMRegistryEntry.getMechanicalSaws()
                    .get(id);
                if (mechanicalSaw != null)
                    event.accept(mechanicalSaw);
            });
    }
}
