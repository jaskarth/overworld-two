package net.gegy1000.overworldtwo.generator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

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
    protected ChunkGenerator getChunkGenerator(Registry<Biome> biomes, Registry<ChunkGeneratorSettings> chunkgens, long seed) {
        return new OverworldTwoChunkGenerator(new VanillaLayeredBiomeSource(seed, false, false, biomes), seed, OverworldTwoChunkGenerator.OVERWORLD);
    }
}
