package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le Bloc de l'Ender : semi-transparent comme du verre teinté gris. Les faces
 * entre deux blocs de l'Ender sont invisibles (héritées de
 * {@link TransparentBlock}), si bien qu'on voit au travers de la masse.
 *
 * <p>Il n'est récolté que par la Pioche de l'Ender — ou par n'importe quelle
 * pioche portant l'enchantement « Brisure d'Espace-Temps ».</p>
 */
public class EnderBlock extends TransparentBlock {

    public EnderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        if (ModEnchantments.allowsEnderBlock(player.level(), tool)) {
            return true;
        }
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }
}
