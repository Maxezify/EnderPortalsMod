package com.maxezify.enderportals.block;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.tardis.TardisHelper;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * La porte inactive : une porte "normale" (posable, cassable à la pioche).
 * Frappée à la masse alors qu'elle domine un vide d'au moins
 * {@link EnderPortalsMod#ACTIVATION_HEIGHT} blocs, elle s'éveille et devient
 * un TARDIS.
 */
public class InactiveTardisDoorBlock extends DoorBlock {

    public InactiveTardisDoorBlock(BlockSetType type, Settings settings) {
        super(type, settings);
    }

    /**
     * Rituel de la masse. Retourne true si la porte s'est éveillée.
     */
    public static boolean tryActivate(ServerWorld world, BlockPos pos, ServerPlayerEntity player, ItemStack hammer) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof InactiveTardisDoorBlock)) {
            return false;
        }
        BlockPos base = state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;

        int clearance = maxDropAround(world, base);
        if (clearance < EnderPortalsMod.ACTIVATION_HEIGHT) {
            player.sendMessage(Text.translatable("enderportals.message.not_high_enough",
                    EnderPortalsMod.ACTIVATION_HEIGHT, clearance), true);
            world.playSound(null, base, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.6f, 0.5f);
            return false;
        }

        Direction facing = world.getBlockState(base).get(DoorBlock.FACING);
        TardisHelper.activate(world, base, facing, player);
        hammer.damage(10, player, EquipmentSlot.MAINHAND);
        return true;
    }

    /**
     * Plus grande hauteur d'air libre sous les quatre colonnes voisines de la
     * base de la porte (la porte doit trôner au sommet d'un pilier).
     */
    private static int maxDropAround(ServerWorld world, BlockPos base) {
        int best = 0;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos.Mutable cursor = base.offset(dir).mutableCopy();
            int drop = 0;
            while (cursor.getY() > world.getBottomY() && world.getBlockState(cursor).isAir()) {
                drop++;
                cursor.move(Direction.DOWN);
            }
            best = Math.max(best, drop);
        }
        return best;
    }
}
