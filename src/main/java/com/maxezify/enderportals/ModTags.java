package com.maxezify.enderportals;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    /** Blocs que la pioche de l'Ender casse très vite et fait toujours tomber. */
    public static final TagKey<Block> ENDER_PICKAXE_FAST =
            TagKey.create(Registries.BLOCK, EnderPortalsMod.id("ender_pickaxe_fast"));

    /**
     * Ce qu'un <b>visiteur</b> peut actionner dans la parcelle d'un autre :
     * portes, trappes, portillons, boutons, plaques de pression.
     *
     * <p>Une <b>liste blanche</b>, et c'est tout l'intérêt. La 0.29.0 essayait
     * l'inverse — reconnaître les rangements pour les refuser — et les coffres
     * de Sophisticated Storage passaient au travers, faute d'exposer ce qu'on
     * cherchait. Une liste noire doit connaître à l'avance tout ce qui existe ;
     * une liste blanche refuse par défaut ce qu'elle ne connaît pas, y compris
     * le rangement du prochain mod installé.</p>
     *
     * <p>Elle est faite de tags de vanilla plutôt que de blocs nommés un à un :
     * les portes des mods qui s'inscrivent dans {@code #minecraft:doors} en
     * profitent, sans que personne ait à les recenser. Le levier en est
     * volontairement absent — il s'enclenche et reste, là où un bouton retombe
     * — et un pack qui le veut n'a qu'une ligne à ajouter à ce tag.</p>
     *
     * <p>Les cinq références y sont déclarées <b>facultatives</b>. Une liste
     * blanche échoue du côté sûr : un tag introuvable resserre la garde au lieu
     * de l'ouvrir, et il n'y a donc aucune raison de faire échouer le chargement
     * du pack pour cela.</p>
     */
    public static final TagKey<Block> VISITOR_USABLE =
            TagKey.create(Registries.BLOCK, EnderPortalsMod.id("visitor_usable"));

    private ModTags() {
    }
}
