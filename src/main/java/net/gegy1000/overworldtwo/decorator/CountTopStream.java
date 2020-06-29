package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.CountDecoratorConfig;

import java.util.Random;
import java.util.function.IntUnaryOperator;

public final class CountTopStream extends DecoratorStream<CountDecoratorConfig> {
    private final Heightmap.Type heightmap;
    private final IntUnaryOperator transformer;

    private int count;

    public CountTopStream(Heightmap.Type heightmap, IntUnaryOperator transformer) {
        this.heightmap = heightmap;
        this.transformer = transformer;
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

            int maxY = this.transformer.applyAsInt(world.getTopY(this.heightmap, x, z));
            if (maxY <= 0) continue;

            output.set(x, random.nextInt(maxY), z);
            return true;
        }

        return false;
    }
}
