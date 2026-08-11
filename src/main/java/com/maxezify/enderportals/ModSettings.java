package com.maxezify.enderportals;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Les réglages du mod, côté serveur.
 *
 * <p>Réglages <b>serveur</b> et non commun : ils décident de ce qui est permis
 * dans le monde, ce qui regarde la partie et non l'installation. Le fichier
 * vit donc dans la sauvegarde ({@code serverconfig/enderportals-server.toml}),
 * chaque monde a le sien, et en multijoueur c'est celui de l'hôte qui fait
 * loi.</p>
 */
public final class ModSettings {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue BLOCK_TELEPORTS = BUILDER
            .comment("Refuse toute téléportation qui entre dans le monde de l'Ender, en sort,",
                    "ou franchit un mur de parcelle — sauf celles du mod lui-même (porte,",
                    "Passage des Alliés, Téléporteur d'entité).",
                    "",
                    "C'est ce qui tient le mod debout. Sans ce refus, un mod de points de",
                    "voyage rend la porte et le Passage des Alliés inutiles : on entre chez",
                    "soi, et chez les autres, sans jamais rien matérialiser ni ouvrir.",
                    "",
                    "Refuse any teleport that enters the Ender world, leaves it, or crosses a",
                    "plot wall — except the mod's own (door, Allies' Passage, Entity",
                    "Teleporter).")
            .define("blockTeleports", true);

    /**
      * Renommée depuis {@code operatorBypass} (0.31.0), et pas par coquetterie.
      *
      * <p>Cette option valait {@code true} par défaut, ce qui n'a rien bloqué
      * sur un serveur où le joueur est aussi l'administrateur. Corriger le
      * défaut ne suffisait pas : un fichier de configuration déjà écrit garde
      * sa valeur, et la correction n'aurait servi qu'aux mondes neufs. Sous un
      * nom neuf, la clé périmée est retirée au chargement et la nouvelle naît
      * avec son défaut — l'ancien réglage ne survit à personne.</p>
      */
    private static final ModConfigSpec.BooleanValue OPERATOR_BYPASS = BUILDER
            .comment("Les opérateurs (niveau de permission 2) échappent au refus ci-dessus.",
                    "",
                    "Faux par défaut, et c'est un choix. Sur la plupart des serveurs, celui",
                    "qui joue est aussi celui qui administre : laisser les opérateurs passer",
                    "revenait à ne rien bloquer pour personne, et le garde-fou semblait cassé",
                    "alors qu'il obéissait. Un réglage dont la valeur par défaut annule la",
                    "fonction n'est pas un réglage, c'est un piege.",
                    "",
                    "Passez-le a true si vous devez deplacer un joueur a la commande : tant",
                    "qu'il est faux, /tp vers l'Ender est refuse aux operateurs comme aux",
                    "autres.",
                    "",
                    "Operators (permission level 2) bypass the refusal above. False by default:",
                    "on most servers the person playing is also the person running it, so a",
                    "true default would silently disable the whole feature.")
            .define("allowOperatorTeleports", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Le garde-fou des téléportations est-il actif ? */
    public static boolean blockTeleports() {
        return BLOCK_TELEPORTS.get();
    }

    /** Les opérateurs y échappent-ils ? */
    public static boolean allowOperatorTeleports() {
        return OPERATOR_BYPASS.get();
    }

    private ModSettings() {
    }
}
