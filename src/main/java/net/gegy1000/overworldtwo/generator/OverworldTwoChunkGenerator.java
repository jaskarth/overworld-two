package net.gegy1000.overworldtwo.generator;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.HashCommon;
import net.gegy1000.overworldtwo.OverworldTwo;
import net.gegy1000.overworldtwo.noise.Noise;
import net.gegy1000.overworldtwo.noise.NoiseFactory;
import net.gegy1000.overworldtwo.noise.NormalizedNoise;
import net.gegy1000.overworldtwo.noise.OctaveNoise;
import net.gegy1000.overworldtwo.noise.PerlinNoise;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.NoiseConfig;
import net.minecraft.world.gen.chunk.NoiseSamplingConfig;
import net.minecraft.world.gen.chunk.SlideConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;

public class OverworldTwoChunkGenerator extends SurfaceChunkGenerator {
    public static final ChunkGeneratorType TYPE = createGeneratorType();

    public static final Codec<OverworldTwoChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                BiomeSource.field_24713.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                Codec.LONG.fieldOf("seed").stable().forGetter(generator -> generator.field_24778),
                ChunkGeneratorType.field_24781.fieldOf("settings").forGetter(generator -> generator.field_24774)
        ).apply(instance, instance.stable(OverworldTwoChunkGenerator::new));
    });

    private static final int NOISE_RES_XZ = 1;
    private static final int NOISE_RES_Y = 2;

    private static final float DEPTH_SCALE = 16.0F;
    private static final float DEPTH_OFFSET = 54.0F;

    private final Noise[] surfaceNoise;
    private final Noise tearNoise;

    private final SurfaceParameters surfaceParameters = new SurfaceParameters();
    private final ThreadLocal<BiomeCache> biomeCache;

    public OverworldTwoChunkGenerator(BiomeSource biomes, long seed, ChunkGeneratorType generatorType) {
        super(biomes, seed, generatorType);

        ChunkRandom random = new ChunkRandom(seed);

        NoiseFactory surfaceNoise = surfaceNoise();
        this.surfaceNoise = new Noise[] {
                surfaceNoise.create(random.nextLong()),
                surfaceNoise.create(random.nextLong()),
        };

        NoiseFactory tearNoise = tearNoise();
        this.tearNoise = tearNoise.create(random.nextLong());

        this.biomeCache = ThreadLocal.withInitial(() -> new BiomeCache(128, biomes));
    }

    private static NoiseFactory surfaceNoise() {
        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setFrequency(1.0 / 20.0)
                .setLacunarity(1.6)
                .setPersistence(1.0 / 1.5);

        octaves.add(PerlinNoise.create(), 6);

        return NormalizedNoise.of(octaves.build());
    }

    private static NoiseFactory tearNoise() {
        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setFrequency(1.0 / 24.0)
                .setLacunarity(1.35)
                .setPersistence(1.0 / 2.0);

        octaves.add(PerlinNoise.create(), 4);

        return NormalizedNoise.of(octaves.build());
    }

    public static void register() {
        Registry.register(Registry.CHUNK_GENERATOR, new Identifier(OverworldTwo.ID, "overworld_two"), CODEC);
    }

    // TODO: better support customization
    private static ChunkGeneratorType createGeneratorType() {
        StructuresConfig structures = new StructuresConfig(true);

        NoiseSamplingConfig noiseSampler = new NoiseSamplingConfig(1.0, 1.0, 80.0, 40.0);
        NoiseConfig noise = new NoiseConfig(
                256,
                noiseSampler,
                new SlideConfig(-10, 3, 0),
                new SlideConfig(-30, 0, 0),
                NOISE_RES_XZ, NOISE_RES_Y,
                1.0,
                -60.0 / (256.0 / 2.0),
                true,
                true,
                false,
                false
        );

        return new ChunkGeneratorType(
                structures, noise,
                Blocks.STONE.getDefaultState(),
                Blocks.WATER.getDefaultState(),
                -10, 0, 63,
                false
        );
    }

    @Override
    protected Codec<? extends ChunkGenerator> method_28506() {
        return OverworldTwoChunkGenerator.CODEC;
    }

    private static float biomeWeight(int x, int z) {
        int idx = (x + 2) + (z + 2) * 5;
        return field_24775[idx];
    }

    private SurfaceParameters sampleSurfaceParameters(int x, int z) {
        BiomeCache cache = biomeCache.get();

        float totalScale = 0.0F;
        float totalDepth = 0.0F;
        float totalWeight = 0.0F;

        float depthHere = cache.get(x, z).getDepth();

        for (int oz = -2; oz <= 2; oz++) {
            for (int ox = -2; ox <= 2; ox++) {
                Biome biome = cache.get(x + ox, z + oz);
                float depth = biome.getDepth();
                float scale = biome.getScale();

                float weight = biomeWeight(ox, oz) / (depth + 2.0F);
                if (depth > depthHere) {
                    weight *= 0.5F;
                }

                totalScale += scale * weight;
                totalDepth += depth * weight;
                totalWeight += weight;
            }
        }

        float depth = totalDepth / totalWeight;
        float scale = totalScale / totalWeight;

        this.surfaceParameters.depth = depth;
        this.surfaceParameters.scale = (scale * 0.9F) + 0.1F;

        return this.surfaceParameters;
    }

    @Override
    protected void sampleNoiseColumn(double[] buffer, int x, int z) {
        NoiseConfig noiseConfig = this.field_24774.method_28559();

        SurfaceParameters surface = this.sampleSurfaceParameters(x, z);

        float depth = (surface.depth * DEPTH_SCALE + DEPTH_OFFSET) / 256.0F;
        float scale = surface.scale / 1.9F;

        SlideConfig top = noiseConfig.getTopSlide();
        double topTarget = top.getTarget();
        double topSize = top.getSize();
        double topY = this.noiseSizeY - top.getOffset();

        SlideConfig bottom = noiseConfig.getBottomSlide();
        double bottomTarget = bottom.getTarget();
        double bottomSize = bottom.getSize();
        double bottomY = bottom.getOffset();

        double tearNoise = this.tearNoise.get(x, z) * 20.0;
        double surfaceNoise;

        if (tearNoise <= 0.0) {
            surfaceNoise = this.surfaceNoise[0].get(x, z);
        } else if (tearNoise >= 1.0) {
            surfaceNoise = this.surfaceNoise[1].get(x, z);
        } else {
            double left = this.surfaceNoise[0].get(x, z);
            double right = this.surfaceNoise[1].get(x, z);
            surfaceNoise = MathHelper.lerp(tearNoise, left, right);
        }

        // Depth factor is from [0.88; 1.12]
        double depthFactor = 1 + (surfaceNoise * 0.12);

        // map from [-1; 1] to [-0.4; 1]
        surfaceNoise = (surfaceNoise + 0.6) / 1.6;

        // [-0.4; 1] to [-0.4; 0.8]
        if (surfaceNoise > 0) {
            surfaceNoise *= 0.8;
        }

        double surfaceY = (surfaceNoise * scale) + (depth * depthFactor);

        for (int y = 0; y <= this.noiseSizeY; y++) {
            double surfaceDepth = ((double) y / this.noiseSizeY) - surfaceY;
            if (surfaceDepth < 0.0) {
                surfaceDepth *= 4.0;
            }

            double density = -surfaceDepth;

            if (topSize > 0.0) {
                double delta = (topY - y) / topSize;
                density = MathHelper.clampedLerp(topTarget, density, delta);
            }

            if (bottomSize > 0.0) {
                double delta = (y - bottomY) / bottomSize;
                density = MathHelper.clampedLerp(bottomTarget, density, delta);
            }

            buffer[y] = density;
        }
    }

    static class SurfaceParameters {
        float depth;
        float scale;
    }

    static class BiomeCache {
        private final long[] keys;
        private final Biome[] values;

        private final int mask;
        private final BiomeSource source;

        private BiomeCache(int size, BiomeSource source) {
            this.source = source;
            size = MathHelper.smallestEncompassingPowerOfTwo(size);
            this.mask = size - 1;

            this.keys = new long[size];
            Arrays.fill(this.keys, Long.MIN_VALUE);
            this.values = new Biome[size];
        }

        public Biome get(int x, int z) {
            long key = key(x, z);
            int idx = hash(key) & this.mask;

            // if the entry here has a key that matches ours, we have a cache hit
            if (this.keys[idx] == key) {
                return this.values[idx];
            }

            // cache miss: sample the source and put the result into our cache entry
            Biome sampled = source.getBiomeForNoiseGen(x, 0, z);
            this.values[idx] = sampled;
            this.keys[idx] = key;

            return sampled;
        }

        private static int hash(long key) {
            return (int) HashCommon.mix(key);
        }

        private static long key(int x, int z) {
            return ChunkPos.toLong(x, z);
        }
    }
}
