package net.gegy1000.overworldtwo.mixin;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(NoiseChunkGenerator.class)
public class MixinNoiseChunkGenerator {
    @Redirect(
            method = "populateNoise",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/gen/feature/StructureFeature;field_24861:Ljava/util/List;"
            )
    )
    private List<StructureFeature<?>> disableStructureIterator(WorldAccess world, StructureAccessor structures, Chunk chunk) {
        return Collections.emptyList();
    }

    @Inject(
            method = "populateNoise",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;getPos()Lnet/minecraft/util/math/ChunkPos;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void collectStructures(
            WorldAccess world, StructureAccessor structures, Chunk chunk,
            CallbackInfo ci, ObjectList<StructurePiece> pieces, ObjectList<JigsawJunction> junctions
    ) {
        ChunkPos chunkPos = chunk.getPos();

        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        int maxX = chunkPos.getEndX();
        int maxZ = chunkPos.getEndZ();

        Map<StructureFeature<?>, LongSet> structureReferences = chunk.getStructureReferences();
        if (structureReferences.isEmpty()) {
            return;
        }

        for (StructureFeature<?> feature : StructureFeature.field_24861) {
            LongSet references = structureReferences.get(feature);
            if (references == null) continue;

            LongIterator referenceIterator = references.iterator();

            while (referenceIterator.hasNext()) {
                long packedReference = referenceIterator.nextLong();
                int referenceX = ChunkPos.getPackedX(packedReference);
                int referenceZ = ChunkPos.getPackedZ(packedReference);

                Chunk referenceChunk = world.getChunk(referenceX, referenceZ, ChunkStatus.STRUCTURE_STARTS);
                StructureStart<?> start = referenceChunk.getStructureStart(feature);
                if (start == null || !start.hasChildren()) {
                    continue;
                }

                for (StructurePiece piece : start.getChildren()) {
                    int radius = 12;
                    if (!piece.intersectsChunk(chunkPos, radius)) {
                        continue;
                    }

                    if (piece instanceof PoolStructurePiece) {
                        PoolStructurePiece pooledPiece = (PoolStructurePiece) piece;
                        StructurePool.Projection projection = pooledPiece.getPoolElement().getProjection();
                        if (projection == StructurePool.Projection.RIGID) {
                            pieces.add(pooledPiece);
                        }

                        for (JigsawJunction junction : pooledPiece.getJunctions()) {
                            int junctionX = junction.getSourceX();
                            int junctionZ = junction.getSourceZ();
                            if (junctionX > minX - radius && junctionZ > minZ - radius && junctionX < maxX + radius && junctionZ < maxZ + radius) {
                                junctions.add(junction);
                            }
                        }
                    } else {
                        pieces.add(piece);
                    }
                }
            }
        }
    }
}
