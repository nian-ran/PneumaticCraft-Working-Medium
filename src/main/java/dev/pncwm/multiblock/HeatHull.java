package dev.pncwm.multiblock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Faces of a formed shell that look outward, used to attach heat pipes on casings and parts.
 * Coordinates are relative integers so the rules can be tested without a running game.
 */
public final class HeatHull {
    public static final int[][] DIRECTIONS = {
            {0, -1, 0},
            {0, 1, 0},
            {0, 0, -1},
            {0, 0, 1},
            {-1, 0, 0},
            {1, 0, 0}
    };

    private HeatHull() {
    }

    public record Cell(int x, int y, int z) {
        public Cell offset(int dx, int dy, int dz) {
            return new Cell(x + dx, y + dy, z + dz);
        }
    }

    /** One hull block and a unit offset toward a neighbour that is not itself part of the hull. */
    public record Probe(Cell cell, int dx, int dy, int dz) {
        public Cell neighbour() {
            return cell.offset(dx, dy, dz);
        }
    }

    public static List<Probe> externalProbes(Collection<Cell> hull) {
        Set<Cell> set = new HashSet<>(hull);
        List<Probe> probes = new ArrayList<>();
        for (Cell cell : set) {
            for (int[] dir : DIRECTIONS) {
                if (!set.contains(cell.offset(dir[0], dir[1], dir[2]))) {
                    probes.add(new Probe(cell, dir[0], dir[1], dir[2]));
                }
            }
        }
        return probes;
    }
}
