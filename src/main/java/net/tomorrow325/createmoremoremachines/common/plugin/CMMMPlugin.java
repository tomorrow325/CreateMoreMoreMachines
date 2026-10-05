package net.tomorrow325.createmoremoremachines.common.plugin;

import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMAdvancedMachineTypes;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import net.yxiao233.createmoremachines.api.registry.CMMPlugin;
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
 * <li>{@link #registryTiers()} and {@link #registryPartialModels()} stay empty: this addon
 * reuses the CMM tiers as-is and ships no partial models.</li>
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
        CMMMAdvancedMachineTypes.STONE_CUTTER.getName();
        CMMMRegistryEntry.registerAll();
    }
}
