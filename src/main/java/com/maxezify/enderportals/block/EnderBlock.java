package com.maxezify.enderportals.block;

import com.maxezify.enderportals.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

    /**
     * Le carillon de l'End, posé par-dessus le bruit de casse.
     *
     * <p>Un {@code SoundType} ne porte qu'un son de casse, jamais deux : le
     * mélange demandé — le mou du miel et le mystique de l'End — ne peut donc
     * pas se déclarer, il se joue. Le miel vient du {@code SoundType} du bloc,
     * ce carillon vient d'ici, et les deux partent au même instant.</p>
     *
     * <p>Joué depuis le serveur pour que tout le monde l'entende, et non
     * seulement celui qui creuse : dans une galerie à deux, on doit s'entendre
     * travailler.</p>
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS,
                    0.35f, 1.35f + level.getRandom().nextFloat() * 0.25f);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
