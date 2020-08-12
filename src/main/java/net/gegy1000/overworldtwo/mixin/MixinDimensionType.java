package net.gegy1000.overworldtwo.mixin;

import net.gegy1000.overworldtwo.config.OverworldTwoConfig;
import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;

@Mixin(DimensionType.class)
public class MixinDimensionType {

	/**
	 * @reason nether-two :D
	 *
	 * @author SuperCoder79
	 */
	@Overwrite
	private static ChunkGenerator createNetherGenerator(long seed) {
		if (OverworldTwoConfig.get().generateNether) {
			return new OverworldTwoChunkGenerator(MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(seed), seed, OverworldTwoChunkGenerator.NETHER);
		}

		// Vanilla generator
		return new SurfaceChunkGenerator(MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(seed), seed, ChunkGeneratorType.Preset.NETHER.getChunkGeneratorType());
	}
}
