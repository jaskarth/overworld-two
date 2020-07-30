package net.gegy1000.overworldtwo.generator;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
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
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;

public class OverworldTwoChunkGenerator extends SurfaceChunkGenerator {
    public static final ChunkGeneratorType OVERWORLD = createOverworld();
    public static final ChunkGeneratorType NETHER = createNether();

    public static final Codec<OverworldTwoChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.field_24713.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
            Codec.LONG.fieldOf("seed").stable().forGetter(generator -> generator.field_24778),
            ChunkGeneratorType.field_24781.fieldOf("settings").forGetter(generator -> generator.field_24774)
    ).apply(instance, instance.stable(OverworldTwoChunkGenerator::new)));

    private static final int NOISE_RES_XZ = 1;
    private static final int NOISE_RES_Y = 2;

    private final Noise[] surfaceNoise;
    private final Noise tearNoise;
    private final ThreadLocal<BiomeCache> biomeCache;
    private final Noise extraDensityNoise;

    public OverworldTwoChunkGenerator(BiomeSource biomes, long seed, ChunkGeneratorType generatorType) {
        super(biomes, seed, generatorType);

        ChunkRandom random = new ChunkRandom(seed);

        NoiseFactory surfaceNoise = surfaceNoise(generatorType);
        this.surfaceNoise = new Noise[] {
                surfaceNoise.create(random.nextLong()),
                surfaceNoise.create(random.nextLong()),
        };

        this.extraDensityNoise = extraDensityNoise().create(random.nextLong());

        NoiseFactory tearNoise = tearNoise(generatorType);
        this.tearNoise = tearNoise.create(random.nextLong());

        this.biomeCache = ThreadLocal.withInitial(() -> new BiomeCache(128, biomes));
    }

    private static NoiseFactory surfaceNoise(ChunkGeneratorType type) {
        NoiseSamplingConfig config = type.method_28559().getSampling();

        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / config.getXZScale())
                .setVerticalFrequency(1.0 / config.getYScale())
                .setLacunarity(1.7)
                .setPersistence(1.0 / 1.8);

        octaves.add(PerlinNoise.create(), 6);

        return NormalizedNoise.of(octaves.build());
    }

    private static NoiseFactory tearNoise(ChunkGeneratorType type) {
        NoiseSamplingConfig config = type.method_28559().getSampling();

        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / config.getXZFactor())
                .setVerticalFrequency(1.0 / config.getYFactor())
                .setLacunarity(1.35)
                .setPersistence(1.0 / 2.0);

        octaves.add(PerlinNoise.create(), 4);

        return NormalizedNoise.of(octaves.build());
    }

    private static NoiseFactory extraDensityNoise() {
        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / 150.0)
                .setVerticalFrequency(1.0 / 150.0)
                .setLacunarity(1.4)
                .setPersistence(1.0 / 1.4);

        octaves.add(PerlinNoise.create(), 4);

        return NormalizedNoise.of(octaves.build());
    }

    public static void register() {
        Registry.register(Registry.CHUNK_GENERATOR, new Identifier(OverworldTwo.ID, "overworld_two"), CODEC);
    }

    // TODO: better support customization
    private static ChunkGeneratorType createOverworld() {
        StructuresConfig structures = new StructuresConfig(true);

        // Vanilla: 1.0, 1.0, 40.0, 22.0
        NoiseSamplingConfig noiseSampler = new NoiseSamplingConfig(24.0, 24.0, 40.0, 24.0);
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

    private static ChunkGeneratorType createNether() {
        StructuresConfig structures = new StructuresConfig(false);
        Map<StructureFeature<?>, StructureConfig> map = Maps.newHashMap(StructuresConfig.DEFAULT_STRUCTURES);
        map.put(StructureFeature.RUINED_PORTAL, new StructureConfig(25, 10, 34222645));

        // Vanilla: 1.0, 3.0, 80.0, 60.0
        NoiseSamplingConfig noiseSampler = new NoiseSamplingConfig(48.0, 18.0, 120.0, 40.0);
        NoiseConfig noise = new NoiseConfig(
                128,
                noiseSampler,
                new SlideConfig(120, 3, 0),
                new SlideConfig(320, 4, -1),
                1,
                2,
                0.0,
                0.019921875,
                false,
                false,
                false,
                false
        );

        return new ChunkGeneratorType(
                new StructuresConfig(Optional.ofNullable(structures.getStronghold()), map),
                noise,
                Blocks.NETHERRACK.getDefaultState(),
                Blocks.LAVA.getDefaultState(),
                0, 0, 32,
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

        return new SurfaceParameters((depth * 0.5F) - 0.125F, (scale * 0.9F) + 0.1F);
    }

    @Override
    protected void sampleNoiseColumn(double[] buffer, int x, int z) {
        NoiseConfig noiseConfig = this.field_24774.method_28559();
        SurfaceParameters params = sampleSurfaceParameters(x, z);
        double scaledDepth = params.depth * 0.265625D;
        double scaledScale = 96.0D / params.scale;

        double topTarget = noiseConfig.getTopSlide().getTarget();
        double topSize = noiseConfig.getTopSlide().getSize();
        double topOffset = noiseConfig.getTopSlide().getOffset();
        double bottomTarget = noiseConfig.getBottomSlide().getTarget();
        double bottomSize = noiseConfig.getBottomSlide().getSize();
        double bottomOffset = noiseConfig.getBottomSlide().getOffset();
        double randomDensityOffset = noiseConfig.hasRandomDensityOffset() ? this.extraDensityNoiseAt(x, z) : 0.0D;
        double densityFactor = noiseConfig.getDensityFactor();
        double densityOffset = noiseConfig.getDensityOffset();

        for(int y = 0; y <= this.noiseSizeY; ++y) {
            double noise = this.getNoiseAt(x, y, z);
            double yOffset = 1.0D - (double) y * 2.0D / (double)this.noiseSizeY + randomDensityOffset;
            double density = yOffset * densityFactor + densityOffset;
            double falloff = (density + scaledDepth) * scaledScale;
            if (falloff > 0.0D) {
                noise += falloff * 4.0D;
            } else {
                noise += falloff;
            }
            double slide;
            if (topSize > 0.0D) {
                slide = ((double)(this.noiseSizeY - y) - topOffset) / topSize;
                noise = MathHelper.clampedLerp(topTarget, noise, slide);
            }

            if (bottomSize > 0.0D) {
                slide = ((double) y - bottomOffset) / bottomSize;
                noise = MathHelper.clampedLerp(bottomTarget, noise, slide);
            }

            buffer[y] = noise;
        }
    }

    private double getNoiseAt(int x, int y, int z) {
        double tearNoise = this.tearNoise.get(x, y, z) * 15.0;
        double surfaceNoise;

        if (tearNoise <= 0.0) {
            surfaceNoise = this.surfaceNoise[0].get(x, y, z);
        } else if (tearNoise >= 1.0) {
            surfaceNoise = this.surfaceNoise[1].get(x, y, z);
        } else {
            double left = this.surfaceNoise[0].get(x, y, z);
            double right = this.surfaceNoise[1].get(x, y, z);
            surfaceNoise = MathHelper.lerp(tearNoise, left, right);
        }

        return surfaceNoise * 200;
    }

    protected double extraDensityNoiseAt(int x, int z) {
        double rawDensity = this.extraDensityNoise.get(x, 10.0D, z);
        double scaledDensity;
        if (rawDensity < 0.0D) {
            scaledDensity = -rawDensity * 0.3D;
        } else {
            scaledDensity = rawDensity;
        }

        double finalDensity = scaledDensity * 24.575625D - 2.0D;
        return finalDensity < 0.0D ? finalDensity * 0.009486607142857142D : Math.min(finalDensity, 1.0D) * 0.006640625D;
    }

    private static class SurfaceParameters {
        private final float depth;
        private final float scale;

        public SurfaceParameters(float depth, float scale) {
            this.depth = depth;
            this.scale = scale;
        }
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
