package com.maxezify.enderportals.item;

import com.maxezify.enderportals.client.ClientShift;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
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
     * Le nom de la touche, inséré dans cette ligne plutôt qu'écrit dedans.
     *
     * <p>Deux raisons de le sortir du texte. Il se colore alors seul, en jaune
     * sur le gris sombre de la ligne, sans codes {@code §} noyés dans les
     * fichiers de langue. Et sa place dans la phrase reste libre : le français
     * la met au milieu, une autre langue la mettrait ailleurs.</p>
     */
    private static final String HINT_KEY = "enderportals.tooltip.shift_key";

    /**
     * Faut-il montrer le détail ?
     *
     * <p>Deux raisons de le montrer, et elles ne se ressemblent pas.</p>
     *
     * <p>La première est le joueur qui appuie sur Maj. Le test de {@code Dist}
     * qui l'accompagne n'est pas décoratif : {@link ClientShift} touche
     * {@code Screen}, absent d'un serveur dédié. Tant que la branche ne
     * s'exécute pas, la classe n'est pas chargée — et une infobulle construite
     * côté serveur, ce qui n'arrive pas en jeu normal mais reste permis par
     * l'API, se contente alors de la forme repliée.</p>
     *
     * <p>La seconde n'a pas de joueur du tout. Un visualiseur de recettes — JEI,
     * EMI, REI — lit les infobulles pour les indexer, et sa recherche ne trouve
     * que ce qu'il a pu lire. Replié derrière une touche que personne ne tient
     * au moment de l'indexation, tout le mode d'emploi du mod lui était
     * invisible : chercher « atterrisseur » ne ramenait pas le téléporteur qui
     * le vise. {@code shouldDisplayAllInformation} est le signal que NeoForge a
     * ajouté pour cela — le lecteur annonce qu'il veut tout, y compris ce qui
     * se replie, et on le lui donne.</p>
     */
    public static boolean expanded(TooltipFlag flag) {
        return flag.shouldDisplayAllInformation()
                || (FMLEnvironment.dist == Dist.CLIENT && ClientShift.down());
    }

    /**
     * Ajoute le mode d'emploi, ou l'invite qui dit comment l'ouvrir.
     *
     * <p>À appeler en dernier : l'invite doit fermer l'infobulle, pas s'insérer
     * au milieu de ce qui reste affiché.</p>
     */
    public static void details(List<Component> tooltip, TooltipFlag flag, Component... lines) {
        if (expanded(flag)) {
            tooltip.addAll(List.of(lines));
        } else {
            tooltip.add(Component.translatable(HINT,
                            Component.translatable(HINT_KEY).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.DARK_GRAY));
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
