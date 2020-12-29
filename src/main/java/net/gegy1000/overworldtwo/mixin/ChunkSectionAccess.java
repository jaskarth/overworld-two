package net.gegy1000.overworldtwo.mixin;

import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSection.class)
public interface ChunkSectionAccess {
    @Accessor
    void setNonEmptyBlockCount(short nonEmptyBlockCount);

    @Accessor
    short getNonEmptyBlockCount();

    @Accessor
    void setRandomTickableBlockCount(short randomTickableBlockCount);

    @Accessor
    short getRandomTickableBlockCount();

    @Accessor
    void setNonEmptyFluidCount(short nonEmptyFluidCount);

    @Accessor
    short getNonEmptyFluidCount();
}
