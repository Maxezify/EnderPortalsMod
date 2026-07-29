package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

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

    public EnderWorldBlockItem(Block block, Properties properties, String refusalKey) {
        super(block, properties);
        this.refusalKey = refusalKey;
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
