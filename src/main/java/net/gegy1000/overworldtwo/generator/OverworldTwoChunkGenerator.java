package net.gegy1000.overworldtwo.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gegy1000.overworldtwo.noise.Noise;
import net.gegy1000.overworldtwo.noise.NoiseFactory;
import net.gegy1000.overworldtwo.noise.NormalizedNoise;
import net.gegy1000.overworldtwo.noise.OctaveNoise;
import net.gegy1000.overworldtwo.noise.PerlinNoise;
import net.minecraft.block.Blocks;
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
    private static final float DEPTH_OFFSET = 62.0F;

    private final Noise[] surfaceNoise;
    private final Noise tearNoise;

    private final SurfaceParameters surfaceParameters = new SurfaceParameters();

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
    }

    private static NoiseFactory surfaceNoise() {
        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setFrequency(1.0 / 30.0)
                .setLacunarity(1.8)
                .setPersistence(1.0 / 1.9);

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
        Registry.register(Registry.CHUNK_GENERATOR, "overworld_two", CODEC);
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
        float totalScale = 0.0F;
        float totalDepth = 0.0F;
        float totalWeight = 0.0F;

        int seaLevel = this.getSeaLevel();
        float depthHere = this.biomeSource.getBiomeForNoiseGen(x, seaLevel, z).getDepth();

        for (int oz = -2; oz <= 2; oz++) {
            for (int ox = -2; ox <= 2; ox++) {
                Biome biome = this.biomeSource.getBiomeForNoiseGen(x + ox, seaLevel, z + oz);
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
        float scale = surface.scale / 2.0F;

        NoiseSamplingConfig sampling = noiseConfig.getSampling();
        double xzScale = 684.412 * sampling.getXZScale();
        double yScale = 684.412 * sampling.getYScale();
        double xzFactor = xzScale / sampling.getXZFactor();
        double yFactor = yScale / sampling.getYFactor();

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

        // map from [-1; 1] to [-0.25; 1]
        surfaceNoise = (surfaceNoise + 0.75) / 1.75;

        double surfaceY = (surfaceNoise * scale) + depth;

        for (int y = 0; y <= this.noiseSizeY; y++) {
            double surfaceDepth = (double) y / this.noiseSizeY - surfaceY;
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

    @Override
    public double sampleNoise(int x, int y, int z, double xzScale, double yScale, double xzStretch, double yStretch) {
        double nx = x * xzScale;
        double ny = y * yScale;
        double nz = z * xzScale;

        return this.surfaceNoise[0].get(nx, ny, nz) * 128.0;

//        double lerp = (this.lerpNoise.get(x * xzStretch, y * yStretch, z * xzStretch) + 1.0) / 2.0;
//
//        double nx = x * xzScale;
//        double ny = y * yScale;
//        double nz = z * xzScale;
//
//        if (lerp <= 0.0) {
//            return this.lowerNoise.get(nx, ny, nz);
//        } else if (lerp >= 1.0) {
//            return this.upperNoise.get(nx, ny, nz);
//        } else {
//            double lower = this.lowerNoise.get(nx, ny, nz);
//            double upper = this.upperNoise.get(nx, ny, nz);
//            return MathHelper.lerp(lerp, lower, upper);
//        }
    }

    static class SurfaceParameters {
        float depth;
        float scale;
    }
}
