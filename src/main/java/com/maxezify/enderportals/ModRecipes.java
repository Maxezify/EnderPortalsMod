package com.maxezify.enderportals;

import com.maxezify.enderportals.recipe.GuideBookRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EnderPortalsMod.MODID);

    /** Le livre-guide : motif déclaré en JSON, contenu bâti en Java. */
    public static final DeferredHolder<RecipeSerializer<?>, GuideBookRecipe.Serializer> GUIDE_BOOK =
            RECIPE_SERIALIZERS.register("guide_book", GuideBookRecipe.Serializer::new);

    private ModRecipes() {
    }
}
