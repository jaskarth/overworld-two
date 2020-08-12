package net.gegy1000.overworldtwo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.gegy1000.overworldtwo.OverworldTwo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class OverworldTwoConfig {
    public static final int VERSION = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static OverworldTwoConfig config;

    public int version = VERSION;

    @SerializedName("generate_nether")
    public boolean generateNether = true;

    public static OverworldTwoConfig get() {
        if (config == null) {
            try {
                config = loadConfig();
            } catch (IOException e) {
                OverworldTwo.LOGGER.warn("Failed to read config file", e);
                config = new OverworldTwoConfig();
            }
        }

        return config;
    }

    private static OverworldTwoConfig loadConfig() throws IOException {
        Path path = Paths.get("config", "overworldtwo.json");
        if (!Files.exists(path)) {
            return createConfig(path);
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            OverworldTwoConfig config = parseConfig(reader);

            // version change: recreate the config
            if (config.version != VERSION) {
                return createConfig(path);
            }

            return config;
        }
    }

    private static OverworldTwoConfig createConfig(Path path) throws IOException {
        OverworldTwoConfig config = new OverworldTwoConfig();

        Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(GSON.toJson(config));
        }

        return config;
    }

    private static OverworldTwoConfig parseConfig(Reader reader) {
        return GSON.fromJson(reader, OverworldTwoConfig.class);
    }
}
