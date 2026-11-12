package dev.pncwm.client.model;

import net.minecraft.core.Direction;

/**
 * Maps a cube face to the four texture-space edges used by {@link CtmIndex}.
 * Mixed-grade casings do not connect; only the same grade casing does.
 */
public final class CtmFaces {
    private CtmFaces() {
    }

    /**
     * Edges of a face in texture space: up, right, down, left, when looking at the face from outside.
     */
    public static Direction[] edges(Direction face) {
        return switch (face) {
            case UP -> new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            case DOWN -> new Direction[] {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
            case NORTH -> new Direction[] {Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST};
            case SOUTH -> new Direction[] {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};
            case WEST -> new Direction[] {Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
            case EAST -> new Direction[] {Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH};
        };
    }
}
