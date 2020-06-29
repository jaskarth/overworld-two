package net.gegy1000.overworldtwo;

import net.fabricmc.api.ModInitializer;
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
        OverworldTwoGeneratorType.register();
    }
}
