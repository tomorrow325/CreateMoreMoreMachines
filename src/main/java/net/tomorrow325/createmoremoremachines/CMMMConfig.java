package net.tomorrow325.createmoremoremachines;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tomorrow325.createmoremoremachines.api.config.CMMMTierConfig;
import net.yxiao233.createmoremachines.api.registry.CMMTier;

/**
 * STARTUP config of this addon, aggregated exactly like CMM's {@code CMMConfig}: one
 * {@link ModConfigSpec} holding one {@link CMMMTierConfig} per built-in CMM tier, registered by
 * the mod constructor as {@code config/createmoremoremachines-startup.toml}. Like CMM, only
 * {@link ModConfigEvent.Loading} is consumed - the values are snapshotted once per game start
 * and config edits take effect on restart.
 *
 * <p>The four resolvers below are the single read path for the machine code. Resolution rule:
 * a configured value below 0 (the default {@code -1}, or any unexpected negative) means "follow
 * the tier's CMM processing multiple", which reproduces the previous un-overridden behaviour;
 * anything else is returned as configured. No clamping happens here on purpose - the use sites
 * keep their existing guards (the 64 batch cap, the cycle-duration floors, the vanilla-speed
 * early-outs and the {@code max(..., 1)} protections), so an explicit value behaves exactly
 * like the same CMM multiple would.
 */
public class CMMMConfig {
    private static final ModConfigSpec.Builder BUILDER;
    public static final CMMMTierConfig BRASS = CMMMTierConfig.create("brass");
    public static final CMMMTierConfig NETHERITE = CMMMTierConfig.create("netherite");
    public static final CMMMTierConfig END = CMMMTierConfig.create("end");
    public static final CMMMTierConfig BEYOND = CMMMTierConfig.create("beyond");
    public static final CMMMTierConfig CREATIVE = CMMMTierConfig.create("creative");
    protected static final ModConfigSpec SPEC;
    static {
        BUILDER = new ModConfigSpec.Builder();
        {
            BUILDER.translation(CMMMTierConfig.key("tier_settings")).push("TierSettings");
            {
                BRASS.registry(BUILDER);
                NETHERITE.registry(BUILDER);
                END.registry(BUILDER);
                BEYOND.registry(BUILDER);
                CREATIVE.registry(BUILDER);
            }
            BUILDER.pop();
        }
        SPEC = BUILDER.build();
    }

    protected static void loadConfig(ModConfigEvent.Loading event){
        BRASS.onLoad(event);
        NETHERITE.onLoad(event);
        END.onLoad(event);
        BEYOND.onLoad(event);
        CREATIVE.onLoad(event);
    }

    /**
     * Returns the config section of one of the five built-in tiers, or {@code null} for any
     * other tier (e.g. one registered by a third-party CMM plugin), for which every resolver
     * then follows CMM. Matching compares the tier id path only - the same convention as the
     * stress tables in {@code CMMMAdvancedMachineTypes} - so a third-party tier reusing a
     * built-in path ({@code othermod:brass}) shares the built-in section instead of falling
     * back, and needs no section of its own.
     */
    public static CMMMTierConfig forTier(CMMTier tier) {
        return switch (tier.getId().getPath()) {
            case "brass" -> BRASS;
            case "netherite" -> NETHERITE;
            case "end" -> END;
            case "beyond" -> BEYOND;
            case "creative" -> CREATIVE;
            default -> null;
        };
    }

    public static int crushingWheelProcessingMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getCrushingWheelProcessingMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }

    public static int crushingWheelSpeedMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getCrushingWheelSpeedMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }

    public static int stoneCutterProcessingMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getStoneCutterProcessingMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }

    public static int stoneCutterSpeedMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getStoneCutterSpeedMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }
}
