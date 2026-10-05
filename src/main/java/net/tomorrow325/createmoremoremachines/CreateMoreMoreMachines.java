package net.tomorrow325.createmoremoremachines;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.tomorrow325.createmoremoremachines.datagen.CMMMRecipeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Mod(CreateMoreMoreMachines.MODID)
public class CreateMoreMoreMachines {
    public static final String MODID = "createmoremoremachines";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateMoreMoreMachines(IEventBus modEventBus, ModContainer modContainer) {
        // STARTUP config, mirroring CMM's CreateMoreMachines constructor: the Loading listener
        // must be registered before registerConfig, because FML loads a STARTUP spec and fires
        // ModConfigEvent.Loading synchronously inside the registerConfig call - by the time it
        // returns, CMMMConfig's snapshots are filled.
        modEventBus.addListener(CMMMConfig::loadConfig);
        modContainer.registerConfig(ModConfig.Type.STARTUP, CMMMConfig.SPEC);
        LOGGER.info("CreateMoreMoreMachines is loading (requires CreateMoreMachines)");
        modEventBus.addListener(CreateMoreMoreMachines::gatherData);

        if (FMLEnvironment.dist == Dist.CLIENT){
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        generator.addProvider(event.includeServer(), new CMMMRecipeProvider(packOutput, lookupProvider));
    }

    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
