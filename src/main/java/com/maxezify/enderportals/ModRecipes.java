package com.maxezify.enderportals;

import com.maxezify.enderportals.recipe.GuideBookRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EnderPortalsMod.MODID);

    public static final Supplier<RecipeSerializer<GuideBookRecipe>> GUIDE_BOOK =
            RECIPE_SERIALIZERS.register("guide_book",
                    () -> new SimpleCraftingRecipeSerializer<>(GuideBookRecipe::new));

    private ModRecipes() {
    }
}
