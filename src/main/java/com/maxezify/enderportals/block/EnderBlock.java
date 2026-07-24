package com.maxezify.enderportals.block;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Le Bloc de l'Ender : semi-transparent comme du verre teinté gris. Les faces
 * entre deux blocs de l'Ender sont invisibles (héritées de
 * {@link TransparentBlock}), si bien qu'on voit au travers de la masse.
 */
public class EnderBlock extends TransparentBlock {

    public EnderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
