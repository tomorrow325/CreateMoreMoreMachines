package net.tomorrow325.createmoremoremachines.common.event;

import java.util.Map;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.yxiao233.createmoremachines.common.registry.CMMRegistryEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

/**
 * Restores the cutout layer for Create More Machines blocks whose models use 1-bit alpha textures.
 * CMM registers these layers through Registrate callbacks, which can be lost when multiple CMM
 * plugins contribute registrate instances during client setup.
 */
@EventBusSubscriber(
    modid = CreateMoreMoreMachines.MODID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public class CMMMRenderLayerHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            applyCutout(CMMRegistryEntry.getBasins());
            applyCutout(CMMRegistryEntry.getMechanicalMixers());
            applyCutout(CMMRegistryEntry.getSpouts());
            applyCutout(CMMRegistryEntry.getFluidTanks());
        });
    }

    private static void applyCutout(Map<?, ? extends BlockEntry<? extends Block>> blocks) {
        blocks.values().forEach(entry ->
            ItemBlockRenderTypes.setRenderLayer(entry.get(), RenderType.cutoutMipped())
        );
    }
}
