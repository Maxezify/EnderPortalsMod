package com.maxezify.enderportals.item;

import com.maxezify.enderportals.tardis.CentralizerLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Le Sac de l'Ender : une pile prise au curseur, un clic droit sur le sac, et
 * elle part dans les rangements de la base. Toute la logique vit dans
 * {@link CentralizerLogic}.
 *
 * <p>Le geste est celui des bourses de vanilla, et il passe par le même
 * mécanisme : {@code overrideOtherStackedOnMe} est appelé quand on lâche une
 * pile sur un objet posé dans une case. Le joueur n'a donc rien de nouveau à
 * apprendre, et le sac n'a plus besoin d'occuper la seconde main pour servir —
 * c'était le prix de l'ancienne version, qui envoyait la rangée entière d'un
 * clic droit hors inventaire.</p>
 *
 * <p><b>Ce crochet tourne des deux côtés.</b> Le menu du client rejoue le clic
 * pour prédire ce qu'il va afficher, puis le serveur le rejoue pour de bon. Le
 * client, lui, ne sait rien des coffres de la base : il ne peut pas prédire si
 * la pile passera. Il ne touche donc à rien et se contente de <b>consommer</b>
 * le clic — sans quoi il échangerait la pile et le sac le temps d'un aller-
 * retour, ce qui se verrait. Le serveur fait le travail et corrige l'écran au
 * paquet suivant : la pile s'efface du curseur un aller-retour plus tard.</p>
 *
 * <p>Le geste inverse — porter le sac au curseur et cliquer une pile pour
 * l'aspirer — n'est délibérément pas implémenté, bien que les bourses le
 * fassent. On déplace un sac dans son inventaire plus souvent qu'on ne range :
 * un clic droit malheureux pendant ce rangement enverrait une pile à l'autre
 * bout du monde, et coûterait de l'expérience pour la peine.</p>
 */
public class EnderBagItem extends Item {

    public EnderBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack carried, Slot slot,
                                            ClickAction action, Player player, SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || carried.isEmpty()) {
            // Curseur vide, ou clic gauche : c'est un déplacement de sac
            // ordinaire, et vanilla le fait mieux que nous.
            return false;
        }
        if (!slot.allowModification(player)) {
            // Case de résultat d'établi, inventaire d'un autre en lecture
            // seule : on n'y touche pas.
            return false;
        }
        if (player instanceof ServerPlayer server) {
            CentralizerLogic.sendCarried(server, carried, carriedAccess);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("enderportals.tooltip.ender_bag")
                .withStyle(ChatFormatting.GRAY));
        // Le prix vient de la logique : une infobulle qui ment sur un coût est
        // pire que pas d'infobulle du tout.
        tooltip.add(Component.translatable("enderportals.tooltip.ender_bag_price",
                CentralizerLogic.XP_COST_PER_STACK).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
