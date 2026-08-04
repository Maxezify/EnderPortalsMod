package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModComponents;
import com.maxezify.enderportals.ModEntities;
import com.maxezify.enderportals.entity.EntityTeleporterEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Le Téléporteur d'entité en main : il se lie, puis il se pose.
 *
 * <p>Un seul geste, deux effets selon ce que l'on vise. Sur un Atterrisseur, il
 * note la destination ; ailleurs, il pose la coque au sol. C'est ce qui permet
 * de préparer la machine dans sa base — là où sont les Atterrisseurs — et de
 * n'emporter dehors qu'un objet déjà réglé.</p>
 *
 * <p>La destination voyage sur l'objet, pas dans un registre : une machine
 * donnée à quelqu'un garde son réglage, et deux Téléporteurs de la même pile
 * <b>ne se mélangent pas</b> — un composant différent sépare les piles.</p>
 */
public class EntityTeleporterItem extends Item {

    public EntityTeleporterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(clicked).is(ModBlocks.ENTITY_LANDER.get())) {
            if (!level.isClientSide) {
                stack.set(ModComponents.LANDER_POS.get(), clicked);
                level.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                        0.8f, 1.6f);
                if (player != null) {
                    player.displayClientMessage(Component.translatable(
                            "enderportals.message.teleporter_linked",
                            clicked.getX(), clicked.getY(), clicked.getZ()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos target = clicked.relative(context.getClickedFace());
        if (!level.getBlockState(target).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            EntityTeleporterEntity machine = ModEntities.ENTITY_TELEPORTER.get().create(level);
            if (machine == null) {
                return InteractionResult.FAIL;
            }
            machine.setLander(stack.get(ModComponents.LANDER_POS.get()));
            Vec3 spot = Vec3.atBottomCenterOf(target);
            machine.moveTo(spot.x, spot.y, spot.z, player == null ? 0.0f : player.getYRot() + 180.0f, 0.0f);
            level.addFreshEntity(machine);
            level.playSound(null, target, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enderportals.tooltip.entity_teleporter_1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("enderportals.tooltip.entity_teleporter_2")
                .withStyle(ChatFormatting.DARK_GRAY));
        BlockPos lander = stack.get(ModComponents.LANDER_POS.get());
        tooltip.add(lander == null
                ? Component.translatable("enderportals.tooltip.entity_teleporter_free")
                        .withStyle(ChatFormatting.RED)
                : Component.translatable("enderportals.tooltip.entity_teleporter_bound",
                        lander.getX(), lander.getY(), lander.getZ()).withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
