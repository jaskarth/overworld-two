package net.gegy1000.overworldtwo.generator;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.gegy1000.overworldtwo.OverworldTwo;
import net.gegy1000.overworldtwo.noise.Noise;
import net.gegy1000.overworldtwo.noise.NoiseFactory;
import net.gegy1000.overworldtwo.noise.NormalizedNoise;
import net.gegy1000.overworldtwo.noise.OctaveNoise;
import net.gegy1000.overworldtwo.noise.PerlinNoiseTwo;
import net.minecraft.block.Blocks;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseSamplingConfig;
import net.minecraft.world.gen.chunk.SlideConfig;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class OverworldTwoChunkGenerator extends NoiseChunkGenerator {
    public static final OverworldTwoGenerationSettings OVERWORLD = createOverworld();
    public static final OverworldTwoGenerationSettings NETHER = createNether();

    // Use a 4x smaller array to reduce memory weight and hopefully make it fit into the cache.
    private static final float[] NOISE_WEIGHT_TABLE = Util.make(new float[12 * 12 * 24], (array) -> {
        for(int z = 0; z < 12; z++) {
            for(int x = 0; x < 12; x++) {
                for(int y = 0; y < 24; y++) {
                    array[(z * 12 * 24) + (x * 24) + y] = (float)calculateNoiseWeight(x, y - 12, z);
                }
            }
        }
    });

    public static final Codec<OverworldTwoChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
            Codec.LONG.fieldOf("seed").stable().forGetter(generator -> generator.seed),
            OverworldTwoGenerationSettings.CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
    ).apply(instance, instance.stable(OverworldTwoChunkGenerator::new)));

    private static final int NOISE_RES_XZ = 1;
    private static final int NOISE_RES_Y = 2;

    private final Noise[] surfaceNoise;
    private final Noise tearNoise;
    private final ThreadLocal<BiomeCache> biomeCache;
    private final ThreadLocal<NoiseCache> noiseCache;
    private final Noise extraDensityNoise;
    private final OverworldTwoGenerationSettings settings;
    private int cachedSeaLevel = Integer.MIN_VALUE;

    public OverworldTwoChunkGenerator(BiomeSource biomes, long seed, OverworldTwoGenerationSettings settings) {
        super(biomes, seed, () -> settings.wrapped);
        this.settings = settings;

        ChunkRandom random = new ChunkRandom(seed);

        NoiseFactory surfaceNoise = surfaceNoise(settings);
        this.surfaceNoise = new Noise[] {
                surfaceNoise.create(random.nextLong()),
                surfaceNoise.create(random.nextLong()),
        };

        this.extraDensityNoise = extraDensityNoise(settings).create(random.nextLong());

        NoiseFactory tearNoise = tearNoise(settings);
        this.tearNoise = tearNoise.create(random.nextLong());

        this.biomeCache = ThreadLocal.withInitial(() -> new BiomeCache(128, biomes));
        this.noiseCache = ThreadLocal.withInitial(() -> new NoiseCache(128, this.noiseSizeY + 1));
    }

    private static NoiseFactory surfaceNoise(OverworldTwoGenerationSettings settings) {
        NoiseSamplingConfig config = settings.wrapped.getGenerationShapeConfig().getSampling();

        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / config.getXZScale())
                .setVerticalFrequency(1.0 / config.getYScale())
                .setLacunarity(settings.surfaceLacunarity)
                .setPersistence(1.0 / settings.surfacePersistence);

        octaves.add(PerlinNoiseTwo.create(), 5);

        return NormalizedNoise.of(octaves.build());
    }

    private static NoiseFactory tearNoise(OverworldTwoGenerationSettings settings) {
        NoiseSamplingConfig config = settings.wrapped.getGenerationShapeConfig().getSampling();

        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / config.getXZFactor())
                .setVerticalFrequency(1.0 / config.getYFactor())
                .setLacunarity(settings.tearLacunarity)
                .setPersistence(1.0 / settings.tearPersistence);

        octaves.add(PerlinNoiseTwo.create(), 3);

        return NormalizedNoise.of(octaves.build());
    }

    private static NoiseFactory extraDensityNoise(OverworldTwoGenerationSettings settings) {
        OctaveNoise.Builder octaves = OctaveNoise.builder()
                .setHorizontalFrequency(1.0 / settings.extraDensityScale)
                .setVerticalFrequency(1.0 / settings.extraDensityScale)
                .setLacunarity(settings.extraDensityLacunarity)
                .setPersistence(1.0 / settings.extraDensityPersistence);

        octaves.add(PerlinNoiseTwo.create(), 3);

        return NormalizedNoise.of(octaves.build());
    }

    public static void register() {
        Registry.register(Registry.CHUNK_GENERATOR, new Identifier(OverworldTwo.ID, "overworld_two"), CODEC);
    }

    private static OverworldTwoGenerationSettings createOverworld() {
        StructuresConfig structures = new StructuresConfig(true);

        // Vanilla: 1.0, 1.0, 40.0, 22.0
        NoiseSamplingConfig noiseSampler = new NoiseSamplingConfig(24.0, 24.0, 40.0, 18.0);
        GenerationShapeConfig noise = new GenerationShapeConfig(
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

        ChunkGeneratorSettings type = new ChunkGeneratorSettings(
                structures, noise,
                Blocks.STONE.getDefaultState(),
                Blocks.WATER.getDefaultState(),
                -10, 0, 63,
                false
        );

        return new OverworldTwoGenerationSettings(
                type,
                1.7, 1.8,
                1.35, 2.0,
                150, 1.5, 1.4
        );
    }

    private static OverworldTwoGenerationSettings createNether() {
        StructuresConfig structures = new StructuresConfig(false);
        Map<StructureFeature<?>, StructureConfig> map = Maps.newHashMap(StructuresConfig.DEFAULT_STRUCTURES);
        map.put(StructureFeature.RUINED_PORTAL, new StructureConfig(25, 10, 34222645));

        // Vanilla: 1.0, 3.0, 80.0, 60.0
        NoiseSamplingConfig noiseSampler = new NoiseSamplingConfig(32.0, 10.0, 60.0, 40.0);
        GenerationShapeConfig noise = new GenerationShapeConfig(
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

        ChunkGeneratorSettings type = new ChunkGeneratorSettings(
                new StructuresConfig(Optional.ofNullable(structures.getStronghold()), map),
                noise,
                Blocks.NETHERRACK.getDefaultState(),
                Blocks.LAVA.getDefaultState(),
                0, 0, 32,
                false
        );

        return new OverworldTwoGenerationSettings(
                type,
                1.7, 1.8,
                1.5, 1.75,
                // The nether doesn't use these values
                150, 1.5, 1.4
        );
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return OverworldTwoChunkGenerator.CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new OverworldTwoChunkGenerator(this.biomeSource.withSeed(seed), seed, this.settings);
    }

    private static float biomeWeight(int x, int z) {
        int idx = (x + 2) + (z + 2) * 5;
        return BIOME_WEIGHT_TABLE[idx];
    }

    private SurfaceParameters sampleSurfaceParameters(int x, int z) {
        BiomeCache cache = this.biomeCache.get();

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
    protected double[] sampleNoiseColumn(int x, int z) {
        return this.noiseCache.get().get(new double[this.noiseSizeY + 1], x, z);
    }

    @Override
    protected void sampleNoiseColumn(double[] buffer, int x, int z) {
        this.noiseCache.get().get(buffer, x, z);
    }

    private void fillNoiseColumn(double[] buffer, int x, int z) {
        GenerationShapeConfig noiseConfig = this.settings.wrapped.getGenerationShapeConfig();
        SurfaceParameters params = this.sampleSurfaceParameters(x, z);
        double scaledDepth = params.depth * 0.265625;
        double scaledScale = (96.0 / params.scale) / 200.0;

        double topTarget = noiseConfig.getTopSlide().getTarget() / 200.0;
        double topSize = noiseConfig.getTopSlide().getSize();
        double topOffset = noiseConfig.getTopSlide().getOffset();
        double bottomTarget = noiseConfig.getBottomSlide().getTarget() / 200.0;
        double bottomSize = noiseConfig.getBottomSlide().getSize();
        double bottomOffset = noiseConfig.getBottomSlide().getOffset();
        double randomDensityOffset = noiseConfig.hasRandomDensityOffset() ? this.extraDensityNoiseAt(x, z) : 0.0;
        double densityFactor = noiseConfig.getDensityFactor();
        double densityOffset = noiseConfig.getDensityOffset();

        for (int y = 0; y <= this.noiseSizeY; y++) {
            double yOffset = 1.0 - y * 2.0 / this.noiseSizeY + randomDensityOffset;
            double density = yOffset * densityFactor + densityOffset;
            double falloff = (density + scaledDepth) * scaledScale;

            if (falloff > 0.0D) {
                falloff *= 4.0;
            }

            // Avoid sampling the noise until we've evaluated the falloff.
            // Since the extent of the noise is (-1, 1), adding +/- 2 won't change the final noise
            // So if the falloff is too high or low, we know that the noise won't have any effect on the terrain.
            // This generally happens at low y sections where the terrain is fully solid or at high y sections where it's all air.
            // TODO: While this does look like it's working, I'm almost certain I screwed something up severely here.
            double noise;
            if (falloff > 2 || falloff < -2) {
                noise = 0;
            } else {
                noise = this.getNoiseAt(x, y, z);
            }

            noise += falloff;

            double slide;
            if (topSize > 0.0) {
                slide = ((this.noiseSizeY - y) - topOffset) / topSize;
                noise = MathHelper.clampedLerp(topTarget, noise, slide);
            }

            if (bottomSize > 0.0) {
                slide = (y - bottomOffset) / bottomSize;
                noise = MathHelper.clampedLerp(bottomTarget, noise, slide);
            }

            buffer[y] = MathHelper.clamp(noise, -1.0, 1.0);
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

        return surfaceNoise;
    }

    protected double extraDensityNoiseAt(int x, int z) {
        double rawDensity = this.extraDensityNoise.get(x, 10.0, z);
        double scaledDensity;
        if (rawDensity < 0.0) {
            scaledDensity = -rawDensity * 0.3;
        } else {
            scaledDensity = rawDensity;
        }

        double finalDensity = scaledDensity * 24.575625 - 2.0;
        return finalDensity < 0.0 ? finalDensity * 0.009486607142857142 : Math.min(finalDensity, 1.0) * 0.006640625;
    }

    @Override
    public void populateNoise(WorldAccess world, StructureAccessor structures, Chunk chunk) {
        ObjectList<StructurePiece> pieces = new ObjectArrayList<>(10);
        ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);

        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        this.collectStructures(world, chunk, pieces, junctions);

        double[][] noiseX0 = new double[this.noiseSizeZ + 1][this.noiseSizeY + 1];
        double[][] noiseX1 = new double[this.noiseSizeZ + 1][this.noiseSizeY + 1];

        for (int z = 0; z < this.noiseSizeZ + 1; z++) {
            noiseX0[z] = new double[this.noiseSizeY + 1];
            this.sampleNoiseColumn(noiseX0[z], chunkX * this.noiseSizeX, chunkZ * this.noiseSizeZ + z);
            noiseX1[z] = new double[this.noiseSizeY + 1];
        }

        ProtoChunk protoChunk = (ProtoChunk) chunk;

        try (
                NoiseBlockWriter surfaceWriter = new NoiseBlockWriter(protoChunk, this.defaultBlock);
                NoiseBlockWriter fluidWriter = new NoiseBlockWriter(protoChunk, this.defaultFluid)
        ) {
            for (int noiseX = 0; noiseX < this.noiseSizeX; noiseX++) {
                for (int noiseZ = 0; noiseZ < this.noiseSizeZ + 1; noiseZ++) {
                    this.sampleNoiseColumn(noiseX1[noiseZ], chunkX * this.noiseSizeX + noiseX + 1, chunkZ * this.noiseSizeZ + noiseZ);
                }

                for (int noiseZ = 0; noiseZ < this.noiseSizeZ; noiseZ++) {
                    this.populateNoiseColumn(
                            pieces, junctions,
                            protoChunk, surfaceWriter, fluidWriter,
                            noiseX0[noiseZ], noiseX1[noiseZ],
                            noiseX0[noiseZ + 1], noiseX1[noiseZ + 1],
                            noiseX, noiseZ
                    );
                }

                double[][] swap = noiseX0;
                noiseX0 = noiseX1;
                noiseX1 = swap;
            }
        }
    }

    private void populateNoiseColumn(
            ObjectList<StructurePiece> pieces, ObjectList<JigsawJunction> junctions,
            ProtoChunk chunk, NoiseBlockWriter surfaceWriter, NoiseBlockWriter fluidWriter,
            double[] noiseX0Z0, double[] noiseX1Z0,
            double[] noiseX0Z1, double[] noiseX1Z1,
            int noiseX, int noiseZ
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int minChunkX = chunkPos.getStartX();
        int minChunkZ = chunkPos.getStartZ();

        int xzRes = this.horizontalNoiseResolution;
        int yRes = this.verticalNoiseResolution;

        int seaLevel = this.getSeaLevel();

        int lastSectionY = -1;

        boolean hasStructures = !pieces.isEmpty() || !junctions.isEmpty();

        int noiseY = this.noiseSizeY - 1;
        double lx0y1z0 = noiseX0Z0[noiseY + 1];
        double lx0y1z1 = noiseX0Z1[noiseY + 1];
        double lx1y1z0 = noiseX1Z0[noiseY + 1];
        double lx1y1z1 = noiseX1Z1[noiseY + 1];

        for (; noiseY >= 0; noiseY--) {
            double x0y0z0 = noiseX0Z0[noiseY];
            double x0y0z1 = noiseX0Z1[noiseY];
            double x1y0z0 = noiseX1Z0[noiseY];
            double x1y0z1 = noiseX1Z1[noiseY];
            double x0y1z0 = lx0y1z0;
            double x0y1z1 = lx0y1z1;
            double x1y1z0 = lx1y1z0;
            double x1y1z1 = lx1y1z1;

            lx0y1z0 = x0y0z0;
            lx0y1z1 = x0y0z1;
            lx1y1z0 = x1y0z0;
            lx1y1z1 = x1y0z1;

            double dx0z0 = x0y1z0 - x0y0z0;
            double dx1z0 = x1y1z0 - x1y0z0;
            double dx0z1 = x0y1z1 - x0y0z1;
            double dx1z1 = x1y1z1 - x1y0z1;

            for (int localY = yRes - 1; localY >= 0; localY--) {
                int globalY = noiseY * yRes + localY;
                int sectionLocalY = globalY & 15;

                int sectionY = globalY >> 4;
                if (lastSectionY != sectionY) {
                    ChunkSection section = chunk.getSection(sectionY);
                    lastSectionY = sectionY;

                    surfaceWriter.setSection(section);
                    fluidWriter.setSection(section);
                }

                double iy = (double) localY / yRes;
                double x0z0 = iy * dx0z0 + x0y0z0;
                double x1z0 = iy * dx1z0 + x1y0z0;
                double x0z1 = iy * dx0z1 + x0y0z1;
                double x1z1 = iy * dx1z1 + x1y0z1;

                double dz0 = x1z0 - x0z0;
                double dz1 = x1z1 - x0z1;

                for (int localX = 0; localX < xzRes; localX++) {
                    int globalX = minChunkX + noiseX * xzRes + localX;
                    int sectionLocalX = globalX & 15;

                    double ix = (double) localX / xzRes;
                    double z0 = ix * dz0 + x0z0;
                    double z1 = ix * dz1 + x0z1;
                    double dz = z1 - z0;

                    for (int localZ = 0; localZ < xzRes; ++localZ) {
                        int globalZ = minChunkZ + noiseZ * xzRes + localZ;
                        int sectionLocalZ = globalZ & 15;

                        double iz = (double) localZ / xzRes;
                        double noise = iz * dz + z0;

                        boolean solid = noise > 0.0;

                        // avoid further structure computation given we know we will be solid already
                        if (!solid && hasStructures) {
                            solid = evaluateSolid(pieces, junctions, globalX, globalY, globalZ, noise);
                        }

                        if (solid) {
                            surfaceWriter.set(sectionLocalX, sectionLocalY, sectionLocalZ);
                        } else if (globalY < seaLevel) {
                            fluidWriter.set(sectionLocalX, sectionLocalY, sectionLocalZ);
                        }
                    }
                }
            }
        }
    }

    private static boolean evaluateSolid(
            ObjectList<StructurePiece> pieces, ObjectList<JigsawJunction> junctions,
            int globalX, int globalY, int globalZ,
            double noise
    ) {
        double density = noise / 2.0 - noise * noise * noise / 24.0;

        for (int i = 0; i < pieces.size(); i++) {
            StructurePiece piece = pieces.get(i);

            BlockBox bounds = piece.getBoundingBox();
            int dx = Math.max(0, Math.max(bounds.minX - globalX, globalX - bounds.maxX));
            int dy = globalY - (bounds.minY + (piece instanceof PoolStructurePiece ? ((PoolStructurePiece) piece).getGroundLevelDelta() : 0));
            int dz = Math.max(0, Math.max(bounds.minZ - globalZ, globalZ - bounds.maxZ));
            density += getNoiseWeight(Math.abs(dx), dy, Math.abs(dz)) * 0.8;
        }

        for (int i = 0; i < junctions.size(); i++) {
            JigsawJunction junction = junctions.get(i);

            int dx = globalX - junction.getSourceX();
            int dy = globalY - junction.getSourceGroundY();
            int dz = globalZ - junction.getSourceZ();

            density += getNoiseWeight(Math.abs(dx), dy, Math.abs(dz)) * 0.4;
        }

        return density > 0.0;
    }

    private void collectStructures(WorldAccess world, Chunk chunk, ObjectList<StructurePiece> pieces, ObjectList<JigsawJunction> junctions) {
        ChunkPos chunkPos = chunk.getPos();

        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        int maxX = chunkPos.getEndX();
        int maxZ = chunkPos.getEndZ();

        Map<StructureFeature<?>, LongSet> structureReferences = chunk.getStructureReferences();
        if (structureReferences.isEmpty()) {
            return;
        }

        for (StructureFeature<?> feature : StructureFeature.JIGSAW_STRUCTURES) {
            LongSet references = structureReferences.get(feature);
            if (references == null) continue;

            LongIterator referenceIterator = references.iterator();

            while (referenceIterator.hasNext()) {
                long packedReference = referenceIterator.nextLong();
                int referenceX = ChunkPos.getPackedX(packedReference);
                int referenceZ = ChunkPos.getPackedZ(packedReference);

                Chunk referenceChunk = world.getChunk(referenceX, referenceZ, ChunkStatus.STRUCTURE_STARTS);
                StructureStart<?> start = referenceChunk.getStructureStart(feature);
                if (start == null || !start.hasChildren()) {
                    continue;
                }

                for (StructurePiece piece : start.getChildren()) {
                    int radius = 12;
                    if (!piece.intersectsChunk(chunkPos, radius)) {
                        continue;
                    }

                    if (piece instanceof PoolStructurePiece) {
                        PoolStructurePiece pooledPiece = (PoolStructurePiece) piece;
                        StructurePool.Projection projection = pooledPiece.getPoolElement().getProjection();
                        if (projection == StructurePool.Projection.RIGID) {
                            pieces.add(pooledPiece);
                        }

                        for (JigsawJunction junction : pooledPiece.getJunctions()) {
                            int junctionX = junction.getSourceX();
                            int junctionZ = junction.getSourceZ();
                            if (junctionX > minX - radius && junctionZ > minZ - radius && junctionX < maxX + radius && junctionZ < maxZ + radius) {
                                junctions.add(junction);
                            }
                        }
                    } else {
                        pieces.add(piece);
                    }
                }
            }
        }
    }

    @Override
    public int getSeaLevel() {
        int cachedSeaLevel = this.cachedSeaLevel;
        if (cachedSeaLevel == Integer.MIN_VALUE) {
            this.cachedSeaLevel = cachedSeaLevel = this.settings.wrapped.getSeaLevel();
        }
        return cachedSeaLevel;
    }

    public static double getNoiseWeight(int x, int y, int z) {
        int dy = y + 12;

        if (x >= 0 && x < 12 && z >= 0 && z < 12 && dy >= 0 && dy < 24) {
            return NOISE_WEIGHT_TABLE[(z * 12 * 24) + (x * 24) + dy];
        }

        return 0.0D;
    }

    private static class SurfaceParameters {
        private final float depth;
        private final float scale;

        public SurfaceParameters(float depth, float scale) {
            this.depth = depth;
            this.scale = scale;
        }
    }

    private static class BiomeCache {
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
            Biome sampled = this.source.getBiomeForNoiseGen(x, 0, z);
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

    private class NoiseCache {
        private final long[] keys;
        private final double[] values;

        private final int mask;

        private NoiseCache(int size, int noiseSize) {
            size = MathHelper.smallestEncompassingPowerOfTwo(size);
            this.mask = size - 1;

            this.keys = new long[size];
            Arrays.fill(this.keys, Long.MIN_VALUE);
            this.values = new double[size * noiseSize];
        }

        public double[] get(double[] buffer, int x, int z) {
            long key = this.key(x, z);
            int idx = this.hash(key) & this.mask;

            // if the entry here has a key that matches ours, we have a cache hit
            if (this.keys[idx] == key) {
                // Copy values into buffer
                System.arraycopy(this.values, idx * buffer.length, buffer, 0, buffer.length);
            } else {
                // cache miss: sample and put the result into our cache entry

                // Sample the noise column to store the new values
                OverworldTwoChunkGenerator.this.fillNoiseColumn(buffer, x, z);

                // Create copy of the array
                System.arraycopy(buffer, 0, this.values, idx * buffer.length, buffer.length);

                this.keys[idx] = key;
            }

            return buffer;
        }

        private int hash(long key) {
            return (int) HashCommon.mix(key);
        }

        private long key(int x, int z) {
            return ChunkPos.toLong(x, z);
        }
    }
}
