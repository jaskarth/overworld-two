package net.gegy1000.overworldtwo.decorator;

import java.util.Random;
import java.util.function.IntUnaryOperator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.CountDecoratorConfig;

public final class CountHeightmapStream extends DecoratorStream<CountDecoratorConfig> {
    private final Heightmap.Type heightmap;

    private int count;

    public CountHeightmapStream(Heightmap.Type heightmap) {
        this.heightmap = heightmap;
    }

    @Override
    protected void reset() {
        this.count = 0;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, CountDecoratorConfig config, BlockPos origin) {
        while (++this.count < config.count) {
            int x = random.nextInt(16) + origin.getX();
            int z = random.nextInt(16) + origin.getZ();

            int y = world.getTopY(this.heightmap, x, z);
            if (y <= 0) continue;

            output.set(x, y, z);
            return true;
        }

        return false;
    }
}
