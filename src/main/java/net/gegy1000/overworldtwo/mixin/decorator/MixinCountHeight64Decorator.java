package net.gegy1000.overworldtwo.mixin.decorator;

import net.gegy1000.overworldtwo.decorator.CountAtYStream;
import net.gegy1000.overworldtwo.decorator.DecoratorStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.CountDecoratorConfig;
import net.minecraft.world.gen.decorator.CountHeight64Decorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(CountHeight64Decorator.class)
public class MixinCountHeight64Decorator {
    private final DecoratorStream<CountDecoratorConfig> stream = new CountAtYStream(64);

    /**
     * @reason replace with non-allocating stream
     * @author gegy1000
     */
    @Overwrite
    public Stream<BlockPos> getPositions(WorldAccess world, ChunkGenerator generator, Random random, CountDecoratorConfig config, BlockPos pos) {
        this.stream.open(world, generator, random, config, pos);
        return this.stream;
    }
}
