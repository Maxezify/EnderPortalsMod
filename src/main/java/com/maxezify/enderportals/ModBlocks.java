package com.maxezify.enderportals;

import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.block.CentralizerBlock;
import com.maxezify.enderportals.block.EnderBlock;
import com.maxezify.enderportals.block.FriendshipConsoleBlock;
import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.block.TardisDoorBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
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
    /**
     * La voix du Bloc de l'Ender : le fond mou d'un bloc de miel, et par-dessus
     * le carillon d'un cadre de portail de l'End (voir
     * {@link EnderBlock#playerWillDestroy}).
     *
     * <p>Le miel seul sonnait trop organique pour de la matière translucide, et
     * l'améthyste d'origine trop nette — un cristal qu'on brise, là où cette
     * masse cède plutôt qu'elle ne casse. Le grave à 0,85 lui donne du poids ;
     * le carillon, par-dessus, dit d'où elle vient.</p>
     */
    private static final SoundType ENDER_BLOCK_SOUND = new SoundType(1.0f, 0.85f,
            SoundEvents.HONEY_BLOCK_BREAK, SoundEvents.HONEY_BLOCK_STEP,
            SoundEvents.HONEY_BLOCK_PLACE, SoundEvents.HONEY_BLOCK_HIT,
            SoundEvents.HONEY_BLOCK_FALL);

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
                    .sound(ENDER_BLOCK_SOUND)));

    /**
     * Briques de l'Ender — taillées dans le Bloc de l'Ender, et translucides
     * au même titre que lui : un mur de briques laisse deviner ce qu'il y a
     * derrière exactement comme la masse dans laquelle on le bâtit.
     *
     * <p>{@link TransparentBlock} n'est pas là pour la transparence — c'est la
     * texture et la couche de rendu du modèle qui s'en chargent — mais pour son
     * {@code skipRendering} : les faces entre deux briques voisines ne sont pas
     * dessinées, sans quoi un mur épais empilerait ses vitres et s'assombrirait
     * couche après couche.</p>
     */
    public static final DeferredBlock<TransparentBlock> ENDER_BRICKS = BLOCKS.register("ender_bricks",
            () -> new TransparentBlock(brickProperties()));

    /** Briques de l'Ender ciselées — l'œil des cadres de portail, gravé. */
    public static final DeferredBlock<TransparentBlock> CHISELED_ENDER_BRICKS = BLOCKS.register(
            "chiseled_ender_bricks",
            () -> new TransparentBlock(brickProperties()));

    public static final DeferredBlock<StairBlock> ENDER_BRICK_STAIRS = BLOCKS.register("ender_brick_stairs",
            () -> new StairBlock(ENDER_BRICKS.get().defaultBlockState(), brickProperties()));

    public static final DeferredBlock<SlabBlock> ENDER_BRICK_SLAB = BLOCKS.register("ender_brick_slab",
            () -> new SlabBlock(brickProperties()));

    public static final DeferredBlock<WallBlock> ENDER_BRICK_WALL = BLOCKS.register("ender_brick_wall",
            () -> new WallBlock(brickProperties()));

    /**
     * Propriétés communes à toute la famille des briques. Une instance neuve à
     * chaque appel : un {@code Properties} est un constructeur mutable, le
     * partager entre deux blocs les ferait se marcher dessus.
     */
    private static BlockBehaviour.Properties brickProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(3.5f, 8.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .sound(SoundType.DEEPSLATE_BRICKS);
    }

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

    /**
     * Passage des Alliés — l'arche claire qui relie deux bases. Indestructible
     * par explosion mais récupérable à la pioche : on doit pouvoir déplacer son
     * passage sans perdre la matière qu'il a coûtée.
     */
    public static final DeferredBlock<AllyPassageBlock> ALLY_PASSAGE = BLOCKS.register("ally_passage",
            () -> new AllyPassageBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(4.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> switch (state.getValue(AllyPassageBlock.PHASE)) {
                        case CLOSED -> 4;
                        case OPENING -> 9;
                        case OPEN, THROUGH -> 12;
                    })
                    .isValidSpawn((state, level, pos, type) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.AMETHYST)));

    /** Contrôle de l'amitié — le pavé numérique qui commande le Passage. */
    public static final DeferredBlock<FriendshipConsoleBlock> FRIENDSHIP_CONSOLE = BLOCKS.register(
            "friendship_console",
            () -> new FriendshipConsoleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(3.5f, 9.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
