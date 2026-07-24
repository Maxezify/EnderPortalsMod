package com.maxezify.enderportals.world;

import com.maxezify.enderportals.ModBlocks;
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

    private static final int MIN_Y = 0;
    private static final int HEIGHT = 128;

    // Bruits fixes : le paradis des blocs est le même dans toutes les graines.
    private static final ImprovedNoise CAVERN = new ImprovedNoise(RandomSource.create(0x7A4D15L));
    private static final ImprovedNoise TUNNEL_A = new ImprovedNoise(RandomSource.create(0xE17EBEEFL));
    private static final ImprovedNoise TUNNEL_B = new ImprovedNoise(RandomSource.create(0x0DD5EEDL));

    /** Blocs-reliques (pondérés par répétition), lumineux compris. */
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
            Blocks.GLOWSTONE.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState(),
            Blocks.SEA_LANTERN.defaultBlockState(),
            Blocks.SHROOMLIGHT.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.BOOKSHELF.defaultBlockState(),
            Blocks.PUMPKIN.defaultBlockState(),
            Blocks.MELON.defaultBlockState(),
            Blocks.SPONGE.defaultBlockState(),
            Blocks.GOLD_BLOCK.defaultBlockState());

    /** Une relique tous les ~N blocs pleins. */
    private static final int RELIC_RARITY = 256;

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
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.getMinBlockX() + dx;
                int z = chunkPos.getMinBlockZ() + dz;
                for (int y = bottom; y < top; y++) {
                    chunk.setBlockState(cursor.set(x, y, z), stateAt(x, y, z, bottom, top), false);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState stateAt(int x, int y, int z, int bottom, int top) {
        if (y <= bottom + 1 || y >= top - 2) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (isCarved(x, y, z, bottom, top)) {
            return Blocks.AIR.defaultBlockState();
        }
        long hash = Mth.getSeed(x, y, z);
        RandomSource random = RandomSource.create(hash);
        if (random.nextInt(RELIC_RARITY) == 0) {
            return RELICS.get(random.nextInt(RELICS.size()));
        }
        return ModBlocks.ENDER_BLOCK.get().defaultBlockState();
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

        // Grandes cavernes.
        double cavern = CAVERN.noise(x * 0.014, y * 0.026, z * 0.014);
        if (cavern > 0.34 + edge) {
            return true;
        }
        // Tunnels "spaghetti".
        double a = TUNNEL_A.noise(x * 0.011, y * 0.021, z * 0.011);
        double b = TUNNEL_B.noise(x * 0.011, y * 0.021, z * 0.011);
        return a * a + b * b < Math.max(0.0, 0.0075 - edge * 0.01);
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
        return MIN_Y;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int bottom = level.getMinBuildHeight();
        int top = bottom + level.getHeight();
        for (int y = top - 3; y > bottom + 1; y--) {
            if (!isCarved(x, y, z, bottom, top)) {
                return y + 1;
            }
        }
        return bottom + 2;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int bottom = level.getMinBuildHeight();
        int top = bottom + level.getHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = bottom + i;
            if (y <= bottom + 1 || y >= top - 2) {
                states[i] = Blocks.BEDROCK.defaultBlockState();
            } else if (isCarved(x, y, z, bottom, top)) {
                states[i] = Blocks.AIR.defaultBlockState();
            } else {
                states[i] = ModBlocks.ENDER_BLOCK.get().defaultBlockState();
            }
        }
        return new NoiseColumn(bottom, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("EnderWorld (paradis des cubes)");
    }
}
