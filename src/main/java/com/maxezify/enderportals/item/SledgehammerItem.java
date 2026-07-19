package com.maxezify.enderportals.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * La Masse. Frappez (clic gauche) une porte inactive perchée en hauteur pour
 * éveiller le TARDIS — la logique est branchée sur AttackBlockCallback dans
 * {@link com.maxezify.enderportals.EnderPortalsMod}.
 */
public class SledgehammerItem extends Item {

    public SledgehammerItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("enderportals.tooltip.sledgehammer").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
