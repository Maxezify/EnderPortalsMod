package com.maxezify.enderportals.recipe;

import com.maxezify.enderportals.ModItems;
import com.maxezify.enderportals.ModRecipes;
import com.maxezify.enderportals.item.GuideBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Recette spéciale du livre-guide : un livre au centre, entouré de huit
 * Cristaux de l'Ender. Le résultat est construit en Java par
 * {@link GuideBook#create()}, ce qui évite le format JSON fragile des
 * composants de livre écrit.
 */
public class GuideBookRecipe extends SpecialCraftingRecipe {

    public GuideBookRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getWidth() != 3 || input.getHeight() != 3) {
            return false;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (slot == 4) {
                if (!stack.isOf(Items.BOOK)) {
                    return false;
                }
            } else if (!stack.isOf(ModItems.ENDER_CRYSTAL)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return GuideBook.create();
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GUIDE_BOOK;
    }
}
