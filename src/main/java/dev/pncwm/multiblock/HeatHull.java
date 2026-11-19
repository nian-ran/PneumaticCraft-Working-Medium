package dev.pncwm.multiblock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Faces of a formed shell that look outward, used to attach heat pipes and other heat sources
 * on casings, hatches and parts. Coordinates are relative integers so the rules can be tested
 * without a running game.
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

    /** A unit offset from a hull cell toward a neighbour. */
    public record Face(int dx, int dy, int dz) {
    }

    /** One hull block and a unit offset toward a neighbour that is not itself part of the hull. */
    public record Probe(Cell cell, int dx, int dy, int dz) {
        public Cell neighbour() {
            return cell.offset(dx, dy, dz);
        }

        public Face face() {
            return new Face(dx, dy, dz);
        }
    }

    /** All outward faces of one hull cell, so a single auxiliary exchanger can cover that block. */
    public record CellFaces(Cell cell, List<Face> faces) {
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

    /**
     * Outward faces that should join the thermal hull (heat pipes, lava, ice, furnaces).
     * {@code blocked} is typically the air-isolated back face of a compressor/expander column
     * or a regulator, which must not conduct heat either.
     */
    public static List<Probe> conductingProbes(Collection<Cell> hull, Probe blocked) {
        List<Probe> probes = externalProbes(hull);
        if (blocked == null) {
            return probes;
        }
        List<Probe> kept = new ArrayList<>(probes.size());
        for (Probe probe : probes) {
            if (!probe.equals(blocked)) {
                kept.add(probe);
            }
        }
        return kept;
    }

    /**
     * Faces that need an auxiliary {@code initializeAsHull} because they are not already
     * scanned on the controller itself. Controller air faces are covered by the main exchanger;
     * the blocked (isolated) face is excluded entirely.
     */
    public static List<Probe> auxiliaryBehaviourProbes(Collection<Cell> hull, Cell controller,
            Collection<Face> controllerScannedFaces, Probe blocked) {
        Set<Face> scanned = controllerScannedFaces == null ? Set.of() : new HashSet<>(controllerScannedFaces);
        List<Probe> probes = new ArrayList<>();
        for (Probe probe : conductingProbes(hull, blocked)) {
            if (probe.cell().equals(controller) && scanned.contains(probe.face())) {
                continue;
            }
            probes.add(probe);
        }
        return probes;
    }

    /** Group auxiliary probes by hull cell so one exchanger can cover all of a block's outward faces. */
    public static List<CellFaces> groupByCell(Collection<Probe> probes) {
        Map<Cell, List<Face>> grouped = new LinkedHashMap<>();
        for (Probe probe : probes) {
            grouped.computeIfAbsent(probe.cell(), cell -> new ArrayList<>()).add(probe.face());
        }
        List<CellFaces> cells = new ArrayList<>();
        for (Map.Entry<Cell, List<Face>> entry : grouped.entrySet()) {
            cells.add(new CellFaces(entry.getKey(), List.copyOf(entry.getValue())));
        }
        return cells;
    }
}
