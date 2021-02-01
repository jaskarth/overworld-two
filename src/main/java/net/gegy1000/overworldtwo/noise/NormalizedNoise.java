package net.gegy1000.overworldtwo.noise;

public final class NormalizedNoise implements Noise {
    public static final NoiseRange RANGE = NoiseRange.NORMAL;

    private final Noise parent;
    private final double offset;
    private final double radius;

    private NormalizedNoise(Noise parent, double offset, double radius) {
        this.parent = parent;
        this.offset = offset;
        this.radius = radius;
    }

    public static NoiseFactory of(NoiseFactory factory) {
        return seed -> {
            Noise noise = factory.create(seed);

            NoiseRange range = noise.getRange();
            if (RANGE.equals(range)) return noise;

            double min = range.min;
            double max = range.max;
            double width = max - min;

            double radius = width / 2.0;
            double offset = min + radius;
            return new NormalizedNoise(noise, offset, radius);
        };
    }

    @Override
    public double get(double x, double y, double z) {
        return (this.parent.get(x, y, z) - this.offset) / this.radius;
    }

    @Override
    public double get(double x, double y) {
        return (this.parent.get(x, y) - this.offset) / this.radius;
    }

    @Override
    public NoiseRange getRange() {
        return RANGE;
    }
}
