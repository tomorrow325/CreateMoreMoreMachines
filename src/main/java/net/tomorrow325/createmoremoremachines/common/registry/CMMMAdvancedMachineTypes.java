package net.tomorrow325.createmoremoremachines.common.registry;

import java.util.Map;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.resources.ResourceLocation;
import net.tomorrow325.createmoremoremachines.api.content.crushing_wheel.CMMCrushingWheelBlock;
import net.tomorrow325.createmoremoremachines.api.content.stone_cutter.CMMStoneCutterBlock;
import net.yxiao233.createmoremachines.api.registry.BuiltInAdvancedMachineTypes;
import net.yxiao233.createmoremachines.api.registry.BuiltInAdvancedMachineTypes.AdvancedMachineType;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * The two machine types added by this addon. {@code AdvancedMachineType.create} is a public
 * static factory and {@code equals} only compares names, so {@code "crushing_wheel"} and
 * {@code "stone_cutter"} cannot collide with the nine built-in CMM type names (basin, depot,
 * fluid_tank, deployer, mixer, press, spout, saw, steam_engine).
 */
public class CMMMAdvancedMachineTypes {
    public static final AdvancedMachineType<CMMCrushingWheelBlock> CRUSHING_WHEEL =
        AdvancedMachineType.create("crushing_wheel", AllBlocks.CRUSHING_WHEEL,
            CMMMRegistryEntry.getCrushingWheels());

    public static final AdvancedMachineType<CMMStoneCutterBlock> STONE_CUTTER =
        AdvancedMachineType.create("stone_cutter", AllBlocks.MECHANICAL_SAW,
            CMMMRegistryEntry.getStoneCutters());

    /**
     * Stress impact constants per tier (brass/netherite/end/beyond/creative), aligned with the
     * CMM mechanical press impact table for wheels and with the CMM saw impact table for
     * stone cutters.
     */
    private static final Map<String, Double> CRUSHING_WHEEL_IMPACTS = Map.of(
        "brass", 32.0,
        "netherite", 128.0,
        "end", 512.0,
        "beyond", 2048.0,
        "creative", 8.0);

    private static final Map<String, Double> STONE_CUTTER_IMPACTS = Map.of(
        "brass", 8.0,
        "netherite", 32.0,
        "end", 128.0,
        "beyond", 512.0,
        "creative", 8.0);

    public static double crushingWheelImpact(CMMTier tier) {
        return CRUSHING_WHEEL_IMPACTS.getOrDefault(tier.getId().getPath(), 8.0);
    }

    public static double stoneCutterImpact(CMMTier tier) {
        return STONE_CUTTER_IMPACTS.getOrDefault(tier.getId().getPath(), 4.0);
    }
}
