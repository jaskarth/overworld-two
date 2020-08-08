package net.gegy1000.overworldtwo.util;

import net.minecraft.block.BlockState;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.util.Random;
import java.util.function.Predicate;

public final class BlockBrush {
    public final BlockState block;
    public final RuleTest replace;
    public final int flags;

    public BlockBrush(BlockState block, RuleTest replace, int flags) {
        this.block = block;
        this.replace = replace;
        this.flags = flags;
    }

    public static BlockBrush of(BlockState block) {
        return new BlockBrush(block, null, 0b10);
    }

    public static BlockBrush ofWhere(BlockState block, RuleTest replace) {
        return new BlockBrush(block, replace, 0b10);
    }

    public boolean test(WorldAccess world, Random random, BlockPos pos) {
        return this.replace == null || this.replace.test(world.getBlockState(pos), random);
    }
}
