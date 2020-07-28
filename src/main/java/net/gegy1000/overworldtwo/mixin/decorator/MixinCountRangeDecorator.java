package net.gegy1000.overworldtwo.mixin.decorator;

import net.gegy1000.overworldtwo.decorator.CountRangeStream;
import net.gegy1000.overworldtwo.decorator.DecoratorStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.decorator.CountRangeDecorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(CountRangeDecorator.class)
public class MixinCountRangeDecorator {
    private final DecoratorStream<RangeDecoratorConfig> stream = new CountRangeStream((config, random) -> random.nextInt(config.maximum - config.topOffset) + config.bottomOffset);

    /**
     * @reason replace with non-allocating stream
     * @author gegy1000
     */
    @Overwrite
    public Stream<BlockPos> getPositions(Random random, RangeDecoratorConfig config, BlockPos pos) {
        this.stream.open(null, null, random, config, pos);
        return this.stream;
    }
}
