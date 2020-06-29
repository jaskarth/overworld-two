package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.NoiseHeightmapDecoratorConfig;

import java.util.Random;
import java.util.function.IntUnaryOperator;

public final class NoiseTopStream extends DecoratorStream<NoiseHeightmapDecoratorConfig> {
    private final Heightmap.Type heightmap;
    private final IntUnaryOperator transformer;

    private int count;
    private int totalCount;

    public NoiseTopStream(Heightmap.Type heightmap, IntUnaryOperator transformer) {
        this.heightmap = heightmap;
        this.transformer = transformer;
    }

    @Override
    protected void reset() {
        this.count = 0;
        this.totalCount = Integer.MIN_VALUE;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, NoiseHeightmapDecoratorConfig config, BlockPos origin) {
        if (this.totalCount == Integer.MIN_VALUE) {
            double noise = Biome.FOLIAGE_NOISE.sample(origin.getX() / 200.0, origin.getZ() / 200.0, false);
            this.totalCount = noise < config.noiseLevel ? config.belowNoise : config.aboveNoise;
        }

        while (++this.count < this.totalCount) {
            int x = random.nextInt(16) + origin.getX();
            int z = random.nextInt(16) + origin.getZ();

            int maxY = this.transformer.applyAsInt(world.getTopY(this.heightmap, x, z));
            if (maxY <= 0) continue;

            output.set(x, random.nextInt(maxY), z);
            return true;
        }

        return false;
    }
}
