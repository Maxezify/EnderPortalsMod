package com.maxezify.enderportals;

import com.maxezify.enderportals.block.EnderBlock;
import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.block.TardisDoorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public final class ModBlocks {

    /** Minerai de l'Ender — se génère dans la pierre de l'End. */
    public static final Block ENDER_ORE = register("ender_ore", new ExperienceDroppingBlock(
            UniformIntProvider.create(3, 7),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.PALE_YELLOW)
                    .strength(4.5f, 9.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)));

    /**
     * Bloc de l'Ender — bloc gris semi-transparent qui compose le monde de
     * l'Ender. On voit au travers, ce qui laisse entrevoir les blocs "au
     * paradis" pris dans la masse. Seule la pioche de l'Ender le récolte.
     */
    public static final Block ENDER_BLOCK = register("ender_block", new EnderBlock(
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.GRAY)
                    .strength(12.0f, 8.0f)
                    .requiresTool()
                    .nonOpaque()
                    .luminance(state -> 3)
                    .allowsSpawning((state, world, pos, type) -> false)
                    .solidBlock((state, world, pos) -> false)
                    .blockVision((state, world, pos) -> false)
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    /** Briques de l'Ender — variante opaque, pratique pour construire sa base. */
    public static final Block ENDER_BRICKS = register("ender_bricks", new Block(
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.GRAY)
                    .strength(3.5f, 8.0f)
                    .requiresTool()
                    .sounds(BlockSoundGroup.DEEPSLATE_BRICKS)));

    /**
     * Porte de l'Ender inactive — caisson dormant de deux blocs, posable et
     * cassable à la pioche. Un coup de Mace en pleine chute l'éveille.
     */
    public static final Block INACTIVE_TARDIS_DOOR = register("inactive_tardis_door",
            new InactiveTardisDoorBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLUE)
                    .strength(5.0f, 1200.0f)
                    .requiresTool()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .sounds(BlockSoundGroup.DEEPSLATE_BRICKS)));

    /**
     * Porte du TARDIS active — indestructible, elle se matérialise et se
     * dématérialise via la clé. Rendu assuré par un BlockEntityRenderer
     * (fondu de matérialisation).
     */
    public static final Block TARDIS_DOOR = register("tardis_door",
            new TardisDoorBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLUE)
                    .strength(-1.0f, 3600000.0f)
                    .dropsNothing()
                    .nonOpaque()
                    .luminance(state -> state.get(TardisDoorBlock.OPEN) ? 7 : 0)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .sounds(BlockSoundGroup.METAL)));

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, EnderPortalsMod.id(name), block);
    }

    public static void init() {
        // Le chargement de la classe déclenche les enregistrements.
    }

    private ModBlocks() {
    }
}
