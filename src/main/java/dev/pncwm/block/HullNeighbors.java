package dev.pncwm.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import dev.pncwm.blockentity.ApparatusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * When a heat pipe (or any other neighbour) is placed against a casing, part or hatch, the bound
 * controller must rebuild its thermal hull. Casings and parts have no block entity, so we walk the
 * connected shell until we find one or more controllers.
 */
public final class HullNeighbors {
    /** Larger than any current formed shell so a complete walk cannot be cut short. */
    public static final int MAX_WALK = 2048;

    private HullNeighbors() {
    }

    public static boolean isShellBlock(Block block) {
        return block instanceof CasingBlock
                || block instanceof HatchBlock
                || block instanceof PartBlock
                || block instanceof ApparatusBlock;
    }

    public static void notifyControllers(Level level, BlockPos origin) {
        if (level.isClientSide) {
            return;
        }
        BlockState start = level.getBlockState(origin);
        if (!isShellBlock(start.getBlock())) {
            return;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin.immutable());
        seen.add(origin.immutable());
        int visited = 0;
        while (!queue.isEmpty() && visited < MAX_WALK) {
            BlockPos pos = queue.removeFirst();
            visited++;
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof ApparatusBlock && level.getBlockEntity(pos) instanceof ApparatusBlockEntity apparatus) {
                apparatus.neighborChanged();
            }
            if (!isShellBlock(block)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && isShellBlock(level.getBlockState(next).getBlock())) {
                    queue.add(next);
                }
            }
        }
    }
}
