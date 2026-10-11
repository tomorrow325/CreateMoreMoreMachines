package net.tomorrow325.createmoremoremachines.compact.jei;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.BlockEntry;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.tomorrow325.createmoremoremachines.CreateMoreMoreMachines;
import net.tomorrow325.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import org.jetbrains.annotations.NotNull;

/**
 * JEI integration. The plugin uid lives in this mod's own namespace
 * ({@code createmoremoremachines:jei}) to avoid clashing with CMM's
 * {@code createmoremachines:jei}.
 *
 * <p>Catalysts are the machine blocks with an item form. The crushing wheel controllers have
 * no BlockItem, and JEI's {@code addRecipeCatalyst(ItemLike, ...)} resolves to
 * {@code asItem().getDefaultInstance()}, i.e. air for them, so they are never used here.
 */
@JeiPlugin
public class CMMMJEIPlugin implements IModPlugin {
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateMoreMoreMachines.MODID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        RecipeType<?> crushing = RecipeType.createRecipeHolderType(Create.asResource("crushing"));
        RecipeType<?> milling = RecipeType.createRecipeHolderType(Create.asResource("milling"));
        RecipeType<?> cutting = RecipeType.createRecipeHolderType(Create.asResource("cutting"));
        RecipeType<?> stonecutting =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("minecraft", "stonecutting"));
        // The fan's four JEI categories use the fan_* uids of Create's own JEI plugin, not the
        // fan processing type registration names (blasting/smoking/splashing/haunting) - the
        // catalysts must match the category ids.
        RecipeType<?> fanWashing = RecipeType.createRecipeHolderType(Create.asResource("fan_washing"));
        RecipeType<?> fanSmoking = RecipeType.createRecipeHolderType(Create.asResource("fan_smoking"));
        RecipeType<?> fanBlasting = RecipeType.createRecipeHolderType(Create.asResource("fan_blasting"));
        RecipeType<?> fanHaunting = RecipeType.createRecipeHolderType(Create.asResource("fan_haunting"));

        CMMTier.getTiers()
            .forEach((id, tier) -> {
                BlockEntry<?> wheel = CMMMRegistryEntry.getCrushingWheels()
                    .get(id);
                if (wheel != null) {
                    // The controller's findRecipe falls back to milling when no crushing recipe matches.
                    registration.addRecipeCatalyst(wheel, crushing);
                    registration.addRecipeCatalyst(wheel, milling);
                }
                BlockEntry<?> mechanicalSaw = CMMMRegistryEntry.getMechanicalSaws()
                    .get(id);
                if (mechanicalSaw != null) {
                    // Create's saw natively supports cutting plus (config-gated, default on) vanilla stonecutting.
                    registration.addRecipeCatalyst(mechanicalSaw, cutting);
                    registration.addRecipeCatalyst(mechanicalSaw, stonecutting);
                }
                BlockEntry<?> encasedFan = CMMMRegistryEntry.getEncasedFans()
                    .get(id);
                if (encasedFan != null) {
                    registration.addRecipeCatalyst(encasedFan, fanWashing);
                    registration.addRecipeCatalyst(encasedFan, fanSmoking);
                    registration.addRecipeCatalyst(encasedFan, fanBlasting);
                    registration.addRecipeCatalyst(encasedFan, fanHaunting);
                }
            });
    }
}
