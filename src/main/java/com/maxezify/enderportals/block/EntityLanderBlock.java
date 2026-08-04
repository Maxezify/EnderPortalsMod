package com.maxezify.enderportals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * L'Atterrisseur d'entité : la dalle sur laquelle un Téléporteur vient se
 * poser.
 *
 * <p>Le bloc ne retient rien, et c'est voulu : le lien est porté par le
 * Téléporteur, qui note la position de l'Atterrisseur choisi. Plusieurs
 * machines peuvent donc viser le même — il n'a rien à compter — et un
 * Atterrisseur cassé puis reposé au même endroit reprend son rôle sans que
 * personne n'ait à relier quoi que ce soit.</p>
 *
 * <p>Il reste un bloc plein : la coque arrive <b>dessus</b>, et une dalle
 * partielle aurait laissé le bateau flotter au-dessus de sa surface.</p>
 */
public class EntityLanderBlock extends Block {

    public EntityLanderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Quelques étincelles au-dessus de la dalle : de loin, c'est ce qui
     * distingue un Atterrisseur d'un bloc de décoration dans une base qui en
     * compte plusieurs.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.END_ROD, x, pos.getY() + 1.05, z, 0.0, 0.015, 0.0);
    }
}
