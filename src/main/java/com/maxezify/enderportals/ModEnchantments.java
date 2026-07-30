package com.maxezify.enderportals;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
     * Le holder résolu, avec le registre dont il provient.
     *
     * <p>Les deux vont ensemble et ne doivent jamais être lus dépareillés : un
     * registre neuf associé à un ancien holder rendrait une réponse fausse. Les
     * garder dans un seul objet immuable derrière un champ {@code volatile} rend
     * la lecture atomique — nécessaire ici, car {@code canHarvestBlock} est
     * appelé côté client pour l'animation de casse tandis que
     * {@code BreakSpeed} l'est côté serveur intégré.</p>
     */
    private record Resolved(Registry<Enchantment> registry, @Nullable Holder.Reference<Enchantment> holder) {
    }

    private static volatile Resolved resolved;

    /**
     * L'objet est-il une pioche portant la Brisure d'Espace-Temps ? Sert à
     * autoriser la casse (et le butin) du Bloc de l'Ender par n'importe quelle
     * pioche enchantée, en plus de la Pioche de l'Ender.
     *
     * <p>Le test de tag passe d'abord : il est immédiat, et il élimine tout ce
     * qui n'est pas une pioche avant qu'on ne touche au registre.</p>
     */
    public static boolean allowsEnderBlock(Level level, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ItemTags.PICKAXES)) {
            return false;
        }
        Holder.Reference<Enchantment> holder = holder(level);
        return holder != null && EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0;
    }

    /**
     * Le holder de l'enchantement, résolu une fois par registre.
     *
     * <p>{@code PlayerEvent.BreakSpeed} se déclenche à <b>chaque tick</b> de
     * minage : y refaire une recherche de registre et allouer un
     * {@link Optional} à chaque fois était le seul coût récurrent du mod sur un
     * chemin chaud. Le cache est validé par identité du registre plutôt que par
     * durée de vie : un {@code /reload} ou un changement de monde en fournit un
     * autre, et la résolution repart d'elle-même.</p>
     */
    @Nullable
    private static Holder.Reference<Enchantment> holder(Level level) {
        Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Resolved current = resolved;
        if (current == null || current.registry() != registry) {
            current = new Resolved(registry, registry.getHolder(SPACETIME_BREACH).orElse(null));
            resolved = current;
        }
        return current.holder();
    }

    private ModEnchantments() {
    }
}
