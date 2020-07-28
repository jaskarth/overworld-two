package net.gegy1000.overworldtwo;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldAccess;

import java.util.BitSet;

public final class BlockCanvas implements AutoCloseable {
    private static final BlockBrush NO_BRUSH = new BlockBrush(Blocks.AIR.getDefaultState(), s -> false, 0);
    private static final boolean DEBUG = true;

    private WorldAccess world;

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private BitSet mask;

    private BlockBrush brush;

    private final BlockPos.Mutable mutable = new BlockPos.Mutable();

    public BlockCanvas(WorldAccess world, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, BitSet mask, BlockBrush brush) {
        this.world = world;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.mask = mask;
        this.brush = brush;
    }

    public static BlockCanvas open(WorldAccess world, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        return new BlockCanvas(world, minX, minY, minZ, sizeX, sizeY, sizeZ, new BitSet(sizeX * sizeY * sizeZ), NO_BRUSH);
    }

    public void setBrush(BlockBrush brush) {
        this.brush = brush;
    }

    public boolean setLocal(int x, int y, int z) {
        int idx = this.index(x, y, z);

        if (!this.mask.get(idx)) {
            BlockPos pos = this.pos(x, y, z);
            if (this.brush.test(this.world, pos)) {
                this.mask.set(idx);
                this.world.setBlockState(pos, this.brush.block, this.brush.flags);
                return true;
            }
        }

        return false;
    }

    public int drawSphere(double originX, double originY, double originZ, double radius) {
        int count = 0;

        originX -= this.minX;
        originY -= this.minY;
        originZ -= this.minZ;

        int x1 = Math.max(MathHelper.floor(originX - radius), 0);
        int y1 = Math.max(MathHelper.floor(originY - radius), 0);
        int z1 = Math.max(MathHelper.floor(originZ - radius), 0);
        int x2 = Math.max(MathHelper.floor(originX + radius), x1);
        int y2 = Math.max(MathHelper.floor(originY + radius), y1);
        int z2 = Math.max(MathHelper.floor(originZ + radius), z1);

        double radius2 = radius * radius;

        for (int x = x1; x <= x2; x++) {
            double dx = x + 0.5 - originX;
            if (dx >= radius) continue;

            for (int y = y1; y <= y2; y++) {
                double dy = y + 0.5 - originY;
                if (dx * dx + dy * dy >= radius2) continue;

                for (int z = z1; z <= z2; z++) {
                    double dz = z + 0.5 - originZ;
                    if (dx * dx + dy * dy + dz * dz >= radius2) continue;

                    if (this.setLocal(x, y, z)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private BlockPos pos(int x, int y, int z) {
        return this.mutable.set(x + this.minX, y + this.minY, z + this.minZ);
    }

    private int index(int x, int y, int z) {
        if (DEBUG) {
            if (x < 0 || y < 0 || z < 0 || x >= this.sizeX || y >= this.sizeY || z >= this.sizeZ) {
                throw new IllegalArgumentException("pos out of bounds");
            }
        }
        return (x * this.sizeY + y) * this.sizeZ + z;
    }

    @Override
    public void close() {
        if (this.world == null) {
            throw new IllegalStateException("canvas not open!");
        }
        this.world = null;
        this.mask = null;
        this.brush = NO_BRUSH;
    }
}
