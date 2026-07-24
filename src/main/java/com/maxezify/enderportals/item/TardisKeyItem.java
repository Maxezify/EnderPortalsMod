package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModComponents;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;
import java.util.UUID;

/**
 * La Clé du TARDIS.
 * <ul>
 *   <li>Clic droit sur une porte active non liée : lie la clé.</li>
 *   <li>Clic droit par terre : matérialise la porte à l'endroit visé (fondu).</li>
 *   <li>Clic droit sur la porte extérieure : l'ouvre ; re-clic : la referme
 *       et la fait disparaître en fondu.</li>
 *   <li>Clic droit sur la porte intérieure : rappelle ou dématérialise la
 *       porte extérieure.</li>
 * </ul>
 */
public class TardisKeyItem extends Item {

    public TardisKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        MinecraftServer server = serverLevel.getServer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if (state.is(ModBlocks.TARDIS_DOOR.get())) {
            handleDoorClick(server, serverLevel, pos, state, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (state.is(ModBlocks.INACTIVE_TARDIS_DOOR.get())) {
            player.displayClientMessage(Component.translatable("enderportals.message.door_hint"), true);
            return InteractionResult.SUCCESS;
        }
        handleGroundClick(server, serverLevel, context, player, stack);
        return InteractionResult.SUCCESS;
    }

    private static void handleDoorClick(MinecraftServer server, ServerLevel level, BlockPos pos, BlockState state,
                                        Player player, ItemStack stack) {
        BlockPos base = state.getValue(TardisDoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!(level.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.isDematerializing() || door.getTardisId() == null) {
            return;
        }
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound == null) {
            stack.set(ModComponents.TARDIS_ID.get(), door.getTardisId().toString());
            level.playSound(null, base, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.displayClientMessage(Component.translatable("enderportals.message.key_bound"), false);
            return;
        }
        if (!bound.equals(door.getTardisId().toString())) {
            player.displayClientMessage(Component.translatable("enderportals.message.wrong_key"), true);
            level.playSound(null, base, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            return;
        }
        TardisData data = TardisStateManager.get(server).getTardis(door.getTardisId());
        if (data == null) {
            return;
        }
        if (door.isInterior()) {
            if (data.deployed) {
                TardisHelper.dismissExterior(server, data);
                player.displayClientMessage(Component.translatable("enderportals.message.tardis_dismissed"), true);
            } else {
                ServerLevel exteriorWorld = server.getLevel(data.exteriorWorld);
                if (exteriorWorld == null
                        || !TardisHelper.deployExterior(server, data, exteriorWorld,
                                data.exteriorPos, data.exteriorFacing, true, player)) {
                    player.displayClientMessage(Component.translatable("enderportals.message.no_space"), true);
                } else {
                    player.displayClientMessage(Component.translatable("enderportals.message.tardis_recalled"), true);
                }
            }
        } else {
            if (!data.open) {
                TardisHelper.setDoorsOpen(server, data, true);
            } else {
                TardisHelper.dismissExterior(server, data);
            }
        }
    }

    private static void handleGroundClick(MinecraftServer server, ServerLevel level, UseOnContext context,
                                          Player player, ItemStack stack) {
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        if (level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            player.displayClientMessage(Component.translatable("enderportals.message.already_inside"), true);
            return;
        }
        TardisData data;
        try {
            data = TardisStateManager.get(server).getTardis(UUID.fromString(bound));
        } catch (IllegalArgumentException e) {
            data = null;
        }
        if (data == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos base = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
        // Respecte la spawn protection, le mode aventure et les mods de claim.
        if (!level.mayInteract(player, base) || !level.mayInteract(player, base.above())) {
            player.displayClientMessage(Component.translatable("enderportals.message.protected"), true);
            return;
        }
        Direction facing = player.getDirection().getOpposite();
        TardisHelper.deployExterior(server, data, level, base, facing, false, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound != null) {
            tooltip.add(Component.translatable("enderportals.tooltip.key_bound",
                    bound.substring(0, 8)).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("enderportals.tooltip.key_unbound").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
