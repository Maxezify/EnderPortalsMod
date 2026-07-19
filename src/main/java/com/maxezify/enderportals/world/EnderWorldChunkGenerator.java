package com.maxezify.enderportals.world;

import com.maxezify.enderportals.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

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
    private static final PerlinNoiseSampler CAVERN = new PerlinNoiseSampler(Random.create(0x7A4D15L));
    private static final PerlinNoiseSampler TUNNEL_A = new PerlinNoiseSampler(Random.create(0xE17EBEEFL));
    private static final PerlinNoiseSampler TUNNEL_B = new PerlinNoiseSampler(Random.create(0x0DD5EEDL));

    /** Blocs-reliques (pondérés par répétition), lumineux compris. */
    private static final List<BlockState> RELICS = List.of(
            Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState(),
            Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState(),
            Blocks.DEEPSLATE.getDefaultState(), Blocks.DEEPSLATE.getDefaultState(), Blocks.DEEPSLATE.getDefaultState(),
            Blocks.DIRT.getDefaultState(), Blocks.DIRT.getDefaultState(), Blocks.DIRT.getDefaultState(),
            Blocks.GRAVEL.getDefaultState(), Blocks.GRAVEL.getDefaultState(),
            Blocks.SAND.getDefaultState(), Blocks.SAND.getDefaultState(),
            Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LOG.getDefaultState(),
            Blocks.SPRUCE_LOG.getDefaultState(),
            Blocks.CLAY.getDefaultState(), Blocks.CLAY.getDefaultState(),
            Blocks.MOSS_BLOCK.getDefaultState(), Blocks.MOSS_BLOCK.getDefaultState(),
            Blocks.BONE_BLOCK.getDefaultState(), Blocks.BONE_BLOCK.getDefaultState(),
            Blocks.COAL_ORE.getDefaultState(), Blocks.COAL_ORE.getDefaultState(), Blocks.COAL_ORE.getDefaultState(),
            Blocks.COPPER_ORE.getDefaultState(), Blocks.COPPER_ORE.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_ORE.getDefaultState(),
            Blocks.GOLD_ORE.getDefaultState(), Blocks.GOLD_ORE.getDefaultState(),
            Blocks.REDSTONE_ORE.getDefaultState(), Blocks.REDSTONE_ORE.getDefaultState(),
            Blocks.LAPIS_ORE.getDefaultState(),
            Blocks.DIAMOND_ORE.getDefaultState(),
            Blocks.EMERALD_ORE.getDefaultState(),
            Blocks.GLOWSTONE.getDefaultState(), Blocks.GLOWSTONE.getDefaultState(),
            Blocks.SEA_LANTERN.getDefaultState(),
            Blocks.SHROOMLIGHT.getDefaultState(),
            Blocks.AMETHYST_BLOCK.getDefaultState(),
            Blocks.OBSIDIAN.getDefaultState(),
            Blocks.CRYING_OBSIDIAN.getDefaultState(),
            Blocks.BOOKSHELF.getDefaultState(),
            Blocks.PUMPKIN.getDefaultState(),
            Blocks.MELON.getDefaultState(),
            Blocks.SPONGE.getDefaultState(),
            Blocks.GOLD_BLOCK.getDefaultState());

    /** Une relique tous les ~N blocs pleins. */
    private static final int RELIC_RARITY = 96;

    public EnderWorldChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        int bottom = chunk.getBottomY();
        int top = bottom + chunk.getHeight();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.getStartX() + dx;
                int z = chunkPos.getStartZ() + dz;
                for (int y = bottom; y < top; y++) {
                    chunk.setBlockState(cursor.set(x, y, z), stateAt(x, y, z, bottom, top), false);
                }
            }
        }
        Heightmap.populateHeightmaps(chunk, EnumSet.of(
                Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState stateAt(int x, int y, int z, int bottom, int top) {
        if (y <= bottom + 1 || y >= top - 2) {
            return Blocks.BEDROCK.getDefaultState();
        }
        if (isCarved(x, y, z, bottom, top)) {
            return Blocks.AIR.getDefaultState();
        }
        long hash = MathHelper.hashCode(x, y, z);
        Random random = Random.create(hash);
        if (random.nextInt(RELIC_RARITY) == 0) {
            return RELICS.get(random.nextInt(RELICS.size()));
        }
        return ModBlocks.ENDER_BLOCK.getDefaultState();
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
        double cavern = CAVERN.sample(x * 0.014, y * 0.026, z * 0.014);
        if (cavern > 0.34 + edge) {
            return true;
        }
        // Tunnels "spaghetti".
        double a = TUNNEL_A.sample(x * 0.011, y * 0.021, z * 0.011);
        double b = TUNNEL_B.sample(x * 0.011, y * 0.021, z * 0.011);
        return a * a + b * b < Math.max(0.0, 0.0075 - edge * 0.01);
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // Le creusement est intégré à populateNoise.
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Pas de surface : tout est souterrain.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Pas d'apparitions naturelles.
    }

    @Override
    public int getWorldHeight() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return MIN_Y;
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        int top = bottom + world.getHeight();
        for (int y = top - 3; y > bottom + 1; y--) {
            if (!isCarved(x, y, z, bottom, top)) {
                return y + 1;
            }
        }
        return bottom + 2;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        int top = bottom + world.getHeight();
        BlockState[] states = new BlockState[world.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = bottom + i;
            if (y <= bottom + 1 || y >= top - 2) {
                states[i] = Blocks.BEDROCK.getDefaultState();
            } else if (isCarved(x, y, z, bottom, top)) {
                states[i] = Blocks.AIR.getDefaultState();
            } else {
                states[i] = ModBlocks.ENDER_BLOCK.getDefaultState();
            }
        }
        return new VerticalBlockSample(bottom, states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("EnderWorld (paradis des cubes)");
    }
}
