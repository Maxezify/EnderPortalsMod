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
 * Le monde de l'Ender : une masse pleine et continue de blocs de l'Ender
 * semi-transparents, sans la moindre cavité. Des "reliques" — des blocs
 * ordinaires venus mourir ici, le paradis des cubes — y sont prises en nuées,
 * et de rares filons lumineux la traversent ; les unes comme les autres se
 * devinent au travers de la matière translucide.
 *
 * <p>Le creusement a été retiré : cavernes et tunnels ouvraient des vides qui
 * cassaient la lecture de la masse et, sous shader, dissipaient le brouillard
 * là où il faisait tout l'intérêt du lieu. On ne s'y déplace donc qu'à la
 * Pioche de l'Ender, en taillant sa propre galerie.</p>
 */
public class EnderWorldChunkGenerator extends ChunkGenerator {

    public static final MapCodec<EnderWorldChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, EnderWorldChunkGenerator::new));

    /**
     * Plage de construction, identique à celle du Nether : de 0 à 128.
     *
     * <p>C'est l'altitude qui commande le brouillard des shaders. Complementary
     * Reimagined fait décroître son brouillard atmosphérique au-dessus de 55,1
     * et l'éteint à 85,1 ; son brouillard de caverne meurt à 61,9. Un monde de
     * 384 blocs de haut plaçait l'essentiel du volume hors de ces bandes.
     * Ramené aux 128 du Nether, le monde de l'Ender tient tout entier dans la
     * plage où ces effets existent.</p>
     *
     * <p>Doit rester d'accord avec {@code min_y} et {@code height} du fichier
     * {@code dimension_type/ender_world.json} — le jeu lit le type de dimension
     * pour dimensionner les chunks, et le générateur pour les remplir.</p>
     */
    private static final int MIN_Y = 0;
    private static final int HEIGHT = 128;

    /**
     * Épaisseur des calottes de bedrock qui ferment le monde en haut et en bas
     * — la même que celle des murs de séparation.
     */
    private static final int CAP_THICKNESS = 2;

    // Bruits fixes : le paradis des blocs est le même dans toutes les graines.
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
     * <p>Une poche n'est pas une boule pleine : à l'intérieur de son rayon,
     * chaque bloc est tiré au sort avec une probabilité qui décroît du centre
     * vers le bord, et plafonnée à {@value #CLUSTER_DENSITY}. Le cœur lui-même
     * n'est donc jamais compact, et les bords se dissolvent dans la masse au
     * lieu de s'arrêter net sur une surface de sphère.</p>
     *
     * <p>Mesuré hors du jeu sur 144 chunks : 6,3 poches et 82 blocs de relique
     * par chunk, soit 13 blocs dispersés dans une sphère de rayon 3,5 —
     * 7,2 % de remplissage. Autant de matière qu'auparavant, mais deux fois
     * plus diluée : des nuées plus nombreuses, plus petites et plus ténues.
     * L'ancien monde semait 112 blocs isolés par chunk sur une hauteur trois
     * fois moindre — la densité par volume tombe au quart.</p>
     */
    private static final int CLUSTER_CELL = 16;
    private static final int CLUSTER_RARITY = 4;
    /** Rayon des poches, en blocs. Le maximum tient dans la maille (voir relicAt). */
    private static final double CLUSTER_MIN_RADIUS = 3.0;
    private static final double CLUSTER_RADIUS_SPREAD = 1.0;
    /** Remplissage au cœur d'une poche. En dessous de 1, rien n'est jamais collé. */
    private static final double CLUSTER_DENSITY = 0.28;

    /**
     * Filons lumineux : là où deux bruits de basse fréquence s'annulent
     * ensemble, la masse s'illumine. Le lieu géométrique de cette double
     * annulation est une courbe — d'où de longues traînées obliques,
     * perceptibles de très loin au travers du translucide.
     *
     * <p>Le filon n'est pas un fil plein : dans son enveloppe, chaque bloc est
     * tiré au sort avec une probabilité qui décroît du cœur vers le bord et
     * plafonne à {@value #VEIN_DENSITY}. La traînée se lit comme un semis de
     * lueurs le long d'une courbe, pas comme un câble.
     *
     * <p>Mesuré hors du jeu : l'enveloppe à 0,0016 est seize fois plus
     * volumineuse qu'un fil plein à 0,0001, et la dispersion ramène le compte à
     * environ 62 blocs lumineux par chunk — le même compte depuis la 0.8.0,
     * pour un remplissage tombé à 5 % de l'enveloppe. Le Bloc de l'Ender
     * n'atténuant pas la lumière (il ne masque pas la vue), chacun éclaire
     * loin : inutile d'en mettre davantage.</p>
     */
    private static final double VEIN_BUDGET = 0.0016;
    private static final double VEIN_DENSITY = 0.10;
    /** Sel du tirage des filons : sans lui, ils partageraient leur hasard avec les poches. */
    private static final long VEIN_SEED_SALT = 17L;

    /**
     * Matières des filons. Une seule par filon : la teinte reste franche.
     *
     * <p>La glowstone et le shroomlight — jaune chaud et orange — ont laissé la
     * place au froglight perlescent, d'un violet pâle. C'est une palette
     * d'Ender et non de Nether, et surtout Complementary Reimagined connaît ce
     * bloc nommément dans son {@code block.properties} : il lui applique sa
     * propre couleur d'éclairage. Avec l'option de lumière colorée du shader,
     * la brume autour d'un filon se teinte donc de violet sur des dizaines de
     * blocs. Le froglight est doublé pour dominer la sea lantern.</p>
     */
    private static final List<BlockState> LUMINOUS = List.of(
            Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(),
            Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(),
            Blocks.SEA_LANTERN.defaultBlockState());

    // États constants, résolus une fois pour toutes. Le Bloc de l'Ender, lui,
    // ne peut pas être capturé ici : le registre n'est pas encore peuplé au
    // chargement de la classe — il est résolu une fois par appel.
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();

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
                    chunk.setBlockState(cursor.set(x, y, z),
                            stateAt(x, y, z, bottom, top, random, enderBlock), false);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState stateAt(int x, int y, int z, int bottom, int top,
                                      RandomSource random, BlockState enderBlock) {
        if (isPlotWall(x, z) || y < bottom + CAP_THICKNESS || y >= top - CAP_THICKNESS) {
            return BEDROCK;
        }
        BlockState luminous = luminousAt(x, y, z, random);
        if (luminous != null) {
            return luminous;
        }
        BlockState relic = relicAt(x, y, z, random);
        return relic != null ? relic : enderBlock;
    }

    /**
     * La relique de la poche qui couvre ce bloc, ou {@code null}. Une seule
     * maille est interrogée : le centre est tiré entre 4 et 11 blocs du coin de
     * la maille et le rayon plafonné à 4, donc une poche atteint au plus les
     * bords de sa maille sans jamais empiéter sur la voisine — un seul tirage
     * par bloc suffit.
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
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > radius * radius) {
            return null;
        }
        // Densité décroissante du centre au bord : la poche s'effiloche au lieu
        // de s'arrêter sur une surface de sphère.
        double density = (1.0 - Math.sqrt(distanceSq) / radius) * CLUSTER_DENSITY;
        random.setSeed(Mth.getSeed(x, y, z));
        return random.nextFloat() < density ? relic : null;
    }

    /**
     * La matière lumineuse de ce bloc s'il tombe sur un filon, ou {@code null}.
     *
     * <p>La teinte est tirée sur une maille grossière de 64 blocs : une même
     * traînée la garde sur toute sa longueur visible, au lieu de papilloter
     * d'un bloc à l'autre.</p>
     */
    private static BlockState luminousAt(int x, int y, int z, RandomSource random) {
        double a = VEIN_A.noise(x * 0.008, y * 0.010, z * 0.008);
        double b = VEIN_B.noise(x * 0.008, y * 0.010, z * 0.008);
        double distanceSq = a * a + b * b;
        if (distanceSq >= VEIN_BUDGET) {
            return null;
        }
        double density = (1.0 - distanceSq / VEIN_BUDGET) * VEIN_DENSITY;
        random.setSeed(Mth.getSeed(x, y, z) * 31L + VEIN_SEED_SALT);
        if (random.nextFloat() >= density) {
            return null;
        }
        long tint = Mth.getSeed(x >> 6, y >> 6, z >> 6);
        return LUMINOUS.get((int) Math.floorMod(tint, (long) LUMINOUS.size()));
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // Aucun creusement : la masse est pleine d'un bout à l'autre.
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
        // La masse est pleine partout : la première surface libre est le
        // dessous de la calotte de bedrock, et le mur monte jusqu'en haut.
        return level.getMinBuildHeight() + level.getHeight() - (isPlotWall(x, z) ? 0 : CAP_THICKNESS);
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
            states[i] = wall || y < bottom + CAP_THICKNESS || y >= top - CAP_THICKNESS
                    ? BEDROCK : enderBlock;
        }
        return new NoiseColumn(bottom, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("EnderWorld (paradis des cubes)");
    }
}
