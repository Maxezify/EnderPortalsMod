package com.maxezify.enderportals;

import com.maxezify.enderportals.world.EnderWorldChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registres divers : le codec du générateur de chunks du monde de l'Ender et
 * l'onglet créatif du mod.
 */
public final class ModRegistries {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, EnderPortalsMod.MODID);

    public static final Supplier<MapCodec<EnderWorldChunkGenerator>> ENDER_WORLD =
            CHUNK_GENERATORS.register("ender_world", () -> EnderWorldChunkGenerator.CODEC);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnderPortalsMod.MODID);

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enderportals.main"))
                    .icon(() -> new ItemStack(ModItems.TARDIS_KEY.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ENDER_CRYSTAL.get());
                        output.accept(ModItems.TARDIS_KEY.get());
                        output.accept(ModItems.ENDER_PICKAXE.get());
                        output.accept(ModItems.INACTIVE_TARDIS_DOOR.get());
                        output.accept(ModItems.ENDER_ORE.get());
                        output.accept(ModItems.ENDER_BLOCK.get());
                        output.accept(ModItems.ENDER_BRICKS.get());
                        output.accept(ModItems.CHISELED_ENDER_BRICKS.get());
                        output.accept(ModItems.ENDER_BRICK_STAIRS.get());
                        output.accept(ModItems.ENDER_BRICK_SLAB.get());
                        output.accept(ModItems.ENDER_BRICK_WALL.get());
                        output.accept(ModItems.CENTRALIZER.get());
                        output.accept(ModItems.ALLY_PASSAGE.get());
                        output.accept(ModItems.FRIENDSHIP_CONSOLE.get());
                        output.accept(ModItems.ENTITY_TELEPORTER.get());
                        output.accept(ModItems.ENTITY_LANDER.get());
                        output.accept(ModItems.ENDER_BAG.get());
                    })
                    .build());

    private ModRegistries() {
    }
}
