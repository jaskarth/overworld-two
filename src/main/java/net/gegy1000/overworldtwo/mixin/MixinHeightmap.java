package net.gegy1000.overworldtwo.mixin;

import net.gegy1000.overworldtwo.generator.HeightmapExt;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Heightmap.class)
public class MixinHeightmap implements HeightmapExt {
    @Shadow
    @Final
    private PackedIntegerArray storage;

    @Shadow
    private static int toIndex(int x, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trackUpdate(int x, int y, int z) {
        int index = toIndex(x, z);
        int height = this.storage.get(index);
        if (y > height) {
            this.storage.set(index, y);
        }
    }
}
