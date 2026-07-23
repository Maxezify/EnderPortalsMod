package com.maxezify.enderportals;

import com.maxezify.enderportals.item.CentralizerBlockItem;
import com.maxezify.enderportals.item.EnderBagItem;
import com.maxezify.enderportals.item.EnderToolMaterial;
import com.maxezify.enderportals.item.TardisKeyItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Rarity;

import java.util.List;
import java.util.Optional;

public final class ModItems {

    /** Cristal de l'Ender — récolté sur le minerai de l'End. */
    public static final Item ENDER_CRYSTAL = register("ender_crystal",
            new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));

    /** La Clé du TARDIS — matérialise la porte et l'ouvre. */
    public static final Item TARDIS_KEY = register("tardis_key",
            new TardisKeyItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .fireproof()));

    /** Pioche de l'Ender — seule capable de récolter les blocs de l'Ender. */
    public static final Item ENDER_PICKAXE = register("ender_pickaxe",
            new Item(new Item.Settings()
                    .maxCount(1)
                    .maxDamage(2031)
                    .rarity(Rarity.RARE)
                    .attributeModifiers(MiningToolItem.createAttributeModifiers(
                            EnderToolMaterial.INSTANCE, 1.0f, -2.8f))
                    .component(DataComponentTypes.TOOL, createEnderPickaxeTool())));

    public static final Item INACTIVE_TARDIS_DOOR = register("inactive_tardis_door",
            new BlockItem(ModBlocks.INACTIVE_TARDIS_DOOR, new Item.Settings().rarity(Rarity.EPIC).fireproof()));

    public static final Item ENDER_ORE = register("ender_ore",
            new BlockItem(ModBlocks.ENDER_ORE, new Item.Settings()));

    public static final Item ENDER_BLOCK = register("ender_block",
            new BlockItem(ModBlocks.ENDER_BLOCK, new Item.Settings()));

    public static final Item ENDER_BRICKS = register("ender_bricks",
            new BlockItem(ModBlocks.ENDER_BRICKS, new Item.Settings()));

    public static final Item CENTRALIZER = register("centralizer",
            new CentralizerBlockItem(ModBlocks.CENTRALIZER, new Item.Settings().rarity(Rarity.RARE)));

    /** Le Sac de l'Ender — en seconde main, range la ligne du haut dans la base. */
    public static final Item ENDER_BAG = register("ender_bag",
            new EnderBagItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

    /**
     * Outil de la pioche de l'Ender : très rapide sur les blocs de l'Ender
     * (et seule à les faire tomber), niveau diamant pour le reste.
     */
    private static ToolComponent createEnderPickaxeTool() {
        return new ToolComponent(List.of(
                ToolComponent.Rule.ofAlwaysDropping(ModTags.ENDER_PICKAXE_FAST, 45.0f),
                new ToolComponent.Rule(Registries.BLOCK.getOrCreateEntryList(BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
                        Optional.empty(), Optional.of(false)),
                ToolComponent.Rule.ofAlwaysDropping(BlockTags.PICKAXE_MINEABLE, 8.0f)
        ), 1.0f, 1);
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, EnderPortalsMod.id(name), item);
    }

    public static void init() {
    }

    private ModItems() {
    }
}
