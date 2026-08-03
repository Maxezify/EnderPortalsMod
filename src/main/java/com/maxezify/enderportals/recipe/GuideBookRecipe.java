package com.maxezify.enderportals.recipe;

import com.maxezify.enderportals.ModRecipes;
import com.maxezify.enderportals.item.GuideBook;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

/**
 * La recette du livre-guide : une grille en forme, mais un résultat bâti en
 * Java par {@link GuideBook#create()}.
 *
 * <p>Une recette en forme ordinaire déclare son résultat dans le JSON, avec ses
 * composants — et c'est précisément là que le livre écrit se laisse mal écrire
 * (voir {@link GuideBook}). Ce type-ci ne déclare que le motif et les
 * ingrédients ; le résultat, lui, n'a plus de forme JSON à respecter.</p>
 *
 * <p>Elle n'est délibérément <b>pas</b> {@code isSpecial()}. Une recette
 * spéciale n'apparaît jamais dans le livre de recettes du joueur, et c'est
 * pour cette seule raison que la 0.4.0 avait abandonné le résultat en Java au
 * profit du JSON — troquant un défaut d'affichage contre une recette qui ne
 * fonctionnait plus du tout. En gardant motif et ingrédients déclarés, on a les
 * deux : le livre de recettes la montre, et le résultat reste incassable.</p>
 */
public class GuideBookRecipe implements CraftingRecipe {

    private final String group;
    private final CraftingBookCategory category;
    private final ShapedRecipePattern pattern;

    public GuideBookRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return GuideBook.create();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return GuideBook.create();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return pattern.ingredients();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GUIDE_BOOK.get();
    }

    /** Le motif et les ingrédients viennent du JSON ; le résultat, non. */
    public static class Serializer implements RecipeSerializer<GuideBookRecipe> {

        private static final MapCodec<GuideBookRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "")
                                .forGetter(GuideBookRecipe::getGroup),
                        CraftingBookCategory.CODEC.fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(GuideBookRecipe::category),
                        ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern)
                ).apply(instance, GuideBookRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GuideBookRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<GuideBookRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GuideBookRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, GuideBookRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
        }

        private static GuideBookRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            return new GuideBookRecipe(group, category, ShapedRecipePattern.STREAM_CODEC.decode(buffer));
        }
    }
}
