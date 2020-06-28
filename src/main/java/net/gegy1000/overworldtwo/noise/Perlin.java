package net.gegy1000.overworldtwo.noise;

import net.minecraft.util.math.noise.SimplexNoiseSampler;

import java.util.Random;

public final class Perlin {
    public static int[] initPermutationTable(Random random) {
        int[] permutations = new int[256];
        for (int i = 0; i < 256; ++i) {
            permutations[i] = i;
        }

        for (int i = 0; i < 256; ++i) {
            int offset = random.nextInt(256 - i);
            int swap = permutations[i];
            permutations[i] = permutations[i + offset];
            permutations[i + offset] = swap;
        }

        return permutations;
    }

    public static double grad(int hash, double x, double y, double z) {
        int[] gradient = SimplexNoiseSampler.gradients[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }
}
