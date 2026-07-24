package com.maxezify.enderportals;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Enchantement « Brisure d'Espace-Temps ». Il est défini par datapack
 * (data/enderportals/enchantment/spacetime_breach.json) — les enchantements de
 * 1.21 sont des entrées de registre pilotées par les données, sans
 * enregistrement Java. On garde ici la clé de registre et l'aide de détection.
 */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> SPACETIME_BREACH =
            ResourceKey.create(Registries.ENCHANTMENT, EnderPortalsMod.id("spacetime_breach"));

    /**
     * L'objet est-il une pioche portant la Brisure d'Espace-Temps ? Sert à
     * autoriser la casse (et le butin) du Bloc de l'Ender par n'importe quelle
     * pioche enchantée, en plus de la Pioche de l'Ender.
     */
    public static boolean allowsEnderBlock(Level level, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ItemTags.PICKAXES)) {
            return false;
        }
        Optional<Holder.Reference<Enchantment>> holder =
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(SPACETIME_BREACH);
        return holder.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack) > 0;
    }

    private ModEnchantments() {
    }
}
