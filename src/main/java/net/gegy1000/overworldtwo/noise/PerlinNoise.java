package net.gegy1000.overworldtwo.noise;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class PerlinNoise implements Noise {
    public static final NoiseRange RANGE = NoiseRange.NORMAL;

    protected final double originX;
    protected final double originY;
    protected final double originZ;
    protected final int[] permutations;

    protected PerlinNoise(Random random) {
        this.originX = random.nextDouble() * 256.0;
        this.originY = random.nextDouble() * 256.0;
        this.originZ = random.nextDouble() * 256.0;

        this.permutations = PerlinHelper.initPermutationTable(random);
    }

    public static NoiseFactory create() {
        return (seed) -> new PerlinNoise(new Random(seed));
    }

    @Override
    public double get(double x, double y, double z) {
        x = Noise.maintainPrecision(x + this.originX);
        y = Noise.maintainPrecision(y + this.originY);
        z = Noise.maintainPrecision(z + this.originZ);

        int ox = MathHelper.floor(x);
        int oy = MathHelper.floor(y);
        int oz = MathHelper.floor(z);

        double ix = x - ox;
        double iy = y - oy;
        double iz = z - oz;

        int prf = this.permute(ox) + oy;
        int prtf = this.permute(prf) + oz;
        int prbf = this.permute(prf + 1) + oz;
        int plf = this.permute(ox + 1) + oy;
        int pltf = this.permute(plf) + oz;
        int plbf = this.permute(plf + 1) + oz;

        double rtf = PerlinHelper.grad(this.permute(prtf), ix, iy, iz);
        double ltf = PerlinHelper.grad(this.permute(pltf), ix - 1.0, iy, iz);
        double rbf = PerlinHelper.grad(this.permute(prbf), ix, iy - 1.0, iz);
        double lbf = PerlinHelper.grad(this.permute(plbf), ix - 1.0, iy - 1.0, iz);
        double rtb = PerlinHelper.grad(this.permute(prtf + 1), ix, iy, iz - 1.0);
        double ltb = PerlinHelper.grad(this.permute(pltf + 1), ix - 1.0, iy, iz - 1.0);
        double rbb = PerlinHelper.grad(this.permute(prbf + 1), ix, iy - 1.0, iz - 1.0);
        double lbb = PerlinHelper.grad(this.permute(plbf + 1), ix - 1.0, iy - 1.0, iz - 1.0);

        return MathHelper.lerp3(
                MathHelper.perlinFade(ix),
                MathHelper.perlinFade(iy),
                MathHelper.perlinFade(iz),
                rtf, ltf, rbf, lbf,
                rtb, ltb, rbb, lbb
        );
    }

    private int permute(int x) {
        return this.permutations[x & 255] & 255;
    }

    @Override
    public NoiseRange getRange() {
        return RANGE;
    }
}
