package net.gegy1000.overworldtwo.mixin;

import net.gegy1000.overworldtwo.decorator.DecoratorStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.DecoratorConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;
import java.util.stream.Stream;

@Mixin(Decorator.class)
public abstract class MixinDecorator {
    @Shadow
    public abstract Stream<BlockPos> getPositions(WorldAccess world, ChunkGenerator generator, Random random, DecoratorConfig config, BlockPos pos);

    /**
     * @reason use non-allocating implementation if child class supports it
     * @author gegy1000
     */
    @Overwrite
    public boolean generate(
            ServerWorldAccess world, StructureAccessor structures, ChunkGenerator generator,
            Random random, BlockPos origin,
            DecoratorConfig config, ConfiguredFeature<?, ?> feature
    ) {
        Stream<BlockPos> stream = this.getPositions(world, generator, random, config, origin);

        if (stream instanceof DecoratorStream) {
            DecoratorStream<?> decoratorStream = (DecoratorStream<?>) stream;

            boolean generated = false;

            BlockPos pos;
            while ((pos = decoratorStream.next()) != null) {
                generated |= feature.generate(world, structures, generator, random, pos);
            }

            return generated;
        } else {
            MutableBoolean generated = new MutableBoolean();
            stream.forEach(pos -> {
                if (feature.generate(world, structures, generator, random, pos)) {
                    generated.setTrue();
                }
            });

            return generated.isTrue();
        }
    }
}
