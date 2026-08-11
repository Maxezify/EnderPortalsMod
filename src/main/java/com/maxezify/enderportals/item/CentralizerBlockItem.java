package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * BlockItem du Centraliseur : la pose n'est autorisée que dans le monde de
 * l'Ender. Ailleurs, le placement échoue et l'objet est conservé.
 */
public class CentralizerBlockItem extends BlockItem {

    public CentralizerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        EnderTooltip.details(tooltip, flag,
                EnderTooltip.head("enderportals.tooltip.centralizer_1"),
                EnderTooltip.line("enderportals.tooltip.centralizer_2"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().dimension().equals(ModDimensions.ENDER_WORLD)) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("enderportals.message.centralizer_here"), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
