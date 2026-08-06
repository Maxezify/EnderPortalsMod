package com.maxezify.enderportals.item;

import com.maxezify.enderportals.tardis.CentralizerLogic;
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
 * Le Sac de l'Ender : une pile et le sac se rencontrent sous le curseur, clic
 * droit, et la pile part dans les rangements de la base. Toute la logique vit
 * dans {@link CentralizerLogic}.
 *
 * <p>Le geste est celui des bourses de vanilla, et il passe par les mêmes
 * crochets. Le joueur n'a donc rien de nouveau à apprendre, et le sac n'a plus
 * besoin d'occuper la seconde main pour servir — c'était le prix de l'ancienne
 * version, qui envoyait la rangée entière d'un clic droit hors inventaire.</p>
 *
 * <p><b>Ce crochet tourne des deux côtés.</b> Le menu du client rejoue le clic
 * pour prédire ce qu'il va afficher, puis le serveur le rejoue pour de bon. Le
 * client, lui, ne sait rien des coffres de la base : il ne peut pas prédire si
 * la pile passera. Il ne touche donc à rien et se contente de <b>consommer</b>
 * le clic — sans quoi il échangerait la pile et le sac le temps d'un aller-
 * retour, ce qui se verrait. Le serveur fait le travail et corrige l'écran au
 * paquet suivant : la pile s'efface du curseur un aller-retour plus tard.</p>
 *
 * <p>Les <b>deux sens</b> existent, comme pour les bourses : le sac dans une
 * case et la pile au curseur, ou le sac au curseur et la pile dans la case. Le
 * second a un piège qu'il vaut mieux connaître — on déplace un sac dans son
 * inventaire plus souvent qu'on ne range, et pendant ce déplacement un clic
 * droit sur une pile l'expédie. C'est le prix d'un geste symétrique, et le
 * même que paient les bourses de vanilla.</p>
 */
public class EnderBagItem extends Item {

    public EnderBagItem(Properties properties) {
        super(properties);
    }

    /**
     * Le sac est au curseur, on clique une pile : elle part.
     *
     * <p>Vanilla appelle ce crochet sur la pile <b>portée</b> ; l'autre, sur
     * celle qui dort dans la case. C'est toute la différence entre les deux
     * sens, et c'est pourquoi il en faut deux.</p>
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || slot.getItem().isEmpty()) {
            // Case vide : c'est un dépôt de sac ordinaire, laissons faire.
            return false;
        }
        if (!slot.allowModification(player)) {
            return false;
        }
        if (player instanceof ServerPlayer server) {
            CentralizerLogic.sendSlot(server, slot);
        }
        return true;
    }

    /** Le sac est dans une case, on lui apporte une pile au curseur. */
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
        // Le prix vient de la logique : une infobulle qui ment sur un coût est
        // pire que pas d'infobulle du tout.
        EnderTooltip.details(tooltip,
                EnderTooltip.head("enderportals.tooltip.ender_bag"),
                EnderTooltip.head("enderportals.tooltip.ender_bag_reverse"),
                EnderTooltip.line("enderportals.tooltip.ender_bag_price",
                        CentralizerLogic.XP_COST_PER_STACK));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
