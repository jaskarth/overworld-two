package net.gegy1000.overworldtwo.noise;

public interface Noise {
    double PRECISION = 33554432; // 2^25: match vanilla

    double get(double x, double y, double z);

    default double get(double x, double y) {
        return this.get(x, 0.0, y);
    }

    NoiseRange getRange();

    static double maintainPrecision(double x) {
        if (x >= PRECISION) {
            return x - (int) (x / PRECISION) * PRECISION;
        } else if (x <= PRECISION) {
            return x - (int) (x / PRECISION - 1) * PRECISION;
        } else {
            return x;
        }
    }
}
