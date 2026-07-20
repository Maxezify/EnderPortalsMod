package com.maxezify.enderportals.block;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.tardis.TardisHelper;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * La porte inactive : une porte "normale" (posable, cassable à la pioche).
 * Pour l'éveiller, il faut reproduire l'attaque écrasante de la Mace :
 * chuter d'au moins {@link EnderPortalsMod#ACTIVATION_FALL_DISTANCE} blocs
 * et la frapper à la Mace pendant la chute. L'impact absorbe les dégâts de
 * chute du joueur.
 */
public class InactiveTardisDoorBlock extends DoorBlock {

    public InactiveTardisDoorBlock(BlockSetType type, Settings settings) {
        super(type, settings);
    }

    /**
     * Rituel de la Mace. Retourne true si la porte s'est éveillée.
     */
    public static boolean tryActivate(ServerWorld world, BlockPos pos, ServerPlayerEntity player, ItemStack mace) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof InactiveTardisDoorBlock)) {
            return false;
        }
        BlockPos base = state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;

        float fall = player.fallDistance;
        if (fall < EnderPortalsMod.ACTIVATION_FALL_DISTANCE) {
            player.sendMessage(Text.translatable("enderportals.message.not_falling",
                    (int) EnderPortalsMod.ACTIVATION_FALL_DISTANCE, (int) fall), true);
            world.playSound(null, base, SoundEvents.ITEM_MACE_SMASH_AIR, SoundCategory.PLAYERS, 0.8f, 0.9f);
            return false;
        }

        // L'impact absorbe la chute, comme l'attaque écrasante de la Mace.
        player.fallDistance = 0.0f;

        Vec3d impact = Vec3d.ofBottomCenter(base);
        world.playSound(null, base, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.2f, 0.8f);
        world.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, impact.getX(), impact.getY(), impact.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);

        Direction facing = world.getBlockState(base).get(DoorBlock.FACING);
        TardisHelper.activate(world, base, facing, player);
        mace.damage(10, player, EquipmentSlot.MAINHAND);
        return true;
    }
}
