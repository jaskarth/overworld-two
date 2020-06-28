package net.gegy1000.overworldtwo.noise;

public final class NoiseRange {
    public static final NoiseRange NORMAL = new NoiseRange(-1.0, 1.0);

    public final double min;
    public final double max;

    public NoiseRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj instanceof NoiseRange) {
            NoiseRange range = (NoiseRange) obj;
            return Math.abs(this.min - range.min) < 1e-4
                    && Math.abs(this.max - range.max) < 1e-4;
        }

        return false;
    }
}
