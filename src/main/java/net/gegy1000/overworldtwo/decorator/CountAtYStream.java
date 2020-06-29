package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.CountDecoratorConfig;

import java.util.Random;

public final class CountAtYStream extends DecoratorStream<CountDecoratorConfig> {
    private final int y;
    private int count;

    public CountAtYStream(int y) {
        this.y = y;
    }

    @Override
    protected void reset() {
        this.count = 0;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, CountDecoratorConfig config, BlockPos origin) {
        if (++this.count >= config.count) {
            return false;
        }

        int x = random.nextInt(16) + origin.getX();
        int z = random.nextInt(16) + origin.getZ();
        output.set(x, this.y, z);

        return true;
    }
}
