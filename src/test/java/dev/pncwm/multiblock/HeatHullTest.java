package dev.pncwm.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.pncwm.multiblock.HeatHull.Cell;
import dev.pncwm.multiblock.HeatHull.Probe;

class HeatHullTest {
    @Test
    void singleBlockHasSixExternalFaces() {
        Set<Cell> hull = Set.of(new Cell(0, 0, 0));
        List<Probe> probes = HeatHull.externalProbes(hull);
        assertEquals(6, probes.size());
        Set<String> seen = new HashSet<>();
        for (Probe probe : probes) {
            assertEquals(new Cell(0, 0, 0), probe.cell());
            assertTrue(seen.add(probe.dx() + "," + probe.dy() + "," + probe.dz()));
            assertFalse(hull.contains(probe.neighbour()));
        }
    }

    @Test
    void sharedFaceBetweenTwoHullBlocksIsNotProbed() {
        Set<Cell> hull = Set.of(new Cell(0, 0, 0), new Cell(1, 0, 0));
        List<Probe> probes = HeatHull.externalProbes(hull);
        // Two cubes share one face, so 12 - 2 = 10 outward faces.
        assertEquals(10, probes.size());
        for (Probe probe : probes) {
            assertFalse(hull.contains(probe.neighbour()), probe.neighbour().toString());
        }
        assertFalse(contains(probes, new Cell(0, 0, 0), 1, 0, 0));
        assertFalse(contains(probes, new Cell(1, 0, 0), -1, 0, 0));
        assertTrue(contains(probes, new Cell(0, 0, 0), -1, 0, 0));
        assertTrue(contains(probes, new Cell(1, 0, 0), 1, 0, 0));
    }

    @Test
    void interiorCellOfAShellHasNoProbes() {
        Set<Cell> hull = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    hull.add(new Cell(x, y, z));
                }
            }
        }
        List<Probe> probes = HeatHull.externalProbes(hull);
        for (Probe probe : probes) {
            assertFalse(probe.cell().equals(new Cell(0, 0, 0)), "interior cell leaked a probe");
            assertFalse(hull.contains(probe.neighbour()));
        }
        // 3x3x3 cube: 6 faces * 9 cells = 54 outward faces.
        assertEquals(54, probes.size());
    }

    private static boolean contains(List<Probe> probes, Cell cell, int dx, int dy, int dz) {
        for (Probe probe : probes) {
            if (probe.cell().equals(cell) && probe.dx() == dx && probe.dy() == dy && probe.dz() == dz) {
                return true;
            }
        }
        return false;
    }
}
