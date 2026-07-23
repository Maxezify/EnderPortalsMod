package com.maxezify.enderportals.block;

import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Le Centraliseur d'objet : une machine façon gros ordinateur des années 50,
 * dont les voyants clignotent. Posable uniquement dans le monde de l'Ender
 * (via {@link com.maxezify.enderportals.item.CentralizerBlockItem}). Il ne
 * porte pas d'état propre : le Sac de l'Ender le retrouve via la position
 * enregistrée dans le TARDIS de son propriétaire, puis parcourt les coffres
 * accolés.
 */
public class CentralizerBlock extends Block {

    public CentralizerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient && placer instanceof ServerPlayerEntity player && world.getServer() != null) {
            TardisStateManager.get(world.getServer()).setCentralizer(player.getUuid(), pos);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world.getServer() != null) {
            TardisStateManager.get(world.getServer()).clearCentralizer(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Voyants clignotants : de petites étincelles colorées (sarcelle via
        // SCRAPE, violet via WITCH) surgissent au hasard sur les faces.
        for (int i = 0; i < 2; i++) {
            world.addParticle(random.nextBoolean() ? ParticleTypes.SCRAPE : ParticleTypes.WITCH,
                    pos.getX() + edgeOrFace(random),
                    pos.getY() + 0.15 + random.nextDouble() * 0.7,
                    pos.getZ() + edgeOrFace(random),
                    0.0, 0.0, 0.0);
        }
        if (random.nextInt(6) == 0) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + edgeOrFace(random),
                    pos.getY() + 0.2 + random.nextDouble() * 0.6,
                    pos.getZ() + edgeOrFace(random),
                    0.0, 0.0, 0.0);
        }
    }

    /** Renvoie une coordonnée collée à une face (0.02 ou 0.98) ou libre sur l'autre axe. */
    private static double edgeOrFace(Random random) {
        return random.nextBoolean() ? (random.nextBoolean() ? 0.02 : 0.98) : random.nextDouble();
    }
}
