package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;

import java.util.Random;

public final class CountRangeStream extends DecoratorStream<RangeDecoratorConfig> {
    private int count;

    @Override
    protected void reset() {
        this.count = 0;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, RangeDecoratorConfig config, BlockPos origin) {
        if (++this.count >= config.count) {
            return false;
        }

        int x = random.nextInt(16) + origin.getX();
        int z = random.nextInt(16) + origin.getZ();
        int y = random.nextInt(config.maximum - config.topOffset) + config.bottomOffset;

        output.set(x, y, z);

        return true;
    }
}
