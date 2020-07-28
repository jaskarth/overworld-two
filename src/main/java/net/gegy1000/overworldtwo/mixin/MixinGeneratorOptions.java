package net.gegy1000.overworldtwo.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;

import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Properties;
import java.util.Random;

@Mixin(GeneratorOptions.class)
public class MixinGeneratorOptions {
    @Inject(method = "fromProperties", at = @At("HEAD"), cancellable = true)
    private static void injectEcotones(Properties properties, CallbackInfoReturnable<GeneratorOptions> cir) {
        // no server.properties file generated
        if (properties.get("level-type") == null) {
            return;
        }

        // check for our world type and return if so
        if (properties.get("level-type").toString().trim().toLowerCase().equals("overworld_two")) {
            // get or generate seed
            String seedField = (String) MoreObjects.firstNonNull(properties.get("level-seed"), "");
            long seed = new Random().nextLong();
            if (!seedField.isEmpty()) {
                try {
                    long parsedSeed = Long.parseLong(seedField);
                    if (parsedSeed != 0L) {
                        seed = parsedSeed;
                    }
                } catch (NumberFormatException var14) {
                    seed = seedField.hashCode();
                }
            }

            // get other misc data
            SimpleRegistry<DimensionOptions> dimensions = DimensionType.method_28517(seed);

            String generate_structures = (String)properties.get("generate-structures");
            boolean generateStructures = generate_structures == null || Boolean.parseBoolean(generate_structures);

            // return our chunk generator
            cir.setReturnValue(new GeneratorOptions(seed, generateStructures, false, GeneratorOptions.method_28608(dimensions, new OverworldTwoChunkGenerator(new VanillaLayeredBiomeSource(seed, false, false), seed, OverworldTwoChunkGenerator.TYPE))));
        }
    }
}