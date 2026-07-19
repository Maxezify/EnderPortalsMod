package com.maxezify.enderportals.block;

import net.minecraft.block.TransparentBlock;

/**
 * Le Bloc de l'Ender : semi-transparent comme du verre teinté gris. Les faces
 * entre deux blocs de l'Ender sont invisibles (héritées de
 * {@link TransparentBlock}), si bien qu'on voit au travers de la masse et
 * qu'on "entrevoit" les blocs-reliques pris dedans.
 */
public class EnderBlock extends TransparentBlock {

    public EnderBlock(Settings settings) {
        super(settings);
    }
}
