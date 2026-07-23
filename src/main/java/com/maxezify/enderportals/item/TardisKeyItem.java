package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModComponents;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    public TardisKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        ServerWorld serverWorld = (ServerWorld) world;
        MinecraftServer server = serverWorld.getServer();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        ItemStack stack = context.getStack();

        if (state.isOf(ModBlocks.TARDIS_DOOR)) {
            handleDoorClick(server, serverWorld, pos, state, player, stack);
            return ActionResult.SUCCESS;
        }
        if (state.isOf(ModBlocks.INACTIVE_TARDIS_DOOR)) {
            player.sendMessage(Text.translatable("enderportals.message.door_hint"), true);
            return ActionResult.SUCCESS;
        }
        handleGroundClick(server, serverWorld, context, player, stack);
        return ActionResult.SUCCESS;
    }

    private static void handleDoorClick(MinecraftServer server, ServerWorld world, BlockPos pos, BlockState state,
                                        PlayerEntity player, ItemStack stack) {
        BlockPos base = state.get(TardisDoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        if (!(world.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.isDematerializing() || door.getTardisId() == null) {
            return;
        }
        String bound = stack.get(ModComponents.TARDIS_ID);
        if (bound == null) {
            stack.set(ModComponents.TARDIS_ID, door.getTardisId().toString());
            world.playSound(null, base, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
            player.sendMessage(Text.translatable("enderportals.message.key_bound"), false);
            return;
        }
        if (!bound.equals(door.getTardisId().toString())) {
            player.sendMessage(Text.translatable("enderportals.message.wrong_key"), true);
            world.playSound(null, base, SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.8f, 0.6f);
            return;
        }
        TardisData data = TardisStateManager.get(server).getTardis(door.getTardisId());
        if (data == null) {
            return;
        }
        if (door.isInterior()) {
            if (data.deployed) {
                TardisHelper.dismissExterior(server, data);
                player.sendMessage(Text.translatable("enderportals.message.tardis_dismissed"), true);
            } else {
                ServerWorld exteriorWorld = server.getWorld(data.exteriorWorld);
                if (exteriorWorld == null
                        || !TardisHelper.deployExterior(server, data, exteriorWorld,
                                data.exteriorPos, data.exteriorFacing, true, player)) {
                    player.sendMessage(Text.translatable("enderportals.message.no_space"), true);
                } else {
                    player.sendMessage(Text.translatable("enderportals.message.tardis_recalled"), true);
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

    private static void handleGroundClick(MinecraftServer server, ServerWorld world, ItemUsageContext context,
                                          PlayerEntity player, ItemStack stack) {
        String bound = stack.get(ModComponents.TARDIS_ID);
        if (bound == null) {
            player.sendMessage(Text.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        if (world.getRegistryKey().equals(ModDimensions.ENDER_WORLD)) {
            player.sendMessage(Text.translatable("enderportals.message.already_inside"), true);
            return;
        }
        TardisData data;
        try {
            data = TardisStateManager.get(server).getTardis(UUID.fromString(bound));
        } catch (IllegalArgumentException e) {
            data = null;
        }
        if (data == null) {
            player.sendMessage(Text.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        BlockPos clicked = context.getBlockPos();
        BlockPos base = world.getBlockState(clicked).isReplaceable() ? clicked : clicked.offset(context.getSide());
        // Respecte la spawn protection, le mode aventure et les mods de claim.
        if (!world.canPlayerModifyAt(player, base) || !world.canPlayerModifyAt(player, base.up())) {
            player.sendMessage(Text.translatable("enderportals.message.protected"), true);
            return;
        }
        Direction facing = player.getHorizontalFacing().getOpposite();
        TardisHelper.deployExterior(server, data, world, base, facing, false, player);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String bound = stack.get(ModComponents.TARDIS_ID);
        if (bound != null) {
            tooltip.add(Text.translatable("enderportals.tooltip.key_bound",
                    bound.substring(0, 8)).formatted(Formatting.AQUA));
        } else {
            tooltip.add(Text.translatable("enderportals.tooltip.key_unbound").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}
