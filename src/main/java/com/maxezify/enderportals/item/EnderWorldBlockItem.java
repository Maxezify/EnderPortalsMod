package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModDimensions;
import net.minecraft.ChatFormatting;
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
 * BlockItem d'une machine qui n'a de sens que dans une base de poche : la pose
 * échoue ailleurs, et l'objet est conservé.
 *
 * <p>Le message de refus est fourni par l'appelant : un joueur qui essaie de
 * poser un Passage des Alliés dans l'Overworld n'a pas besoin qu'on lui parle du
 * Transmetteur.</p>
 */
public class EnderWorldBlockItem extends BlockItem {

    private final String refusalKey;
    private final String tooltipKey;

    /**
     * @param refusalKey clé du message affiché quand la pose est refusée
     * @param tooltipKey préfixe des lignes d'infobulle, suffixées {@code _1} et
     *                   {@code _2} : deux lignes suffisent à dire ce que fait la
     *                   machine et où elle se pose
     */
    public EnderWorldBlockItem(Block block, Properties properties, String refusalKey, String tooltipKey) {
        super(block, properties);
        this.refusalKey = refusalKey;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey + "_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(tooltipKey + "_2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().dimension().equals(ModDimensions.ENDER_WORLD)) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable(refusalKey), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
