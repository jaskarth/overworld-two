package net.gegy1000.overworldtwo.mixin.decorator;

import net.gegy1000.overworldtwo.decorator.ChanceTopStream;
import net.gegy1000.overworldtwo.decorator.DecoratorStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.ChanceHeightmapDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(ChanceHeightmapDecorator.class)
public class MixinChanceHeightmapDecorator {
    private final DecoratorStream<ChanceDecoratorConfig> stream = new ChanceTopStream(Heightmap.Type.MOTION_BLOCKING, y -> y);

    /**
     * @reason replace with non-allocating stream
     * @author gegy1000
     */
    @Overwrite
    public Stream<BlockPos> getPositions(WorldAccess world, ChunkGenerator generator, Random random, ChanceDecoratorConfig config, BlockPos pos) {
        this.stream.open(world, generator, random, config, pos);
        return this.stream;
    }
}
