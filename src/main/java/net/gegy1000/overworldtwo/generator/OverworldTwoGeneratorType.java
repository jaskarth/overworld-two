package net.gegy1000.overworldtwo.generator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Environment(EnvType.CLIENT)
public final class OverworldTwoGeneratorType extends GeneratorType {
    public static final GeneratorType INSTANCE = new OverworldTwoGeneratorType();

    private OverworldTwoGeneratorType() {
        super("overworld_two");
    }

    public static void register() {
        GeneratorType.VALUES.add(INSTANCE);
    }

    @Override
    protected ChunkGenerator method_29076(long seed) {
        VanillaLayeredBiomeSource biomes = new VanillaLayeredBiomeSource(seed, false, false);
        return new OverworldTwoChunkGenerator(biomes, seed, OverworldTwoChunkGenerator.OVERWORLD);
    }
}
