package net.yxiao233.createmoremoremachines.datagen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.yxiao233.createmoremoremachines.common.registry.CMMMRegistryEntry;
import net.yxiao233.createmoremachines.api.registry.CMMTier;
import net.yxiao233.createmoremachines.common.registry.CMMRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server datagen: the tier upgrade recipes (4 per machine, creative tier has no recipe,
 * following the CMM convention). Deliberately NOT annotated with CMM's {@code @RecipeGen},
 * so CMM's annotation scan does not pick this provider into its own datagen; the provider is
 * attached by {@code CreateMoreMoreMachines#gatherData} instead.
 *
 * <p>The helper mirrors {@code CMMBaseRecipeProvider#mechanicalCraftingRecipe}, with the
 * hardcoded CMM {@code makeId} replaced by this mod's {@code makeId} so the tier map keys
 * resolve in the {@code createmoremoremachines} namespace. Recipe/advancement ids derive from
 * the result item's registry name, which is already in this mod's namespace. The duplicate
 * {@code "has_item"} criterion name (second call overwrites the first) is kept as in CMM.
 */
public class CMMMRecipeProvider extends RecipeProvider {
    public CMMMRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        // Crushing Wheel: vanilla wheel -> brass -> netherite -> end -> beyond
        upgradeRecipe(CMMMRegistryEntry.getCrushingWheels(), AllBlocks.CRUSHING_WHEEL, AllItems.BRASS_SHEET, null,
            "brass")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getCrushingWheels(), null, CMMRegistryEntry.NETHERITE_ALLOY_SHEET, "brass",
            "netherite")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getCrushingWheels(), null, CMMRegistryEntry.END_ALLOY_SHEET, "netherite", "end")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getCrushingWheels(), null, CMMRegistryEntry.BEYOND_ALLOY_SHEET, "end", "beyond")
            .save(recipeOutput);

        // Stone Cutter: vanilla saw -> brass -> netherite -> end -> beyond
        upgradeRecipe(CMMMRegistryEntry.getStoneCutters(), AllBlocks.MECHANICAL_SAW, AllItems.BRASS_SHEET, null,
            "brass")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getStoneCutters(), null, CMMRegistryEntry.NETHERITE_ALLOY_SHEET, "brass",
            "netherite")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getStoneCutters(), null, CMMRegistryEntry.END_ALLOY_SHEET, "netherite", "end")
            .save(recipeOutput);
        upgradeRecipe(CMMMRegistryEntry.getStoneCutters(), null, CMMRegistryEntry.BEYOND_ALLOY_SHEET, "end", "beyond")
            .save(recipeOutput);
    }

    private <T extends Block> ShapelessRecipeBuilder upgradeRecipe(Map<ResourceLocation, BlockEntry<T>> map,
        @Nullable BlockEntry<?> createMechanical, ItemEntry<?> material, String requireId, String resultId) {
        BlockEntry<?> require;
        if (createMechanical != null) {
            require = createMechanical;
        } else {
            require = map.get(tierKey(requireId));
        }
        BlockEntry<?> result = map.get(tierKey(resultId));
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
            .requires(require)
            .requires(material)
            .unlockedBy("has_item", has(require))
            .unlockedBy("has_item", has(material));
    }

    /**
     * The tier maps ({@link CMMTier#getTiers()}) are keyed by the tier ids owned by Create
     * More Machines ({@code createmoremachines:brass} and friends) - NOT by this mod's
     * namespace. Recipe and advancement ids, in contrast, derive from the result item's
     * registry name and therefore land in this mod's namespace on their own.
     */
    private static ResourceLocation tierKey(String path) {
        return CMMTier.getTiers()
            .keySet()
            .stream()
            .filter(id -> id.getPath()
                .equals(path))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown CMM tier: " + path));
    }
}
