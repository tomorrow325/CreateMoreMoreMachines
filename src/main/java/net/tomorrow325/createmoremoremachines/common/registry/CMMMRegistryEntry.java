package net.tomorrow325.createmoremoremachines.common.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.saw.SawRenderer;
import com.simibubi.create.content.kinetics.saw.SawVisual;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.data.CreateBlockEntityBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelBlock;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelBlockEntity;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelControllerBlock;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelControllerBlockEntity;
import net.tomorrow325.createmoremoremachines.api.content.stone_cutter.CMMStoneCutterBlock;
import net.tomorrow325.createmoremoremachines.api.content.stone_cutter.CMMStoneCutterBlockEntity;
import net.yxiao233.createmoremachines.api.registry.CMMBlockStressValues;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * All registration ids of this addon live in the {@code createmoremoremachines} namespace:
 * {@code {tier}_crushing_wheel}, {@code {tier}_crushing_wheel_controller} and
 * {@code {tier}_stone_cutter}; block entities share the name of their block, mirroring the
 * Create More Machines convention.
 *
 * <p>All asset providers (blockstate, item model, lang) are no-ops because every asset is
 * hand-written in {@code src/main/resources}; the controller additionally no-ops its loot
 * table, as it has no item form and must not receive Registrate's default dropSelf loot.
 */
public class CMMMRegistryEntry {
    // Crushing Wheel
    private static final Map<ResourceLocation, BlockEntry<CMMCrushingWheelBlock>> CRUSHING_WHEELS = new HashMap<>();
    private static final Map<ResourceLocation, BlockEntry<CMMCrushingWheelControllerBlock>> CRUSHING_WHEEL_CONTROLLERS = new HashMap<>();
    private static final Map<ResourceLocation, BlockEntityEntry<CMMCrushingWheelBlockEntity>> CRUSHING_WHEEL_ENTITIES = new HashMap<>();
    private static final Map<ResourceLocation, BlockEntityEntry<CMMCrushingWheelControllerBlockEntity>> CRUSHING_WHEEL_CONTROLLER_ENTITIES = new HashMap<>();
    // Stone Cutter
    private static final Map<ResourceLocation, BlockEntry<CMMStoneCutterBlock>> STONE_CUTTERS = new HashMap<>();
    private static final Map<ResourceLocation, BlockEntityEntry<CMMStoneCutterBlockEntity>> STONE_CUTTER_ENTITIES = new HashMap<>();

    public static Map<ResourceLocation, BlockEntry<CMMCrushingWheelBlock>> getCrushingWheels() {
        return Collections.unmodifiableMap(CRUSHING_WHEELS);
    }

    public static Map<ResourceLocation, BlockEntry<CMMCrushingWheelControllerBlock>> getCrushingWheelControllers() {
        return Collections.unmodifiableMap(CRUSHING_WHEEL_CONTROLLERS);
    }

    public static Map<ResourceLocation, BlockEntityEntry<CMMCrushingWheelBlockEntity>> getCrushingWheelEntities() {
        return Collections.unmodifiableMap(CRUSHING_WHEEL_ENTITIES);
    }

    public static Map<ResourceLocation, BlockEntityEntry<CMMCrushingWheelControllerBlockEntity>> getCrushingWheelControllerEntities() {
        return Collections.unmodifiableMap(CRUSHING_WHEEL_CONTROLLER_ENTITIES);
    }

    public static Map<ResourceLocation, BlockEntry<CMMStoneCutterBlock>> getStoneCutters() {
        return Collections.unmodifiableMap(STONE_CUTTERS);
    }

    public static Map<ResourceLocation, BlockEntityEntry<CMMStoneCutterBlockEntity>> getStoneCutterEntities() {
        return Collections.unmodifiableMap(STONE_CUTTER_ENTITIES);
    }

    /**
     * Called from {@link net.yxiao233.createmoremachines.common.registry.CMMRegistryEntry}
     * {@code register()} (via {@code ICMMPlugin#onRegister}), at which point this addon's
     * registrate instance has already been attached to CMM's mod event bus.
     */
    public static void registerAll() {
        CMMTier.getTiers().forEach((id, tier) -> {
            if (CMMTier.shouldRegistry(tier, CMMMAdvancedMachineTypes.CRUSHING_WHEEL)) {
                registerCrushingWheelSet(tier);
            }
            if (CMMTier.shouldRegistry(tier, CMMMAdvancedMachineTypes.STONE_CUTTER)) {
                registerStoneCutter(tier);
            }
        });
    }

