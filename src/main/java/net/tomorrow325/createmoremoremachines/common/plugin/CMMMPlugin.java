package net.tomorrow325.createmoremoremachines.common.plugin;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMAdvancedMachineTypes;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import net.yxiao233.createmoremachines.api.registry.CMMPlugin;
import net.yxiao233.createmoremachines.api.registry.CMMPartialModelsRegistry;
import net.yxiao233.createmoremachines.api.registry.ICMMPlugin;

/**
 * Entry point discovered by Create More Machines through the {@link CMMPlugin} annotation.
 *
 * <p>The public no-arg constructor is required: CMMTierManager.loadAllPlugin instantiates
 * plugin classes via {@code Class#newInstance()}.
 *
 * <p>Lifecycle notes (Create More Machines main constructor order):
 * <ul>
 * <li>{@link #registryRegistrate()} runs during {@code CMMTierManager.registryRegistrate()},
 * before tier/registrate freezing; it must use {@link CMMTier#createRegistrate} with this
 * mod's own id (CMM's own registrate instance refuses foreign callers via a StackWalker
 * package check).</li>
 * <li>{@link #onRegister()} runs as the first statement of
 * {@code CMMRegistryEntry.register()}, i.e. after {@code registerEventListeners} has attached
 * our registrate to CMM's mod event bus, so all registrations performed here are picked up by
 * the registry events.</li>
 * <li>{@link #registryTiers()} stays empty: this addon reuses the CMM tiers as-is.
 * {@link #registryPartialModels()} registers each registered tier's crushing wheel partial
 * model under the {@code "crushing_wheel"} key of {@link CMMPartialModelsRegistry} (with this
 * mod's own namespace), so the Flywheel visual registered on the crushing wheel block entity
 * can resolve it at client setup; the registry's static map keeps the {@link PartialModel}
 * strongly referenced, which Flywheel's weak-valued {@code PartialModel.ALL} cache does not.</li>
 * </ul>
 */
@CMMPlugin
public class CMMMPlugin implements ICMMPlugin {

    @Override
    public void registryRegistrate() {
        CMMTier.createRegistrate(CreateMoreMoreMachines.MODID);
    }

    @Override
    public void onRegister() {
        // Force the machine type singletons to exist before the shouldRegistry gate reads them.
        CMMMAdvancedMachineTypes.CRUSHING_WHEEL.getName();
        CMMMAdvancedMachineTypes.MECHANICAL_SAW.getName();
        CMMMRegistryEntry.registerAll();
    }

    /**
     * Must mirror the gate used by {@link CMMMRegistryEntry#registerAll()}: a tier whose
     * crushing wheel block entity is registered but whose partial model is not would make
     * {@link CMMPartialModelsRegistry#getPartialModels(CMMTier, String)} throw on the client
     * when the visual factory is evaluated.
     */
    @Override
    public void registryPartialModels() {
        CMMTier.getTiers().forEach((id, tier) -> {
            if (CMMTier.shouldRegistry(tier, CMMMAdvancedMachineTypes.CRUSHING_WHEEL)) {
                PartialModel wheel = PartialModel.of(CreateMoreMoreMachines
                    .makeId("block/crushing_wheel/" + tier.getId().getPath() + "_crushing_wheel"));
                CMMPartialModelsRegistry.registry(tier, "crushing_wheel", new PartialModel[]{wheel});
            }
        });
    }
}
