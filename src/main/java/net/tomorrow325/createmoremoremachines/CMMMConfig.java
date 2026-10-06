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
 * a configured value below 0 (an explicit {@code -1}, or any unexpected negative) means "follow
 * the tier's CMM processing multiple"; anything else is returned as configured. No clamping
 * happens here on purpose - the use sites keep their existing guards (the wheel's 64 parallel
 * multiple cap - the actual batch is the capped multiple &times; the item's max stack size - and
 * the saw's 64 batch cap, the cycle-duration floors, the vanilla-speed early-outs and the
 * {@code max(..., 1)} protections), so an explicit value behaves exactly like the same CMM
 * multiple would.
 */
public class CMMMConfig {
    private static final ModConfigSpec.Builder BUILDER;
    // Defaults mirror the CMM deployer of the same tier: CMMConfig ships deployer processing
    // multiples of 4 (brass), 8 (netherite), 16 (end), 32 (beyond) and 64 (creative), so the
    // addon machines start out matching their same-tier deployer instead of the usually larger
    // tier processing multiple. An explicit -1 in the config file opts back into that multiple.
    public static final CMMMTierConfig BRASS = CMMMTierConfig.create("brass",
            4,4,
            4,4
    );
    public static final CMMMTierConfig NETHERITE = CMMMTierConfig.create("netherite",
            8,8,
            8,8
    );
    public static final CMMMTierConfig END = CMMMTierConfig.create("end",
            16,16,
            16,16
    );
    public static final CMMMTierConfig BEYOND = CMMMTierConfig.create("beyond",
            32,32,
            32,32
    );
    public static final CMMMTierConfig CREATIVE = CMMMTierConfig.create("creative",
            64,64,
            64,64
    );
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

    /**
     * Runtime and tooltip cap of the parallel multiple, matching CMM's own deployer clamp
     * ({@code CMMBeltDeployerCallbacks} uses {@code Math.min(multiple, 64)}). The crushing
     * wheel's actual batch is this capped multiple &times; the item's max stack size, while the
     * mechanical saw clamps its batch directly with the capped multiple.
     */
    public static final int MAX_PROCESSING_MULTIPLE = 64;

    /**
     * Translation key of the parallel-count tooltip line. Deliberately an own key instead of
     * CMM's {@code tooltip.createmoremachines.processing_multiple} so the addon controls its
     * wording; the shipped strings copy CMM's so both mods' tooltips stay visually identical.
     */
    public static final String TOOLTIP_PROCESSING_MULTIPLE = "tooltip.createmoremoremachines.processing_multiple";

    /**
     * Effective parallel count shown in the machine tooltips: the resolved multiple floored at
     * 1 (0/1 mean "parallel disabled", never "zero") and capped at {@link #MAX_PROCESSING_MULTIPLE}
     * - for the saw, the exact clamp its slot-0 capacity applies at runtime; for the wheel, the
     * clamp of its parallel multiple, whose actual batch is the capped multiple &times; the
     * item's max stack size (multiples &le; 1 keep the wheel's vanilla whole-stack baseline).
     * The tooltip must go through the same resolvers as the machines:
     * {@code CMMTierTooltip.Type.PROCESSING_MULTIPLE} renders {@code tier.getProcessingMultiple()}
     * only and never sees this addon's per-machine config override, which is the tooltip/config
     * mismatch this fixes. The floored display of 1 matches the config comment ("0/1 disables
     * the parallel feature").
     */
    public static int clampProcessingMultiple(int resolvedMultiple) {
        return Math.min(Math.max(resolvedMultiple, 1), MAX_PROCESSING_MULTIPLE);
    }

    /**
     * Translation key of the crushing wheel's static batch formula tooltip line. Deliberately a
     * wheel-specific key instead of the parameterised {@link #TOOLTIP_PROCESSING_MULTIPLE}:
     * the wheel's actual batch is the shown multiple &times; the item's max stack size, a
     * formula rather than a number, so the shipped strings state it without arguments.
     */
    public static final String TOOLTIP_CRUSHING_BATCH_HINT = "tooltip.createmoremoremachines.crushing_wheel_batch_hint";

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

    public static int mechanicalSawProcessingMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getMechanicalSawProcessingMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }

    public static int mechanicalSawSpeedMultiple(CMMTier tier) {
        CMMMTierConfig config = forTier(tier);
        if (config == null)
            return tier.getProcessingMultiple();
        int value = config.getMechanicalSawSpeedMultiple();
        return value < 0 ? tier.getProcessingMultiple() : value;
    }
}
