package net.gegy1000.overworldtwo.decorator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;

import java.util.Random;
import java.util.function.IntUnaryOperator;

public final class ChanceTopStream extends DecoratorStream<ChanceDecoratorConfig> {
    private final Heightmap.Type heightmap;
    private final IntUnaryOperator transformer;

    private boolean polled;

    public ChanceTopStream(Heightmap.Type heightmap, IntUnaryOperator transformer) {
        this.heightmap = heightmap;
        this.transformer = transformer;
    }

    @Override
    protected void reset() {
        this.polled = false;
    }

    @Override
    protected boolean next(BlockPos.Mutable output, WorldAccess world, ChunkGenerator generator, Random random, ChanceDecoratorConfig config, BlockPos origin) {
        if (this.polled) {
            return false;
        }

        this.polled = true;

        if (random.nextFloat() < 1.0F / config.chance) {
            int x = random.nextInt(16) + origin.getX();
            int z = random.nextInt(16) + origin.getZ();

            int maxY = this.transformer.applyAsInt(world.getTopY(this.heightmap, x, z));
            if (maxY <= 0) return false;

            output.set(x, random.nextInt(maxY), z);
            return true;
        }

        return false;
    }
}
