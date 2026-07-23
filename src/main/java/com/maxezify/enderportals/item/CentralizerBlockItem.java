package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * BlockItem du Centraliseur : la pose n'est autorisée que dans le monde de
 * l'Ender. Ailleurs, le placement échoue et l'objet est conservé.
 */
public class CentralizerBlockItem extends BlockItem {

    public CentralizerBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    protected ActionResult place(ItemPlacementContext context) {
        if (!context.getWorld().getRegistryKey().equals(ModDimensions.ENDER_WORLD)) {
            PlayerEntity player = context.getPlayer();
            if (player != null && !context.getWorld().isClient) {
                player.sendMessage(Text.translatable("enderportals.message.centralizer_here"), true);
            }
            return ActionResult.FAIL;
        }
        return super.place(context);
    }
}
