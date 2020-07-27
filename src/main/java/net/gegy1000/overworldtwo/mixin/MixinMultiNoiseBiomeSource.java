package net.gegy1000.overworldtwo.mixin;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;

@Mixin(MultiNoiseBiomeSource.class)
public class MixinMultiNoiseBiomeSource {

	/**
	 * Testing purposes only! Makes the only nether biome Basalt Deltas for data collection.
	 * @author SuperCoder79
	 */
	@Overwrite
	private static MultiNoiseBiomeSource method_28467(long l) {
		ImmutableList<Biome> immutableList = ImmutableList.of(Biomes.BASALT_DELTAS);
		return new MultiNoiseBiomeSource(l, immutableList.stream().flatMap((biome) -> biome.streamNoises().map((point) -> Pair.of(point, biome))).collect(ImmutableList.toImmutableList()), Optional.of(MultiNoiseBiomeSource.Preset.NETHER));
	}
}
