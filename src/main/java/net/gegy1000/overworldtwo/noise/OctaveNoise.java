package net.gegy1000.overworldtwo.noise;

import net.minecraft.world.biome.source.SeedMixer;

import java.util.ArrayList;
import java.util.List;

public final class OctaveNoise implements Noise {
    private final Noise[] layers;
    private final NoiseRange range;

    private final double amplitude;
    private final double frequency;

    private final double persistence;
    private final double lacunarity;

    OctaveNoise(
            Noise[] layers, NoiseRange range,
            double amplitude, double frequency,
            double persistence, double lacunarity
    ) {
        this.layers = layers;
        this.range = range;

        this.amplitude = amplitude;
        this.frequency = frequency;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public double get(double x, double y, double z) {
        double amplitude = this.amplitude;
        double frequency = this.frequency;
        double persistence = this.persistence;
        double lacunarity = this.lacunarity;

        double accumulator = 0.0;

        x *= frequency;
        y *= frequency;
        z *= frequency;

        Noise[] layers = this.layers;
        for (int i = 0; i < layers.length; i++) {
            Noise layer = layers[i];
            accumulator += layer.get(x, y, z) * amplitude;

            amplitude *= persistence;
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
        }

        return accumulator;
    }

    @Override
    public double get(double x, double y) {
        double amplitude = this.amplitude;
        double frequency = this.frequency;
        double persistence = this.persistence;
        double lacunarity = this.lacunarity;

        double accumulator = 0.0;

        x *= frequency;
        y *= frequency;

        Noise[] layers = this.layers;
        for (int i = 0; i < layers.length; i++) {
            Noise layer = layers[i];
            accumulator += layer.get(x, y) * amplitude;

            amplitude *= persistence;
            x *= lacunarity;
            y *= lacunarity;
        }

        return accumulator;
    }

    @Override
    public NoiseRange getRange() {
        return this.range;
    }

    public static class Builder {
        private double amplitude = 1.0;
        private double frequency = 1.0;

        private double persistence = 1.0 / 2.0;
        private double lacunarity = 2.0;

        private final List<NoiseFactory> octaves = new ArrayList<>();

        Builder() {
        }

        public Builder setAmplitude(double amplitude) {
            this.amplitude = amplitude;
            return this;
        }

        public Builder setFrequency(double frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder setPersistence(double persistence) {
            this.persistence = persistence;
            return this;
        }

        public Builder setLacunarity(double lacunarity) {
            this.lacunarity = lacunarity;
            return this;
        }

        public Builder add(NoiseFactory octave) {
            this.octaves.add(octave);
            return this;
        }

        public Builder add(NoiseFactory octave, int count) {
            for (int i = 0; i < count; i++) {
                this.octaves.add(octave);
            }
            return this;
        }

        public NoiseFactory build() {
            return seed -> {
                Noise[] octaves = this.createOctaves(seed);
                NoiseRange range = this.computeRange(octaves);

                return new OctaveNoise(
                        octaves, range,
                        this.amplitude, this.frequency,
                        this.persistence, this.lacunarity
                );
            };
        }

        private Noise[] createOctaves(long seed) {
            Noise[] layers = new Noise[this.octaves.size()];

            for (int i = 0; i < layers.length; i++) {
                layers[i] = this.octaves.get(i).create(seed);
                seed = SeedMixer.mixSeed(seed, i);
            }

            return layers;
        }

        private NoiseRange computeRange(Noise[] octaves) {
            double min = 0.0;
            double max = 0.0;

            double amplitude = this.amplitude;

            for (Noise layer : octaves) {
                NoiseRange range = layer.getRange();
                min += range.min * amplitude;
                max += range.max * amplitude;
                amplitude *= this.persistence;
            }

            return new NoiseRange(min, max);
        }
    }
}