    private static void registerCrushingWheelSet(CMMTier tier) {
        ResourceLocation id = tier.getId();
        String name = id.getPath() + "_crushing_wheel";
        String controllerName = id.getPath() + "_crushing_wheel_controller";

        BlockEntry<CMMCrushingWheelBlock> wheel = CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
            .block(name, properties -> new CMMCrushingWheelBlock(tier, properties))
            .properties(properties -> properties.mapColor(MapColor.METAL))
            .initialProperties(SharedProperties::stone)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(TagGen.pickaxeOnly())
            .onRegister(CMMBlockStressValues.setImpact(CMMMAdvancedMachineTypes.crushingWheelImpact(tier)))
            .addLayer(() -> () -> RenderType.cutoutMipped())
            .setData(ProviderType.BLOCKSTATE, NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .item()
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .model(NonNullBiConsumer.noop())
            .build()
            .register();

        BlockEntry<CMMCrushingWheelControllerBlock> controller = CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
            .block(controllerName, properties -> new CMMCrushingWheelControllerBlock(tier, properties))
            .properties(properties -> properties.mapColor(MapColor.STONE)
                .noOcclusion()
                .noLootTable()
                .air()
                .noCollission()
                .pushReaction(PushReaction.BLOCK))
            .setData(ProviderType.BLOCKSTATE, NonNullBiConsumer.noop())
            .setData(ProviderType.LOOT, NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .register();

        BlockEntityEntry<CMMCrushingWheelBlockEntity> wheelEntity;
        CreateBlockEntityBuilder<CMMCrushingWheelBlockEntity, CreateRegistrate> wheelEntityBuilder =
            CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
                .blockEntity(name, (type, pos, state) -> new CMMCrushingWheelBlockEntity(tier, type, pos, state));
        wheelEntity = wheelEntityBuilder
            .validBlocks(wheel)
            .renderer(() -> KineticBlockEntityRenderer::new)
            .register();

        BlockEntityEntry<CMMCrushingWheelControllerBlockEntity> controllerEntity;
        CreateBlockEntityBuilder<CMMCrushingWheelControllerBlockEntity, CreateRegistrate> controllerEntityBuilder =
            CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
                .blockEntity(controllerName,
                    (type, pos, state) -> new CMMCrushingWheelControllerBlockEntity(tier, type, pos, state));
        controllerEntity = controllerEntityBuilder
            .validBlocks(controller)
            .register();

        CRUSHING_WHEELS.put(id, wheel);
        CRUSHING_WHEEL_CONTROLLERS.put(id, controller);
        CRUSHING_WHEEL_ENTITIES.put(id, wheelEntity);
        CRUSHING_WHEEL_CONTROLLER_ENTITIES.put(id, controllerEntity);
    }

    private static void registerStoneCutter(CMMTier tier) {
        ResourceLocation id = tier.getId();
        String name = id.getPath() + "_stone_cutter";

        BlockEntry<CMMStoneCutterBlock> stoneCutter = CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
            .block(name, properties -> new CMMStoneCutterBlock(tier, properties))
            .initialProperties(SharedProperties::stone)
            .properties(properties -> properties.noOcclusion()
                .mapColor(MapColor.PODZOL))
            .onRegister(CMMBlockStressValues.setImpact(CMMMAdvancedMachineTypes.stoneCutterImpact(tier)))
            .setData(ProviderType.BLOCKSTATE, NonNullBiConsumer.noop())
            .transform(TagGen.axeOrPickaxe())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .item(AssemblyOperatorBlockItem::new)
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .model(NonNullBiConsumer.noop())
            .build()
            .register();

        BlockEntityEntry<CMMStoneCutterBlockEntity> stoneCutterEntity;
        CreateBlockEntityBuilder<CMMStoneCutterBlockEntity, CreateRegistrate> stoneCutterEntityBuilder =
            CMMTier.getRegistrate(CreateMoreMoreMachines.MODID)
                .blockEntity(name, (type, pos, state) -> new CMMStoneCutterBlockEntity(tier, type, pos, state));
        stoneCutterEntity = stoneCutterEntityBuilder
            .visual(() -> SawVisual::new)
            .validBlocks(stoneCutter)
            .renderer(() -> SawRenderer::new)
            .register();

        STONE_CUTTERS.put(id, stoneCutter);
        STONE_CUTTER_ENTITIES.put(id, stoneCutterEntity);
    }
}
