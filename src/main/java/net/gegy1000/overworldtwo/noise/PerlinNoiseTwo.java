package net.gegy1000.overworldtwo.noise;

import java.util.Random;

public class PerlinNoiseTwo extends PerlinNoise {
	protected PerlinNoiseTwo(Random random) {
		super(random);
	}

	public static NoiseFactory create() {
		return (seed) -> new PerlinNoiseTwo(new Random(seed));
	}

	/**
	 * Re-oriented perlin noise that hides most of it's artifacts.
	 */
	@Override
	public double get(double x, double y, double z) {
		double xz = x + z;
		double s2 = xz * -0.211324865405187;
		double yy = y * 0.577350269189626;
		double xr = x + s2 + yy;
		double zr = z + s2 + yy;
		double yr = xz * -0.577350269189626 + yy;

		return super.get(xr, yr, zr);
	}
}
