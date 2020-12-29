package net.gegy1000.overworldtwo.mixin;

import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PalettedContainer.class)
public interface PalettedContainerAccess<T> {
    @Accessor
    Palette<T> getPalette();

    @Accessor
    PackedIntegerArray getData();
}
