package net.gegy1000.overworldtwo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import net.gegy1000.overworldtwo.generator.OverworldTwoGeneratorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class OverworldTwo implements ModInitializer {
    public static final String ID = "overworld_two";

    public static final Logger LOGGER = LogManager.getLogger("OverworldTwo");

    @Override
    public void onInitialize() {
        OverworldTwoChunkGenerator.register();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            OverworldTwoGeneratorType.register();
        }
    }
}
