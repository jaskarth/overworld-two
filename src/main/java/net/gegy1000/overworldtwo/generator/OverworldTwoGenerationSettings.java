package net.gegy1000.overworldtwo.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.gen.chunk.ChunkGeneratorType;

public class OverworldTwoGenerationSettings {
	public static final Codec<OverworldTwoGenerationSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ChunkGeneratorType.field_24780.fieldOf("type").forGetter(settings -> settings.wrapped),
			Codec.DOUBLE.fieldOf("surface_lacunarity").forGetter(settings -> settings.surfaceLacunarity),
			Codec.DOUBLE.fieldOf("surface_persistence").forGetter(settings -> settings.surfacePersistence),
			Codec.DOUBLE.fieldOf("tear_lacunarity").forGetter(settings -> settings.tearLacunarity),
			Codec.DOUBLE.fieldOf("tear_persistence").forGetter(settings -> settings.tearPersistence),
			Codec.DOUBLE.fieldOf("extra_density_scale").forGetter(settings -> settings.extraDensityScale),
			Codec.DOUBLE.fieldOf("extra_density_lacunarity").forGetter(settings -> settings.extraDensityLacunarity),
			Codec.DOUBLE.fieldOf("extra_density_persistence").forGetter(settings -> settings.extraDensityPersistence)
	).apply(instance, OverworldTwoGenerationSettings::new));
	public final ChunkGeneratorType wrapped;
	public final double surfaceLacunarity;
	public final double surfacePersistence;
	public final double tearLacunarity;
	public final double tearPersistence;
	public final double extraDensityScale;
	public final double extraDensityLacunarity;
	public final double extraDensityPersistence;

	public OverworldTwoGenerationSettings(ChunkGeneratorType wrapped, double surfaceLacunarity, double surfacePersistence, double tearLacunarity, double tearPersistence, double extraDensityScale, double extraDensityLacunarity, double extraDensityPersistence) {
		this.wrapped = wrapped;
		this.surfaceLacunarity = surfaceLacunarity;
		this.surfacePersistence = surfacePersistence;
		this.tearLacunarity = tearLacunarity;
		this.tearPersistence = tearPersistence;
		this.extraDensityScale = extraDensityScale;
		this.extraDensityLacunarity = extraDensityLacunarity;
		this.extraDensityPersistence = extraDensityPersistence;
	}
}
