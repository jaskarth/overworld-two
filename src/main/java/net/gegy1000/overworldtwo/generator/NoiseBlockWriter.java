package net.gegy1000.overworldtwo.generator;

import net.gegy1000.overworldtwo.mixin.ChunkSectionAccess;
import net.gegy1000.overworldtwo.mixin.PalettedContainerAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;

final class NoiseBlockWriter implements AutoCloseable {
    private final ProtoChunk chunk;
    private final BlockState state;

    private final boolean nonEmpty;
    private final boolean hasTicks;
    private final boolean hasFluid;
    private final boolean hasLight;

    private final HeightmapExt worldGenHeightmap;
    private final HeightmapExt oceanFloorHeightmap;

    private ChunkSection section;
    private Palette<BlockState> palette;
    private PackedIntegerArray data;

    private final int minSectionX;
    private int minSectionY;
    private final int minSectionZ;

    private int paletteValue = -1;
    private int count;

    public NoiseBlockWriter(ProtoChunk chunk, BlockState state) {
        this.chunk = chunk;
        this.state = state;

        this.worldGenHeightmap = (HeightmapExt) chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        if (Heightmap.Type.OCEAN_FLOOR_WG.getBlockPredicate().test(state)) {
            this.oceanFloorHeightmap = (HeightmapExt) chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        } else {
            this.oceanFloorHeightmap = null;
        }

        ChunkPos chunkPos = chunk.getPos();
        this.minSectionX = chunkPos.getStartX();
        this.minSectionZ = chunkPos.getStartZ();

        this.nonEmpty = !state.isAir();
        this.hasTicks = state.hasRandomTicks();
        this.hasFluid = !state.getFluidState().isEmpty();
        this.hasLight = state.getLuminance() != 0;
    }

    public void setSection(ChunkSection section) {
        this.closeSection();
        this.openSection(section);
    }

    public void set(int x, int y, int z) {
        if (this.paletteValue == -1) {
            this.paletteValue = this.palette.getIndex(this.state);
        }

        this.data.set(index(x, y, z), this.paletteValue);
        this.count++;

        this.trackHeightmapUpdate(x, y, z);

        if (this.hasLight) {
            this.chunk.addLightSource(new BlockPos(this.minSectionX + x, this.minSectionY + y, this.minSectionZ + z));
        }
    }

    private void trackHeightmapUpdate(int x, int y, int z) {
        int worldY = y + this.minSectionY;
        this.worldGenHeightmap.trackUpdate(x, worldY, z);

        HeightmapExt oceanFloorHeightmap = this.oceanFloorHeightmap;
        if (oceanFloorHeightmap != null) {
            oceanFloorHeightmap.trackUpdate(x, worldY, z);
        }
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    @Override
    public void close() {
        this.closeSection();
    }

    @SuppressWarnings("unchecked")
    private void openSection(ChunkSection section) {
        this.section = section;

        this.minSectionY = section.getYOffset();

        PalettedContainer<BlockState> container = section.getContainer();
        PalettedContainerAccess<BlockState> containerAccess = (PalettedContainerAccess<BlockState>) container;

        this.data = containerAccess.getData();
        this.palette = containerAccess.getPalette();

        this.paletteValue = -1;
        this.count = 0;
    }

    private void closeSection() {
        ChunkSection section = this.section;
        if (section == null) {
            return;
        }

        ChunkSectionAccess sectionAccess = (ChunkSectionAccess) section;

        if (this.nonEmpty) {
            short nonEmptyBlockCount = (short) (sectionAccess.getNonEmptyBlockCount() + this.count);
            sectionAccess.setNonEmptyBlockCount(nonEmptyBlockCount);

            if (this.hasTicks) {
                short randomTickableBlockCount = (short) (sectionAccess.getRandomTickableBlockCount() + this.count);
                sectionAccess.setRandomTickableBlockCount(randomTickableBlockCount);
            }
        }

        if (this.hasFluid) {
            short nonEmptyFluidCount = (short) (sectionAccess.getNonEmptyFluidCount() + this.count);
            sectionAccess.setNonEmptyFluidCount(nonEmptyFluidCount);
        }
    }
}
