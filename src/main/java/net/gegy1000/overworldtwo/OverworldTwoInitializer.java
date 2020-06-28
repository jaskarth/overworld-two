package net.gegy1000.overworldtwo;

import net.fabricmc.api.ModInitializer;
import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import net.gegy1000.overworldtwo.generator.OverworldTwoGeneratorType;

public final class OverworldTwoInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        OverworldTwoChunkGenerator.register();
        OverworldTwoGeneratorType.register();
    }
}
