package net.gegy1000.overworldtwo.mixin;

import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(DimensionType.class)
public class MixinDimensionType {

	/**
	 * @reason nether-two :D
	 * @author SuperCoder79
	 */
	@Overwrite
	private static ChunkGenerator createNetherGenerator(long seed) {
		return new OverworldTwoChunkGenerator(MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(seed), seed, OverworldTwoChunkGenerator.NETHER);
	}
}
