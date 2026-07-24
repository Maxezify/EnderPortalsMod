package com.maxezify.enderportals.recipe;

import com.maxezify.enderportals.ModItems;
import com.maxezify.enderportals.ModRecipes;
import com.maxezify.enderportals.item.GuideBook;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Recette spéciale du livre-guide : un livre au centre, entouré de huit
 * Cristaux de l'Ender. Le résultat est construit en Java par
 * {@link GuideBook#create()}, ce qui évite le format JSON fragile des
 * composants de livre écrit.
 */
public class GuideBookRecipe extends CustomRecipe {

    public GuideBookRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            if (slot == 4) {
                if (!stack.is(Items.BOOK)) {
                    return false;
                }
            } else if (!stack.is(ModItems.ENDER_CRYSTAL.get())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return GuideBook.create();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GUIDE_BOOK.get();
    }
}
