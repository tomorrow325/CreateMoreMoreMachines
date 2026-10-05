package net.tomorrow325.createmoremoremachines.api.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;

/**
 * Per-tier config entries for the two machines added by this addon, mirroring CMM's
 * {@code TierConfigBase} pattern (define the entries in {@link #registry}, snapshot them once
 * on {@link ModConfigEvent.Loading} in {@link #onLoad}, and let the getters read only the
 * snapshot). Like CMM, this addon listens to no {@code ModConfigEvent.Reloading}, so config
 * edits take effect on the next game start.
 *
 * <p>Every entry defaults to {@code -1}; the resolvers in {@code CMMMConfig} translate a value
 * below 0 to "follow the tier's CMM processing multiple", which reproduces the previous,
 * un-overridden behaviour. The snapshots start at {@code -1} as well, so an unexpected read
 * before the loading event degrades safely to the follow-CMM behaviour instead of zeroing a
 * batch size or a speed.
 */
public class CMMMTierConfig {
    private ModConfigSpec.IntValue CRUSHING_WHEEL_PROCESSING_MULTIPLE;
    private ModConfigSpec.IntValue CRUSHING_WHEEL_SPEED_MULTIPLE;
    private ModConfigSpec.IntValue MECHANICAL_SAW_PROCESSING_MULTIPLE;
    private ModConfigSpec.IntValue MECHANICAL_SAW_SPEED_MULTIPLE;
    private int tieredCrushingWheelProcessingMultiple = -1;
    private int tieredCrushingWheelSpeedMultiple = -1;
    private int tieredMechanicalSawProcessingMultiple = -1;
    private int tieredMechanicalSawSpeedMultiple = -1;
    private final String tier;

    private CMMMTierConfig(String tier){
        this.tier = tier;
    }

    public static CMMMTierConfig create(String tier){
        return new CMMMTierConfig(tier);
    }

    public void registry(ModConfigSpec.Builder BUILDER){
        BUILDER.translation(key(tier + "_tier")).push(upperCaseForFirstChar(tier) + "Tier");

        // Parallel entry: [-1, 64]. -1 follows CMM; >= 2 is an explicit batch size whose runtime
        // clamp of 64 matches CMM's own deployer clamp (TierConfigBase [1,64] +
        // CMMBeltDeployerCallbacks Math.min(multiple, 64)); 0/1 disables the parallel feature.
        CRUSHING_WHEEL_PROCESSING_MULTIPLE = BUILDER
                .translation(key("crushing_wheel_processing_multiple"))
                .comment("Parallel processing batch size for " + tier + " tier crushing wheel[default:-1] (-1 follows the " + tier + " tier processing multiple of CreateMoreMachines, values above 1 are capped at 64)")
                .defineInRange(tier + "_crushing_wheel_processing_multiple",-1,-1,64);

        // Speed entry: [-1, Integer.MAX_VALUE]. -1 follows CMM; >= 2 divides the crushing
        // duration (floored by MIN_CYCLE_DURATION at the use site); 0/1 keeps the vanilla speed.
        CRUSHING_WHEEL_SPEED_MULTIPLE = BUILDER
                .translation(key("crushing_wheel_speed_multiple"))
                .comment("Speed multiple for " + tier + " tier crushing wheel, the crushing duration is divided by it[default:-1] (-1 follows the " + tier + " tier processing multiple of CreateMoreMachines, values below 2 keep the vanilla speed)")
                .defineInRange(tier + "_crushing_wheel_speed_multiple",-1,-1,Integer.MAX_VALUE);

        // Parallel entry: [-1, 64]. -1 follows CMM; >= 2 is an explicit batch size whose runtime
        // clamp of 64 matches CMM's own deployer clamp; 0/1 collapses to the vanilla saw
        // baseline of one item per slot 0 through the use-site Math.max(..., 1).
        MECHANICAL_SAW_PROCESSING_MULTIPLE = BUILDER
                .translation(key("mechanical_saw_processing_multiple"))
                .comment("Parallel processing batch size for " + tier + " tier mechanical saw[default:-1] (-1 follows the " + tier + " tier processing multiple of CreateMoreMachines, values above 1 are capped at 64)")
                .defineInRange(tier + "_mechanical_saw_processing_multiple",-1,-1,64);

        // Speed entry: [-1, Integer.MAX_VALUE]. -1 follows CMM; >= 2 divides the cutting
        // duration (floored by MIN_CYCLE_DURATION at the use site); 0/1 keeps the vanilla speed.
        MECHANICAL_SAW_SPEED_MULTIPLE = BUILDER
                .translation(key("mechanical_saw_speed_multiple"))
                .comment("Speed multiple for " + tier + " tier mechanical saw, the cutting duration is divided by it[default:-1] (-1 follows the " + tier + " tier processing multiple of CreateMoreMachines, values below 2 keep the vanilla speed)")
                .defineInRange(tier + "_mechanical_saw_speed_multiple",-1,-1,Integer.MAX_VALUE);

        BUILDER.pop();
    }

    @SuppressWarnings("unused")
    public void onLoad(ModConfigEvent.Loading event){
        this.tieredCrushingWheelProcessingMultiple = CRUSHING_WHEEL_PROCESSING_MULTIPLE.get();
        this.tieredCrushingWheelSpeedMultiple = CRUSHING_WHEEL_SPEED_MULTIPLE.get();
        this.tieredMechanicalSawProcessingMultiple = MECHANICAL_SAW_PROCESSING_MULTIPLE.get();
        this.tieredMechanicalSawSpeedMultiple = MECHANICAL_SAW_SPEED_MULTIPLE.get();
    }

    public int getCrushingWheelProcessingMultiple(){
        return tieredCrushingWheelProcessingMultiple;
    }

    public int getCrushingWheelSpeedMultiple(){
        return tieredCrushingWheelSpeedMultiple;
    }

    public int getMechanicalSawProcessingMultiple(){
        return tieredMechanicalSawProcessingMultiple;
    }

    public int getMechanicalSawSpeedMultiple(){
        return tieredMechanicalSawSpeedMultiple;
    }

    /**
     * Single-argument translation key builder: {@code config.createmoremoremachines.<name>}.
     * Deliberately not CMM's varargs {@code TierConfigBase.key()}, whose multi-argument branch
     * joins segments without a separator dot.
     */
    public static String key(String name){
        return "config." + CreateMoreMoreMachines.MODID + "." + name;
    }

    private static String upperCaseForFirstChar(String s){
        char first = s.toCharArray()[0];
        return s.replaceFirst(String.valueOf(first), String.valueOf(first).toUpperCase());
    }
}
