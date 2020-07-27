package net.gegy1000.overworldtwo.mixin;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.NetherrackReplaceBlobsFeature;
import net.minecraft.world.gen.feature.NetherrackReplaceBlobsFeatureConfig;

@Mixin(NetherrackReplaceBlobsFeature.class)
public abstract class MixinNetherrackReplaceBlobsFeature {
	private static final Long2ObjectOpenHashMap<List<BlockPos>> DELTA_CACHE = new Long2ObjectOpenHashMap<>();

	static {
		DELTA_CACHE.defaultReturnValue(null);
	}

	// Finds start position
	@Shadow
	protected static BlockPos method_27107(WorldAccess world, BlockPos.Mutable mutable, Block block) {
		return null;
	}

	// Generates the size of this blob
	@Shadow
	protected static Vec3i method_27108(Random random, NetherrackReplaceBlobsFeatureConfig config) {
		return null;
	}

	/**
	 * Faster implementation
	 * @author SuperCoder79
	 */
	@Overwrite
	public boolean generate(ServerWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, NetherrackReplaceBlobsFeatureConfig config) {
		Block targetBlock = config.target.getBlock();

		// Get start position
		BlockPos startPos = method_27107(world, blockPos.mutableCopy().method_27158(Direction.Axis.Y, 1, world.getHeight() - 1), targetBlock);

		if (startPos == null) {
			return false;
		} else {
			// If the start position is not null, then generate the blob.

			// Generate size and max entent
			Vec3i size = method_27108(random, config);
			int maxExtent = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
			boolean didPlace = false;

			// Get the delta positions
			List<BlockPos> deltas = getDeltas(size);

			// Use a mutable to reduce blockpos creation
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			// Slight deviation from vanilla code here, uses a for loop instead of a while-iterator loop
			for (BlockPos delta : deltas) {
				// Add the delta to the start position
				mutable.set(startPos, delta.getX(), delta.getY(), delta.getZ());

				// If the distance is greater than the max extent, break
				if (mutable.getManhattanDistance(startPos) > maxExtent) {
					break;
				}

				// If the block here is the target, replace with the state
				BlockState hereState = world.getBlockState(mutable);
				if (hereState.isOf(targetBlock)) {
					world.setBlockState(mutable, config.state, 3);
					didPlace = true;
				}
			}

			return didPlace;
		}
	}

	// Gets the positions that would be iterated through this
	private static List<BlockPos> getDeltas(Vec3i size) {
		long packed = BlockPos.asLong(size.getX(), size.getY(), size.getZ());
		List<BlockPos> deltas = DELTA_CACHE.get(packed);

		// Cache hit, return
		if (deltas != null) {
			return deltas;
		}

		// Cache miss, compute and store

		// Do note that this list is full of mutables so be very wary of modifying the positions.
		deltas = ImmutableList.copyOf(BlockPos.iterateOutwards(new BlockPos(0, 0, 0), size.getX(), size.getY(), size.getZ()));
		DELTA_CACHE.put(packed, deltas);

		return deltas;

	}
}
