package com.maxezify.enderportals;

import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.world.EnderWorldChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderPortalsMod implements ModInitializer {
    public static final String MOD_ID = "enderportals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Distance de chute (en blocs) que le joueur doit accumuler avant de
     * frapper la porte inactive à la masse pour l'initialiser — la mécanique
     * de l'attaque écrasante de la Mace, appliquée à la porte.
     */
    public static final float ACTIVATION_FALL_DISTANCE = 20.0f;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModComponents.init();
        ModRecipes.init();

        Registry.register(Registries.CHUNK_GENERATOR, id("ender_world"), EnderWorldChunkGenerator.CODEC);

        registerItemGroup();
        registerEndOreGeneration();
        registerMaceRitual();

        LOGGER.info("Ender Portals initialisé — le vortex vous attend.");
    }

    private static void registerItemGroup() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.TARDIS_KEY))
                .displayName(Text.translatable("itemGroup.enderportals.main"))
                .entries((context, entries) -> {
                    entries.add(ModItems.ENDER_CRYSTAL);
                    entries.add(ModItems.TARDIS_KEY);
                    entries.add(ModItems.ENDER_PICKAXE);
                    entries.add(ModItems.INACTIVE_TARDIS_DOOR);
                    entries.add(ModItems.ENDER_ORE);
                    entries.add(ModItems.ENDER_BLOCK);
                    entries.add(ModItems.ENDER_BRICKS);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, id("main"), group);
    }

    private static void registerEndOreGeneration() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInTheEnd(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, id("ender_ore")));
    }

    /**
     * L'attaque écrasante de la Mace vanilla, appliquée à la porte inactive :
     * frappée en pleine chute, elle s'éveille.
     */
    private static void registerMaceRitual() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.getBlockState(pos).isOf(ModBlocks.INACTIVE_TARDIS_DOOR)
                    && player.getStackInHand(hand).isOf(Items.MACE)) {
                if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                    InactiveTardisDoorBlock.tryActivate((ServerWorld) world, pos, serverPlayer,
                            player.getStackInHand(hand));
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
