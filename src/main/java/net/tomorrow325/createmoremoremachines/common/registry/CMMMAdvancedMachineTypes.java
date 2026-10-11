package net.tomorrow325.createmoremoremachines.common.registry;

import java.util.Map;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.resources.ResourceLocation;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelBlock;
import net.tomorrow325.createmoremoremachines.api.content.encased_fan.CMMEncasedFanBlock;
import net.tomorrow325.createmoremoremachines.api.content.mechanical_saw.CMMMechanicalSawBlock;
import net.yxiao233.createmoremachines.api.registry.BuiltInAdvancedMachineTypes;
import net.yxiao233.createmoremachines.api.registry.BuiltInAdvancedMachineTypes.AdvancedMachineType;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * The three machine types added by this addon. {@code AdvancedMachineType.create} is a public
 * static factory and {@code equals} only compares names, so {@code "crushing_wheel"},
 * {@code "mechanical_saw"} and {@code "encased_fan"} cannot collide with the nine built-in CMM
 * type names (basin, depot, fluid_tank, deployer, mixer, press, spout, saw, steam_engine).
 */
public class CMMMAdvancedMachineTypes {
    public static final AdvancedMachineType<CMMCrushingWheelBlock> CRUSHING_WHEEL =
        AdvancedMachineType.create("crushing_wheel", AllBlocks.CRUSHING_WHEEL,
            CMMMRegistryEntry.getCrushingWheels());

    public static final AdvancedMachineType<CMMMechanicalSawBlock> MECHANICAL_SAW =
        AdvancedMachineType.create("mechanical_saw", AllBlocks.MECHANICAL_SAW,
            CMMMRegistryEntry.getMechanicalSaws());

    public static final AdvancedMachineType<CMMEncasedFanBlock> ENCASED_FAN =
        AdvancedMachineType.create("encased_fan", AllBlocks.ENCASED_FAN,
            CMMMRegistryEntry.getEncasedFans());

    /**
     * Stress impact constants per tier (brass/netherite/end/beyond/creative), aligned with the
     * CMM mechanical press impact table for wheels and with the CMM saw impact table for
     * mechanical saws.
     */
    private static final Map<String, Double> CRUSHING_WHEEL_IMPACTS = Map.of(
        "brass", 32.0,
        "netherite", 128.0,
        "end", 512.0,
        "beyond", 2048.0,
        "creative", 8.0);

    private static final Map<String, Double> MECHANICAL_SAW_IMPACTS = Map.of(
        "brass", 8.0,
        "netherite", 32.0,
        "end", 128.0,
        "beyond", 512.0,
        "creative", 8.0);

    /**
     * Fan impacts follow the saw's gradient (the vanilla encased fan's baseline stress is 2.0,
     * below the saw's 4.0, so the same table is the gentler of the two precedents and keeps the
     * tier costs ordered).
     */
    private static final Map<String, Double> ENCASED_FAN_IMPACTS = Map.of(
        "brass", 8.0,
        "netherite", 32.0,
        "end", 128.0,
        "beyond", 512.0,
        "creative", 8.0);

    public static double crushingWheelImpact(CMMTier tier) {
        return CRUSHING_WHEEL_IMPACTS.getOrDefault(tier.getId().getPath(), 8.0);
    }

    public static double mechanicalSawImpact(CMMTier tier) {
        return MECHANICAL_SAW_IMPACTS.getOrDefault(tier.getId().getPath(), 8.0);
    }

    public static double encasedFanImpact(CMMTier tier) {
        return ENCASED_FAN_IMPACTS.getOrDefault(tier.getId().getPath(), 8.0);
    }
}
