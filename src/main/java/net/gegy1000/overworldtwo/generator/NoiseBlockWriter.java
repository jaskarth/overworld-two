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
    private final BlockState state;
    private final Heightmap.Type heightmapType;

    private final boolean nonEmpty;
    private final boolean hasTicks;
    private final boolean hasFluid;
    private final boolean hasLight;

    private ProtoChunk chunk;
    private HeightmapExt heightmap;
    private ChunkSection section;
    private Palette<BlockState> palette;
    private PackedIntegerArray data;

    private int minSectionX;
    private int minSectionY;
    private int minSectionZ;

    private int paletteValue = -1;
    private int count;

    public NoiseBlockWriter(BlockState state, Heightmap.Type heightmapType) {
        this.state = state;
        this.heightmapType = heightmapType;

        this.nonEmpty = !state.isAir();
        this.hasTicks = state.hasRandomTicks();
        this.hasFluid = !state.getFluidState().isEmpty();
        this.hasLight = state.getLuminance() != 0;
    }

    public void setSection(ProtoChunk chunk, ChunkSection section) {
        this.closeSection();
        this.openSection(chunk, section);
    }

    public void set(int x, int y, int z) {
        if (this.paletteValue == -1) {
            this.paletteValue = this.palette.getIndex(this.state);
        }

        this.data.set(index(x, y, z), this.paletteValue);
        this.count++;

        this.heightmap.trackUpdate(x, y + this.minSectionY, z);

        if (this.hasLight) {
            this.chunk.addLightSource(new BlockPos(this.minSectionX + x, this.minSectionY + y, this.minSectionZ + z));
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
    private void openSection(ProtoChunk chunk, ChunkSection section) {
        this.chunk = chunk;
        this.section = section;
        this.heightmap = (HeightmapExt) chunk.getHeightmap(this.heightmapType);

        ChunkPos chunkPos = chunk.getPos();
        this.minSectionX = chunkPos.getStartX();
        this.minSectionY = section.getYOffset();
        this.minSectionZ = chunkPos.getStartZ();

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
