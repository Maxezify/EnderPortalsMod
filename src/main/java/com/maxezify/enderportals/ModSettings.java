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

    private static final ModConfigSpec.BooleanValue OPERATOR_BYPASS = BUILDER
            .comment("Les opérateurs (niveau de permission 2) échappent au refus ci-dessus.",
                    "",
                    "ATTENTION — sur un serveur de test où tout le monde est opérateur, cela",
                    "revient à ne rien bloquer du tout, et le mod aura l'air de ne pas",
                    "fonctionner. Pour vérifier que le garde-fou agit : /deop <pseudo>, ou",
                    "passer cette option a false.",
                    "",
                    "Operators (permission level 2) bypass the refusal above. On a test server",
                    "where everyone is an operator, this blocks nothing at all.")
            .define("operatorBypass", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Le garde-fou des téléportations est-il actif ? */
    public static boolean blockTeleports() {
        return BLOCK_TELEPORTS.get();
    }

    /** Les opérateurs y échappent-ils ? */
    public static boolean operatorBypass() {
        return OPERATOR_BYPASS.get();
    }

    private ModSettings() {
    }
}
