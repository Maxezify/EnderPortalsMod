package com.maxezify.enderportals.world;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.tardis.TardisStateManager;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Le monde de l'Ender : un monde entièrement souterrain, une masse de blocs
 * de l'Ender semi-transparents creusée de cavernes et de tunnels. Des
 * "reliques" — des blocs ordinaires venus mourir ici, le paradis des cubes —
 * sont prises dans la masse et se devinent au travers des blocs.
 */
public class EnderWorldChunkGenerator extends ChunkGenerator {

    public static final MapCodec<EnderWorldChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, EnderWorldChunkGenerator::new));

    /**
     * Plage de construction, identique à celle du monde normal : de −64 à 320.
     * Doit rester d'accord avec {@code min_y} et {@code height} du fichier
     * {@code dimension_type/ender_world.json} — le jeu lit le type de dimension
     * pour dimensionner les chunks, et le générateur pour les remplir.
     */
    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;

    /**
     * Épaisseur des calottes de bedrock qui ferment le monde en haut et en bas
     * — la même que celle des murs de séparation.
     */
    private static final int CAP_THICKNESS = 2;

    // Bruits fixes : le paradis des blocs est le même dans toutes les graines.
    private static final ImprovedNoise CAVERN = new ImprovedNoise(RandomSource.create(0x7A4D15L));
    private static final ImprovedNoise TUNNEL_A = new ImprovedNoise(RandomSource.create(0xE17EBEEFL));
    private static final ImprovedNoise TUNNEL_B = new ImprovedNoise(RandomSource.create(0x0DD5EEDL));
    private static final ImprovedNoise VEIN_A = new ImprovedNoise(RandomSource.create(0x1105EAL));
    private static final ImprovedNoise VEIN_B = new ImprovedNoise(RandomSource.create(0x0FEC0DEL));

    /**
     * Blocs-reliques (pondérés par répétition) : de la matière morte, jamais
     * lumineuse — la lumière est le domaine des veines, pour que les deux
     * lectures ne se brouillent pas.
     */
    private static final List<BlockState> RELICS = List.of(
            Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(),
            Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(),
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(),
            Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
            Blocks.SPRUCE_LOG.defaultBlockState(),
            Blocks.CLAY.defaultBlockState(), Blocks.CLAY.defaultBlockState(),
            Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
            Blocks.BONE_BLOCK.defaultBlockState(), Blocks.BONE_BLOCK.defaultBlockState(),
            Blocks.COAL_ORE.defaultBlockState(), Blocks.COAL_ORE.defaultBlockState(), Blocks.COAL_ORE.defaultBlockState(),
            Blocks.COPPER_ORE.defaultBlockState(), Blocks.COPPER_ORE.defaultBlockState(),
            Blocks.IRON_ORE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState(),
            Blocks.GOLD_ORE.defaultBlockState(), Blocks.GOLD_ORE.defaultBlockState(),
            Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.REDSTONE_ORE.defaultBlockState(),
            Blocks.LAPIS_ORE.defaultBlockState(),
            Blocks.DIAMOND_ORE.defaultBlockState(),
            Blocks.EMERALD_ORE.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.BOOKSHELF.defaultBlockState(),
            Blocks.PUMPKIN.defaultBlockState(),
            Blocks.MELON.defaultBlockState(),
            Blocks.SPONGE.defaultBlockState(),
            Blocks.GOLD_BLOCK.defaultBlockState());

    /**
     * Les reliques ne sont plus semées bloc par bloc mais par amas : l'espace
     * est découpé en mailles de {@value #CLUSTER_CELL} blocs de côté, une
     * maille sur {@value #CLUSTER_RARITY} porte une poche d'un seul et même
     * bloc mort. Au travers de la masse translucide, une veine de diamant
     * entrevue à dix blocs vaut mille cubes éparpillés.
     *
     * <p>Mesuré hors du jeu sur 400 chunks : 4,0 poches et 85 blocs de relique
     * par chunk, soit 21 blocs par poche. L'ancien monde en semait 112 par
     * chunk, isolés — et il était trois fois moins haut, ce qui met la nouvelle
     * densité à environ un quart de l'ancienne par unité de volume, pour
     * 4 trouvailles franches au lieu de 112 cubes perdus.</p>
     */
    private static final int CLUSTER_CELL = 16;
    private static final int CLUSTER_RARITY = 6;
    /** Rayon des poches, en blocs. Mesuré : 21 blocs de matière en moyenne. */
    private static final double CLUSTER_MIN_RADIUS = 1.2;
    private static final double CLUSTER_RADIUS_SPREAD = 1.0;

    /**
     * Filons lumineux : là où deux bruits de basse fréquence s'annulent
     * ensemble, la masse s'illumine. Le lieu géométrique de cette double
     * annulation est une courbe — d'où de longues traînées obliques,
     * perceptibles de très loin au travers du translucide.
     *
     * <p>Le seuil est le bouton de réglage : mesuré hors du jeu, 0,0001 donne
     * 0,064 % du volume, soit une soixantaine de blocs lumineux par chunk.
     * Le Bloc de l'Ender n'atténuant pas la lumière (il ne masque pas la vue),
     * chacun éclaire loin : inutile d'en mettre davantage.</p>
     */
    private static final double VEIN_BUDGET = 0.0001;

    /** Matières des filons. Une seule par filon : la teinte reste franche. */
    private static final List<BlockState> LUMINOUS = List.of(
            Blocks.GLOWSTONE.defaultBlockState(),
            Blocks.SEA_LANTERN.defaultBlockState(),
            Blocks.SHROOMLIGHT.defaultBlockState());

    // États constants, résolus une fois pour toutes. Le Bloc de l'Ender, lui,
    // ne peut pas être capturé ici : le registre n'est pas encore peuplé au
    // chargement de la classe — il est résolu une fois par appel.
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** Épaisseur du mur de bedrock qui sépare deux parcelles voisines. */
    private static final int PLOT_WALL_THICKNESS = 2;
    /**
     * Décalage du mur dans le pas des parcelles. Les portes intérieures sont
     * posées à {@code n × PLOT_SPACING + 8} ; en plaçant les murs à mi-pas, on
     * enferme chaque porte au centre de son enclos plutôt que de l'adosser
     * aussitôt à une paroi.
     */
    private static final int PLOT_WALL_OFFSET = TardisStateManager.PLOT_SPACING / 2;

    /**
     * Cette colonne fait-elle partie d'un mur de séparation ? Les parcelles
     * s'enroulent en spirale dans le plan XZ : il faut donc un quadrillage,
     * deux familles de murs perpendiculaires, pour enfermer chaque porte dans
     * sa propre case de {@code PLOT_SPACING} blocs de côté. Les murs montent
     * de la calotte de bedrock du bas à celle du haut, sans interruption :
     * impossible de passer par-dessus ni par-dessous.
     */
    private static boolean isPlotWall(int x, int z) {
        return isWallAxis(x) || isWallAxis(z);
    }

    private static boolean isWallAxis(int coordinate) {
        return Math.floorMod(coordinate - PLOT_WALL_OFFSET, TardisStateManager.PLOT_SPACING)
                < PLOT_WALL_THICKNESS;
    }

    public EnderWorldChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int bottom = chunk.getMinBuildHeight();
        int top = bottom + chunk.getHeight();
        // Un seul générateur pour tout le chunk, re-graîné bloc par bloc.
        // setSeed le re-graîne par maille, sans allouer : un RandomSource par
        // bloc plein, c'étaient ~32 000 objets par chunk.
        RandomSource random = RandomSource.create(0L);
        BlockState enderBlock = ModBlocks.ENDER_BLOCK.get().defaultBlockState();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.getMinBlockX() + dx;
                int z = chunkPos.getMinBlockZ() + dz;
                for (int y = bottom; y < top; y++) {
                    BlockState state = stateAt(x, y, z, bottom, top, random, enderBlock);
                    // Le chunk arrive déjà rempli d'air : écrire l'air des
                    // cavernes ne servait qu'à repayer le coût de
                    // setBlockState (palette, sections, heightmaps).
                    if (!state.isAir()) {
                        chunk.setBlockState(cursor.set(x, y, z), state, false);
                    }
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState stateAt(int x, int y, int z, int bottom, int top,
                                      RandomSource random, BlockState enderBlock) {
        // Le mur passe avant le creusement : aucune caverne ne doit le percer.
        if (isPlotWall(x, z) || y < bottom + CAP_THICKNESS || y >= top - CAP_THICKNESS) {
            return BEDROCK;
        }
        if (isCarved(x, y, z, bottom, top)) {
            // Une caverne qui recoupe un filon le met à nu dans sa paroi.
            return AIR;
        }
        if (isLuminousVein(x, y, z)) {
            return luminousAt(x, y, z);
        }
        BlockState relic = relicAt(x, y, z, random);
        return relic != null ? relic : enderBlock;
    }

    /**
     * La relique de la poche qui couvre ce bloc, ou {@code null}. Une seule
     * maille est interrogée : le centre est tiré à au moins 4 blocs de chaque
     * bord et le rayon plafonné à 2,2, donc aucune poche ne déborde chez la
     * voisine et un seul tirage par bloc suffit.
     */
    private static BlockState relicAt(int x, int y, int z, RandomSource random) {
        int cellX = Math.floorDiv(x, CLUSTER_CELL);
        int cellY = Math.floorDiv(y, CLUSTER_CELL);
        int cellZ = Math.floorDiv(z, CLUSTER_CELL);
        random.setSeed(Mth.getSeed(cellX, cellY, cellZ));
        if (random.nextInt(CLUSTER_RARITY) != 0) {
            return null;
        }
        // L'ordre des tirages est le même pour tous les blocs de la maille :
        // ils y lisent donc tous la même poche.
        double centerX = cellX * CLUSTER_CELL + 4 + random.nextInt(8);
        double centerY = cellY * CLUSTER_CELL + 4 + random.nextInt(8);
        double centerZ = cellZ * CLUSTER_CELL + 4 + random.nextInt(8);
        double radius = CLUSTER_MIN_RADIUS + random.nextDouble() * CLUSTER_RADIUS_SPREAD;
        BlockState relic = RELICS.get(random.nextInt(RELICS.size()));

        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius ? relic : null;
    }

    /** Ce bloc est-il sur un filon lumineux ? */
    private static boolean isLuminousVein(int x, int y, int z) {
        double a = VEIN_A.noise(x * 0.008, y * 0.010, z * 0.008);
        double b = VEIN_B.noise(x * 0.008, y * 0.010, z * 0.008);
        return a * a + b * b < VEIN_BUDGET;
    }

    /**
     * Matière du filon. Elle est tirée sur une maille grossière de 64 blocs :
     * une même traînée garde sa teinte sur toute sa longueur visible, au lieu
     * de papilloter d'un bloc à l'autre.
     */
    private static BlockState luminousAt(int x, int y, int z) {
        long seed = Mth.getSeed(x >> 6, y >> 6, z >> 6);
        return LUMINOUS.get((int) Math.floorMod(seed, (long) LUMINOUS.size()));
    }

    private static boolean isCarved(int x, int y, int z, int bottom, int top) {
        // Pénalité près du sol et du plafond pour garder des bords pleins.
        double edge = 0.0;
        if (y < bottom + 10) {
            edge += (bottom + 10 - y) * 0.06;
        }
        if (y > top - 18) {
            edge += (y - (top - 18)) * 0.05;
        }

        // Cavernes. Le seuil est passé de 0,34 à 0,50 et la fréquence a été
        // relevée : moins de poches franchissent la barre, et celles qui la
        // franchissent sont plus resserrées autour de leur sommet. Mesuré hors
        // du jeu sur le même bruit (écart-type 0,268) : 8,11 % du volume
        // creusé auparavant, 1,94 % désormais.
        double cavern = CAVERN.noise(x * 0.016, y * 0.030, z * 0.016);
        if (cavern > 0.50 + edge) {
            return true;
        }
        // Tunnels « spaghetti », resserrés dans les mêmes proportions :
        // 4,70 % du volume auparavant, 1,98 % désormais. Le budget joue sur le
        // carré du rayon, donc 0,0075 → 0,0028 les amincit d'environ 40 %.
        double a = TUNNEL_A.noise(x * 0.013, y * 0.024, z * 0.013);
        double b = TUNNEL_B.noise(x * 0.013, y * 0.024, z * 0.013);
        return a * a + b * b < Math.max(0.0, 0.0028 - edge * 0.01);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // Le creusement est intégré à fillFromNoise.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
                             ChunkAccess chunk) {
        // Pas de surface : tout est souterrain.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Pas d'apparitions naturelles.
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        // Aucune mer ici. La valeur reste 0, celle qu'elle avait quand le monde
        // commençait à 0 : la faire suivre MIN_Y l'aurait passée à -64 sans raison.
        return 0;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int bottom = level.getMinBuildHeight();
        int top = bottom + level.getHeight();
        if (isPlotWall(x, z)) {
            return top;
        }
        for (int y = top - CAP_THICKNESS - 1; y >= bottom + CAP_THICKNESS; y--) {
            if (!isCarved(x, y, z, bottom, top)) {
                return y + 1;
            }
        }
        return bottom + CAP_THICKNESS;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int bottom = level.getMinBuildHeight();
        int top = bottom + level.getHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        BlockState enderBlock = ModBlocks.ENDER_BLOCK.get().defaultBlockState();
        boolean wall = isPlotWall(x, z);
        for (int i = 0; i < states.length; i++) {
            int y = bottom + i;
            if (wall || y < bottom + CAP_THICKNESS || y >= top - CAP_THICKNESS) {
                states[i] = BEDROCK;
            } else if (isCarved(x, y, z, bottom, top)) {
                states[i] = AIR;
            } else {
                states[i] = enderBlock;
            }
        }
        return new NoiseColumn(bottom, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("EnderWorld (paradis des cubes)");
    }
}
