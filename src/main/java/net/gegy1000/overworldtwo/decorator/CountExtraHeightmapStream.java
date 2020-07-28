package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.CountExtraChanceDecoratorConfig;

import java.util.Random;

public final class CountExtraHeightmapStream extends DecoratorStream<CountExtraChanceDecoratorConfig> {
    private final Heightmap.Type heightmap;

    private int count;
    private int totalCount;

    public CountExtraHeightmapStream(Heightmap.Type heightmap) {
        this.heightmap = heightmap;
    }

    @Override
    protected void reset() {
        this.count = 0;
        this.totalCount = Integer.MIN_VALUE;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, CountExtraChanceDecoratorConfig config, BlockPos origin) {
        if (this.totalCount == Integer.MIN_VALUE) {
            this.totalCount = config.count;
            if (random.nextFloat() < config.extraChance) {
                this.totalCount += config.extraCount;
            }
        }

        if (++this.count >= this.totalCount) {
            return false;
        }

        int x = random.nextInt(16) + origin.getX();
        int z = random.nextInt(16) + origin.getZ();
        int y = world.getTopY(this.heightmap, x, z);

        output.set(x, y, z);

        return true;
    }
}
