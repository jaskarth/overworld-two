package net.gegy1000.overworldtwo.mixin.decorator;

import net.gegy1000.overworldtwo.decorator.DecoratorStream;
import net.gegy1000.overworldtwo.decorator.NoiseTopStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.NoiseHeightmapDecoratorConfig;
import net.minecraft.world.gen.decorator.NoiseHeightmapDoubleDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(NoiseHeightmapDoubleDecorator.class)
public class MixinNoiseHeightmapDoubleDecorator {
    private final DecoratorStream<NoiseHeightmapDecoratorConfig> stream = new NoiseTopStream(Heightmap.Type.MOTION_BLOCKING, y -> y * 2);

    /**
     * @reason replace with non-allocating stream
     * @author gegy1000
     */
    @Overwrite
    public Stream<BlockPos> getPositions(WorldAccess world, ChunkGenerator generator, Random random, NoiseHeightmapDecoratorConfig config, BlockPos pos) {
        this.stream.open(world, generator, random, config, pos);
        return this.stream;
    }
}
