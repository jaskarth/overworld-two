package net.gegy1000.overworldtwo.mixin;

import net.gegy1000.overworldtwo.util.BlockBrush;
import net.gegy1000.overworldtwo.util.BlockCanvas;
import net.gegy1000.overworldtwo.generator.OverworldTwoChunkGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(OreFeature.class)
public class MixinOreFeature {
    private static final float PI = (float) Math.PI;
    private static final int MAX_SLOPE = 4;

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    public void generate(
            StructureWorldAccess world, ChunkGenerator generator,
            Random random, BlockPos origin, OreFeatureConfig config,
            CallbackInfoReturnable<Boolean> ci
    ) {
        if (generator instanceof OverworldTwoChunkGenerator) {
            ci.setReturnValue(this.generate(world, random, origin, config));
        }
    }

    private boolean generate(ServerWorldAccess world, Random random, BlockPos origin, OreFeatureConfig config) {
        float theta = random.nextFloat() * PI;

        int nodeRadius = MathHelper.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        float radius = config.size / 8.0F;

        double x1 = origin.getX() + MathHelper.sin(theta) * radius;
        double x2 = origin.getX() - MathHelper.sin(theta) * radius;
        double z1 = origin.getZ() + MathHelper.cos(theta) * radius;
        double z2 = origin.getZ() - MathHelper.cos(theta) * radius;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        double y2 = origin.getY() + random.nextInt(3) - 2;

        int minX = origin.getX() - MathHelper.ceil(radius) - nodeRadius;
        int minY = origin.getY() - 2 - nodeRadius;
        int minZ = origin.getZ() - MathHelper.ceil(radius) - nodeRadius;

        int width = 2 * (MathHelper.ceil(radius) + nodeRadius);
        int height = 2 * (2 + nodeRadius);

        int surfaceY = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, minX, minZ);
        if (minY <= surfaceY) {
            return this.tryGenerate(world, random, config, x1, x2, z1, z2, y1, y2, minX, minY, minZ, width, height);
        }

        // optimization: we assume the terrain can have a maximum gradient of 4:1 blocks
        // if the distance between the surface and here exceeds that, we probably can't generate
        if (minY - surfaceY > MAX_SLOPE * width) {
            return false;
        }

        for (int z = minZ; z <= minZ + width; z += 2) {
            for (int x = minX; x <= minX + width; x += 2) {
                if (minY <= world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z)) {
                    return this.tryGenerate(world, random, config, x1, x2, z1, z2, y1, y2, minX, minY, minZ, width, height);
                }
            }
        }

        return false;
    }

    protected boolean tryGenerate(
            WorldAccess world, Random random, OreFeatureConfig config,
            double x1, double x2, double z1,
            double z2, double y1, double y2,
            int minX, int minY, int minZ,
            int width, int height
    ) {
        try (BlockCanvas canvas = BlockCanvas.open(world, minX, minY, minZ, width, height, width)) {
            canvas.setBrush(BlockBrush.ofWhere(config.state, config.target));

            int blockCount = 0;

            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double pos = 0.0;

            for (int i = 0; i < config.size; i++) {
                double radius = this.nextRadius(random, pos / length, config.size);

                // offset first sphere to have its origin at (x1; y1; z1)
                if (i == 0) pos -= radius;

                double center = pos + radius;
                if (center >= length) break;

                double delta = center / length;
                double x = MathHelper.lerp(delta, x1, x2);
                double y = MathHelper.lerp(delta, y1, y2);
                double z = MathHelper.lerp(delta, z1, z2);

                blockCount += canvas.drawSphere(random, x, y, z, radius);

                pos += radius;
            }

            return blockCount > 0;
        }
    }

    private double nextRadius(Random random, double delta, int size) {
        float theta = (float) (PI * delta);
        double maxRadius = random.nextDouble() * size / 16.0;
        double sin = (MathHelper.sin(theta) + 1.0) / 2.0;
        return (sin * maxRadius) + 0.5;
    }
}
