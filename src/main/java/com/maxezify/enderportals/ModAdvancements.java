package com.maxezify.enderportals;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Les progrès que le mod décerne lui-même.
 *
 * <p>Deux étapes de l'arbre ne correspondent à aucun déclencheur vanilla :
 * réussir le rituel de la Mace, et avoir taillé sa galerie sur mille blocs. Le
 * patron habituel s'applique — le fichier JSON déclare un critère
 * {@code minecraft:impossible}, que le jeu ne validera jamais de lui-même, et
 * le code le décerne au bon moment.</p>
 *
 * <p>Rien n'est mis en cache : le gestionnaire de progrès est rechargé à chaque
 * {@code /reload}, et garder une référence sur un {@link AdvancementHolder}
 * périmé décernerait dans le vide.</p>
 */
public final class ModAdvancements {

    /** Nom du critère unique des progrès décernés par le code. */
    private static final String CRITERION = "code";

    /** Réussir le rituel de la Mace. */
    public static final String RITUAL = "ritual";
    /** Avoir cassé {@value #GALLERY_TARGET} Blocs de l'Ender. */
    public static final String GALLERY = "gallery";

    public static final int GALLERY_TARGET = 1000;

    public static void award(ServerPlayer player, String path) {
        ServerAdvancementManager manager = player.server.getAdvancements();
        ResourceLocation id = EnderPortalsMod.id(path);
        AdvancementHolder holder = manager.get(id);
        if (holder == null) {
            EnderPortalsMod.LOGGER.warn("Progrès introuvable : {}", id);
            return;
        }
        player.getAdvancements().award(holder, CRITERION);
    }

    private ModAdvancements() {
    }
}
