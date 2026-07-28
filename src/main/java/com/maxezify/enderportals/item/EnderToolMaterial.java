package com.maxezify.enderportals.item;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Palier (Tier) des outils en cristal de l'Ender — niveau diamant, très
 * rapide. Son « incorrect tag » est vide : la pioche de l'Ender récolte tout,
 * y compris les Blocs de l'Ender (que les pioches vanilla ne peuvent pas
 * faire tomber, via les tags incorrect_for_*_tool).
 *
 * <p>Attention : la pioche n'étant pas un {@code TieredItem} (elle porte un
 * composant {@code Tool} sur mesure), le jeu ne consulte de ce palier que
 * {@link #getSpeed()} et {@link #getAttackDamageBonus()}, via
 * {@code PickaxeItem.createAttributes}. Le reste n'est là que pour honorer
 * l'interface — la réparation, elle, est déclarée par
 * {@link EnderPickaxeItem#isValidRepairItem}.</p>
 */
public enum EnderToolMaterial implements Tier {
    INSTANCE;

    public static final TagKey<Block> INCORRECT_FOR_ENDER_TOOL =
            TagKey.create(Registries.BLOCK, EnderPortalsMod.id("incorrect_for_ender_tool"));

    @Override
    public int getUses() {
        return 2031;
    }

    @Override
    public float getSpeed() {
        return 9.0f;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0f;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return INCORRECT_FOR_ENDER_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.ENDER_CRYSTAL.get());
    }
}
