package com.maxezify.enderportals.item;

import com.maxezify.enderportals.client.ClientShift;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Les infobulles du mod, repliées derrière la touche Maj.
 *
 * <p>Chaque machine d'ici a de quoi remplir cinq ou six lignes — ce qu'elle
 * fait, où elle se pose, ce qu'elle coûte, ce que dit son témoin. Toutes
 * affichées en permanence, elles couvraient l'écran au moindre survol d'un
 * coffre. Le partage est celui des mods de rangement : <b>l'état de cet
 * exemplaire</b> — la porte à laquelle cette clé est liée, l'Atterrisseur que
 * vise ce téléporteur — reste visible, puisqu'il change d'un objet à l'autre et
 * qu'on le consulte d'un coup d'œil ; <b>le mode d'emploi</b>, lui, ne se
 * déroule que sur demande.</p>
 */
public final class EnderTooltip {

    /** La ligne qui dit comment obtenir le reste. */
    private static final String HINT = "enderportals.tooltip.hold_shift";

    /**
     * Le joueur demande-t-il le détail ?
     *
     * <p>Le test de {@code Dist} n'est pas décoratif : {@link ClientShift}
     * touche {@code Screen}, absent d'un serveur dédié. Tant que la branche ne
     * s'exécute pas, la classe n'est pas chargée — et une infobulle construite
     * côté serveur, ce qui n'arrive pas en jeu normal mais reste permis par
     * l'API, se contente alors de la forme repliée.</p>
     */
    public static boolean expanded() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientShift.down();
    }

    /**
     * Ajoute le mode d'emploi, ou l'invite qui dit comment l'ouvrir.
     *
     * <p>À appeler en dernier : l'invite doit fermer l'infobulle, pas s'insérer
     * au milieu de ce qui reste affiché.</p>
     */
    public static void details(List<Component> tooltip, Component... lines) {
        if (expanded()) {
            tooltip.addAll(List.of(lines));
        } else {
            tooltip.add(Component.translatable(HINT).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Une ligne de mode d'emploi : gris clair, c'est la première. */
    public static Component head(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** Une ligne de mode d'emploi : gris sombre, ce sont les suivantes. */
    public static Component line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.DARK_GRAY);
    }

    private EnderTooltip() {
    }
}
