package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * La Pioche de l'Ender. C'est un {@link Item} ordinaire, et non un
 * {@code TieredItem} : sa vitesse et ses règles de récolte viennent d'un
 * composant {@code Tool} sur mesure, qui seul permet de la rendre très rapide
 * sur les Blocs de l'Ender tout en restant de niveau diamant ailleurs.
 *
 * <p>La contrepartie de ce choix est que tout ce que {@code TieredItem} tire
 * d'office du palier doit être redéclaré ici :</p>
 * <ul>
 *   <li>la <b>réparation à l'enclume</b> — sans cette surcharge,
 *       {@link Item#isValidRepairItem} refuse tout, et la pioche n'était
 *       réparable qu'en combinant deux exemplaires abîmés ;</li>
 *   <li>l'<b>enchantabilité</b> — en 1.21.1 la table d'enchantement lit
 *       {@link Item#getEnchantmentValue()}, qui vaut 0 par défaut : la pioche
 *       n'obtenait donc aucune proposition, malgré ses entrées dans les tags
 *       {@code #minecraft:enchantable/*}.</li>
 * </ul>
 */
public class EnderPickaxeItem extends Item {

    public EnderPickaxeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.ENDER_CRYSTAL.get());
    }

    @Override
    public int getEnchantmentValue() {
        return EnderToolMaterial.INSTANCE.getEnchantmentValue();
    }
}
