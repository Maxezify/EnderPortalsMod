package com.maxezify.enderportals.block;

import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Le Centraliseur d'objet : machine aux voyants clignotants façon gros
 * ordinateur des années 50. Posable uniquement dans le monde de l'Ender (via
 * {@link com.maxezify.enderportals.item.CentralizerBlockItem}). Sans état
 * propre : le Sac de l'Ender le retrouve via la position enregistrée dans le
 * TARDIS de son propriétaire, puis parcourt les coffres accolés.
 */
public class CentralizerBlock extends Block {

    public CentralizerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player && level.getServer() != null) {
            TardisStateManager.get(level.getServer()).setCentralizer(player.getUUID(), pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getServer() != null) {
            TardisStateManager.get(level.getServer()).clearCentralizer(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Voyants clignotants : étincelles colorées (sarcelle via SCRAPE,
        // violet via WITCH) surgissant au hasard sur les faces.
        for (int i = 0; i < 2; i++) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.SCRAPE : ParticleTypes.WITCH,
                    pos.getX() + edgeOrFace(random),
                    pos.getY() + 0.15 + random.nextDouble() * 0.7,
                    pos.getZ() + edgeOrFace(random),
                    0.0, 0.0, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + edgeOrFace(random),
                    pos.getY() + 0.2 + random.nextDouble() * 0.6,
                    pos.getZ() + edgeOrFace(random),
                    0.0, 0.0, 0.0);
        }
    }

    private static double edgeOrFace(RandomSource random) {
        return random.nextBoolean() ? (random.nextBoolean() ? 0.02 : 0.98) : random.nextDouble();
    }
}
