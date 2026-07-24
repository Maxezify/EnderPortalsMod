package com.maxezify.enderportals;

import com.maxezify.enderportals.block.CentralizerBlock;
import com.maxezify.enderportals.block.EnderBlock;
import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.block.TardisDoorBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnderPortalsMod.MODID);

    /** Minerai de l'Ender — se génère dans la pierre de l'End. */
    public static final DeferredBlock<DropExperienceBlock> ENDER_ORE = BLOCKS.register("ender_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(4.5f, 9.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    /**
     * Bloc de l'Ender — gris semi-transparent. On voit au travers, entrevoyant
     * les blocs-reliques. Seule la pioche de l'Ender le récolte.
     */
    public static final DeferredBlock<EnderBlock> ENDER_BLOCK = BLOCKS.register("ender_block",
            () -> new EnderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(12.0f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .sound(SoundType.AMETHYST)));

    /** Briques de l'Ender — variante opaque, pratique pour construire sa base. */
    public static final DeferredBlock<Block> ENDER_BRICKS = BLOCKS.register("ender_bricks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f, 8.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE_BRICKS)));

    /**
     * Porte de l'Ender inactive — caisson dormant de deux blocs, posable et
     * cassable à la pioche. Un coup de Mace en pleine chute l'éveille.
     */
    public static final DeferredBlock<InactiveTardisDoorBlock> INACTIVE_TARDIS_DOOR = BLOCKS.register(
            "inactive_tardis_door",
            () -> new InactiveTardisDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(5.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.DEEPSLATE_BRICKS)));

    /**
     * Porte de l'Ender active — indestructible, matérialisée/dématérialisée
     * via la clé. Rendu assuré par un BlockEntityRenderer (fondu).
     */
    public static final DeferredBlock<TardisDoorBlock> TARDIS_DOOR = BLOCKS.register("tardis_door",
            () -> new TardisDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0f, 3600000.0f)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(TardisDoorBlock.OPEN) ? 7 : 0)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.METAL)));

    /**
     * Centraliseur d'objet — machine à voyants, posable uniquement dans le
     * monde de l'Ender. Point de collecte du Sac de l'Ender.
     */
    public static final DeferredBlock<CentralizerBlock> CENTRALIZER = BLOCKS.register("centralizer",
            () -> new CentralizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.0f, 9.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 8)
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
