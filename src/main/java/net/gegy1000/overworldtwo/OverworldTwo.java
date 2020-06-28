package net.gegy1000.overworldtwo;

import net.fabricmc.api.ModInitializer;
import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import net.gegy1000.overworldtwo.generator.OverworldTwoGeneratorType;

public final class OverworldTwo implements ModInitializer {
    public static final String ID = "overworld_two";

    @Override
    public void onInitialize() {
        OverworldTwoChunkGenerator.register();
        OverworldTwoGeneratorType.register();
    }
}
