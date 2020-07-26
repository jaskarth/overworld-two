package net.gegy1000.overworldtwo.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gegy1000.overworldtwo.OverworldTwo;
import net.gegy1000.overworldtwo.noise.Noise;
import net.gegy1000.overworldtwo.noise.NoiseFactory;
import net.gegy1000.overworldtwo.noise.NormalizedNoise;
import net.gegy1000.overworldtwo.noise.OctaveNoise;
import net.gegy1000.overworldtwo.noise.PerlinNoise;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
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

    private final Noise[] surfaceNoise;
    private final Noise tearNoise;

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
                .setFrequency(1.0 / 22.0)
                .setLacunarity(1.7)
                .setPersistence(1.0 / 1.8);

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

    private double[] sampleSurfaceParameters(int x, int z) {
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

        return new double[]{(depth * 0.5F) - 0.125F, (scale * 0.9F) + 0.1F};
    }

    @Override
    protected void sampleNoiseColumn(double[] buffer, int x, int z) {
        NoiseConfig noiseConfig = this.field_24774.method_28559();
        double[] params = sampleSurfaceParameters(x, z);
        double scaledDepth = params[0] * 0.265625D;
        double scaledScale = 96.0D / params[1];

        double topTarget = noiseConfig.getTopSlide().getTarget();
        double topSize = noiseConfig.getTopSlide().getSize();
        double topOffset = noiseConfig.getTopSlide().getOffset();
        double bottomTarget = noiseConfig.getBottomSlide().getTarget();
        double bottomSize = noiseConfig.getBottomSlide().getSize();
        double bottomOffset = noiseConfig.getBottomSlide().getOffset();
        double randomDensityOffset = noiseConfig.hasRandomDensityOffset() ? this.method_28553(x, z) : 0.0D;
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
}
