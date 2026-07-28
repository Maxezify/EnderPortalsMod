package com.maxezify.enderportals;

import com.maxezify.enderportals.item.CentralizerBlockItem;
import com.maxezify.enderportals.item.EnderBagItem;
import com.maxezify.enderportals.item.EnderPickaxeItem;
import com.maxezify.enderportals.item.EnderToolMaterial;
import com.maxezify.enderportals.item.TardisKeyItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnderPortalsMod.MODID);

    /** Cristal de l'Ender — récolté sur le minerai de l'End. */
    public static final DeferredItem<Item> ENDER_CRYSTAL = ITEMS.register("ender_crystal",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** La Clé de l'Ender — matérialise la porte et l'ouvre. */
    public static final DeferredItem<TardisKeyItem> TARDIS_KEY = ITEMS.register("tardis_key",
            () -> new TardisKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Pioche de l'Ender — seule capable de récolter les Blocs de l'Ender. */
    public static final DeferredItem<EnderPickaxeItem> ENDER_PICKAXE = ITEMS.register("ender_pickaxe",
            () -> new EnderPickaxeItem(new Item.Properties()
                    .durability(2031)
                    .rarity(Rarity.RARE)
                    .attributes(PickaxeItem.createAttributes(EnderToolMaterial.INSTANCE, 1.0f, -2.8f))
                    .component(DataComponents.TOOL, createEnderPickaxeTool())));

    public static final DeferredItem<BlockItem> INACTIVE_TARDIS_DOOR = ITEMS.register("inactive_tardis_door",
            () -> new BlockItem(ModBlocks.INACTIVE_TARDIS_DOOR.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<BlockItem> ENDER_ORE = ITEMS.register("ender_ore",
            () -> new BlockItem(ModBlocks.ENDER_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ENDER_BLOCK = ITEMS.register("ender_block",
            () -> new BlockItem(ModBlocks.ENDER_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ENDER_BRICKS = ITEMS.register("ender_bricks",
            () -> new BlockItem(ModBlocks.ENDER_BRICKS.get(), new Item.Properties()));

    public static final DeferredItem<CentralizerBlockItem> CENTRALIZER = ITEMS.register("centralizer",
            () -> new CentralizerBlockItem(ModBlocks.CENTRALIZER.get(), new Item.Properties().rarity(Rarity.RARE)));

    /** Le Sac de l'Ender — en seconde main, range la ligne du haut dans la base. */
    public static final DeferredItem<EnderBagItem> ENDER_BAG = ITEMS.register("ender_bag",
            () -> new EnderBagItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /**
     * Composant d'outil de la pioche de l'Ender : très rapide sur les Blocs de
     * l'Ender (et seule à les faire tomber), niveau diamant pour le reste.
     */
    private static Tool createEnderPickaxeTool() {
        return new Tool(List.of(
                Tool.Rule.minesAndDrops(ModTags.ENDER_PICKAXE_FAST, 45.0f),
                Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 8.0f)
        ), 1.0f, 1);
    }

    private ModItems() {
    }
}
