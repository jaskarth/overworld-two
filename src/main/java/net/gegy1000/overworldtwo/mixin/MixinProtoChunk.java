package net.gegy1000.overworldtwo.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk {
    @Shadow
    @Final
    private ChunkSection[] sections;
    @Shadow
    @Final
    private List<BlockPos> lightSources;
    @Shadow
    private volatile ChunkStatus status;
    @Shadow
    @Final
    private ChunkPos pos;
    @Shadow
    private volatile LightingProvider lightingProvider;
    @Shadow
    @Final
    private Map<Heightmap.Type, Heightmap> heightmaps;

    @Shadow
    public abstract ChunkSection getSection(int y);

    private boolean statusDirty;
    private boolean shouldCheckLight;

    /**
     * @reason optimize and simplify set logic
     * @author gegy1000
     */
    @Nullable
    @Overwrite
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        int y = pos.getY();
        if (y < 0 || y >= 256) {
            return Blocks.VOID_AIR.getDefaultState();
        }

        if (this.sections[y >> 4] == WorldChunk.EMPTY_SECTION && state.isOf(Blocks.AIR)) {
            return state;
        }

        int x = pos.getX();
        int z = pos.getZ();
        int localX = x & 15;
        int localZ = z & 15;

        ChunkSection section = this.getSection(y >> 4);
        BlockState oldState = section.setBlockState(localX, y & 15, localZ, state);

        if (this.statusDirty) {
            this.populateMissingHeightmaps();
        }

        if (state != oldState) {
            if (state.getLuminance() > 0) {
                BlockPos globalPos = new BlockPos(
                        localX + this.pos.getStartX(),
                        y,
                        localZ + this.pos.getStartZ()
                );
                this.lightSources.add(globalPos);
            }

            if (this.shouldCheckLight && this.isLightingDifferent(oldState, state, pos)) {
                this.lightingProvider.checkBlock(pos);
            }

            for (Heightmap heightmap : this.heightmaps.values()) {
                heightmap.trackUpdate(localX, y, localZ, state);
            }
        }

        return oldState;
    }

    private boolean isLightingDifferent(BlockState from, BlockState to, BlockPos pos) {
        return to.getOpacity(this.self(), pos) != from.getOpacity(this.self(), pos)
                || to.getLuminance() != from.getLuminance()
                || to.hasSidedTransparency() || from.hasSidedTransparency();
    }

    private void populateMissingHeightmaps() {
        EnumSet<Heightmap.Type> missingHeightmaps = null;
        for (Heightmap.Type type : this.status.getHeightmapTypes()) {
            if (!this.heightmaps.containsKey(type)) {
                if (missingHeightmaps == null) {
                    missingHeightmaps = EnumSet.noneOf(Heightmap.Type.class);
                }
                missingHeightmaps.add(type);
            }
        }

        if (missingHeightmaps != null) {
            Heightmap.populateHeightmaps(this.self(), missingHeightmaps);
        }
    }

    @Inject(method = "setStatus", at = @At("HEAD"))
    private void setStatus(ChunkStatus status, CallbackInfo ci) {
        this.statusDirty = true;
        this.shouldCheckLight = status.isAtLeast(ChunkStatus.FEATURES);
    }

    private ProtoChunk self() {
        return (ProtoChunk) (Object) this;
    }
}
