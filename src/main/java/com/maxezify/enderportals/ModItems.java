package com.maxezify.enderportals;

import com.maxezify.enderportals.item.EnderToolMaterial;
import com.maxezify.enderportals.item.SledgehammerItem;
import com.maxezify.enderportals.item.TardisKeyItem;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Rarity;

import java.util.List;

public final class ModItems {

    /** Cristal de l'Ender — récolté sur le minerai de l'End. */
    public static final Item ENDER_CRYSTAL = register("ender_crystal",
            new Item(new Item.Settings().rarity(Rarity.UNCOMMON)));

    /** La Masse — frappez la porte inactive en hauteur pour l'éveiller. */
    public static final Item SLEDGEHAMMER = register("sledgehammer",
            new SledgehammerItem(new Item.Settings()
                    .maxCount(1)
                    .maxDamage(250)
                    .rarity(Rarity.RARE)
                    .attributeModifiers(AttributeModifiersComponent.builder()
                            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                    new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                                            8.0, EntityAttributeModifier.Operation.ADD_VALUE),
                                    AttributeModifierSlot.MAINHAND)
                            .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                                    new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                                            -3.2, EntityAttributeModifier.Operation.ADD_VALUE),
                                    AttributeModifierSlot.MAINHAND)
                            .build())));

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

    /**
     * Outil de la pioche de l'Ender : très rapide sur les blocs de l'Ender
     * (et seule à les faire tomber), niveau diamant pour le reste.
     */
    private static ToolComponent createEnderPickaxeTool() {
        return new ToolComponent(List.of(
                ToolComponent.Rule.ofAlwaysDropping(ModTags.ENDER_PICKAXE_FAST, 45.0f),
                ToolComponent.Rule.deniesDrops(BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
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
