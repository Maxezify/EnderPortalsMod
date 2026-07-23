package com.maxezify.enderportals;

import com.maxezify.enderportals.recipe.GuideBookRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModRecipes {

    public static final RecipeSerializer<GuideBookRecipe> GUIDE_BOOK = Registry.register(
            Registries.RECIPE_SERIALIZER,
            EnderPortalsMod.id("guide_book"),
            new SpecialRecipeSerializer<>(GuideBookRecipe::new));

    public static void init() {
    }

    private ModRecipes() {
    }
}
