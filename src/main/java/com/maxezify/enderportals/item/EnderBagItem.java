package com.maxezify.enderportals.item;

import com.maxezify.enderportals.tardis.CentralizerLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Le Sac de l'Ender. Porté en seconde main (offhand), un clic droit range la
 * ligne du haut de l'inventaire dans les coffres reliés au Centraliseur, au
 * prix d'un peu d'expérience. Toute la logique vit dans
 * {@link CentralizerLogic}.
 */
public class EnderBagItem extends Item {

    public EnderBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand != Hand.OFF_HAND) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            CentralizerLogic.deposit(player);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("enderportals.tooltip.ender_bag").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
