package net.gegy1000.overworldtwo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import net.gegy1000.overworldtwo.generator.OverworldTwoGeneratorType;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public final class OverworldTwo implements ModInitializer {
    public static final String ID = "overworld_two";

    public static final Logger LOGGER = LogManager.getLogger("OverworldTwo");

    @Override
    public void onInitialize() {
        this.replaceOreFeature();

        OverworldTwoChunkGenerator.register();
        OverworldTwoGeneratorType.register();
    }

    private void replaceOreFeature() {
        MappingResolver mappings = FabricLoader.getInstance().getMappingResolver();

        try {
            Field oreField = Feature.class.getDeclaredField(mappings.mapFieldName(
                    "named",
                    "net.minecraft.world.gen.feature.Feature",
                    "ORE",
                    "Lnet/minecraft/world/gen/feature/Feature;"
            ));

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(oreField, oreField.getModifiers() & ~Modifier.FINAL);

            oreField.set(null, new Ow2OreFeature(OreFeatureConfig.CODEC));
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to replace ore feature", e);
        }

        GenerationStep.Feature[] steps = GenerationStep.Feature.values();
        for (Biome biome : Registry.BIOME) {
            for (GenerationStep.Feature step : steps) {
                List<ConfiguredFeature<?, ?>> features = biome.getFeaturesForStep(step);
                features.replaceAll(configured -> {
                    if (configured.feature instanceof DecoratedFeature) {
                        DecoratedFeatureConfig decoratedConfig = (DecoratedFeatureConfig) configured.config;
                        if (decoratedConfig.feature.feature instanceof OreFeature) {
                            return Feature.ORE.configure((OreFeatureConfig) decoratedConfig.feature.config)
                                    .createDecoratedFeature(decoratedConfig.decorator);
                        }
                    }
                    return configured;
                });
            }
        }
    }
}
