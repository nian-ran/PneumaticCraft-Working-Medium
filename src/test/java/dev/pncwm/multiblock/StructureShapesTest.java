package dev.pncwm.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.pncwm.apparatus.Family;
import dev.pncwm.multiblock.StructureShapes.Cell;
import dev.pncwm.multiblock.StructureShapes.Layout;
import dev.pncwm.multiblock.StructureShapes.Role;

class StructureShapesTest {
    @Test
    void everyFamilyBuildsASingleControllerAndNoOverlappingCells() {
        for (Family family : Family.values()) {
            int min = switch (family) {
                case TOWER, HEAT_LINE -> 6;
                case FLARE -> 8;
                case QUENCH, COLUMN -> 4;
                case GALLERY, MANIFOLD -> 6;
                case KETTLE -> 3;
                case ROOM, STORAGE, LOOP -> 5;
                default -> 1;
            };
            Layout layout = StructureShapes.minimum(family.name().toLowerCase(), family, min, min + 2);
            Map<Role, Integer> counts = layout.counts();
            assertEquals(1, counts.getOrDefault(Role.CONTROLLER, 0), family.name());
            Set<String> seen = new HashSet<>();
            for (Cell cell : layout.cells()) {
                String key = cell.x() + "," + cell.y() + "," + cell.z();
                assertTrue(seen.add(key), family.name() + " overlap at " + key);
            }
            int width = layout.maxX() - layout.minX() + 1;
            int height = layout.maxY() - layout.minY() + 1;
            int depth = layout.maxZ() - layout.minZ() + 1;
            int longest = Math.max(width, Math.max(height, depth));
            assertTrue(longest >= 5, family.name() + " too small: " + width + "x" + height + "x" + depth);
        }
    }

    @Test
    void chamberIsAHollow5x5x5AndCycloneIsTaller() {
        Layout layout = StructureShapes.minimum("impact_crusher", Family.CHAMBER, 1, 1);
        assertEquals(new Cell(0, 2, 0, Role.CONTROLLER), layout.controller());
        assertEquals(97, layout.counts().get(Role.CASING));
        assertEquals(27, layout.counts().get(Role.AIR));
        assertTrue(layout.cells().contains(new Cell(0, 2, 2, Role.AIR)));
        assertFalse(layout.scalable());
        assertEquals(0, layout.minY());
        assertEquals(4, layout.maxY());
        assertEquals(4, layout.maxZ());
        assertEquals(2, layout.maxX());

        Layout cyclone = StructureShapes.minimum("cyclone", Family.CHAMBER, 1, 1);
        assertEquals(new Cell(0, 3, 0, Role.CONTROLLER), cyclone.controller());
        assertEquals(129, cyclone.counts().get(Role.CASING));
        assertEquals(45, cyclone.counts().get(Role.AIR));
        assertEquals(6, cyclone.maxY());
        assertTrue(cyclone.maxY() > layout.maxY());
    }

    @Test
    void towerHasAWideBaseATallShaftAndACap() {
        Layout layout = StructureShapes.minimum("washer_tower", Family.TOWER, 6, 10);
        Map<Role, Integer> counts = layout.counts();
        assertEquals(6, counts.get(Role.TRAY));
        assertEquals(1, counts.get(Role.CAP));
        assertEquals(80, counts.get(Role.CASING));
        assertEquals(new Cell(0, 0, 0, Role.CONTROLLER), layout.controller());
        assertTrue(layout.cells().contains(new Cell(0, 1, 2, Role.TRAY)));
        assertTrue(layout.cells().contains(new Cell(0, 7, 2, Role.CAP)));
        assertEquals(4, layout.maxX() - layout.minX()); // 5-wide base
        assertEquals(7, layout.maxY());
        assertTrue(layout.scalable());
    }

    @Test
    void columnIsAVerticalJacketedStack() {
        Layout layout = StructureShapes.minimum("compressor_column", Family.COLUMN, 4, 8);
        Map<Role, Integer> counts = layout.counts();
        assertEquals(4, counts.get(Role.STAGE));
        assertEquals(3, counts.get(Role.INTERCOOLER));
        assertEquals(1, counts.get(Role.CAP));
        assertEquals(72, counts.get(Role.CASING));
        assertTrue(layout.cells().contains(new Cell(0, 1, 1, Role.STAGE)));
        assertTrue(layout.cells().contains(new Cell(0, 2, 1, Role.INTERCOOLER)));
        assertTrue(layout.cells().contains(new Cell(0, 8, 1, Role.CAP)));
        assertEquals(8, layout.maxY());
        assertEquals(2, layout.maxX() - layout.minX()); // 3-wide jacket
    }

    @Test
    void kettleIsAWideVesselAndGalleryIsATunnel() {
        Layout kettle = StructureShapes.minimum("lube_mixer", Family.KETTLE, 3, 6);
        assertEquals(3, kettle.counts().get(Role.BODY));
        assertEquals(96, kettle.counts().get(Role.CASING));
        assertEquals(24, kettle.counts().get(Role.AIR));
        assertEquals(1, kettle.counts().get(Role.CAP));
        assertFalse(kettle.counts().containsKey(Role.COIL));
        assertEquals(4, kettle.maxX() - kettle.minX()); // 5-wide
        assertEquals(4, kettle.maxY());
        assertTrue(kettle.cells().contains(new Cell(0, 1, 2, Role.BODY)));
        assertTrue(kettle.scalable());

        Layout gallery = StructureShapes.minimum("cracker", Family.GALLERY, 6, 10);
        assertEquals(6, gallery.counts().get(Role.BAY));
        assertEquals(64, gallery.counts().get(Role.CASING));
        assertEquals(1, gallery.counts().get(Role.CAP));
        assertEquals(7, gallery.maxZ());
        assertEquals(2, gallery.maxY());
        assertTrue(gallery.cells().contains(new Cell(0, 1, 1, Role.BAY)));
        assertTrue(gallery.cells().contains(new Cell(0, 1, 7, Role.CAP)));
    }

    @Test
    void loopRoomFlareQuenchRegulatorAndPadAreDistinctAndLarge() {
        Layout storage = StructureShapes.minimum("gas_bank", Family.STORAGE, 5, 7);
        assertEquals(124, storage.counts().get(Role.CASING));
        assertEquals(5, storage.maxY() - storage.minY() + 1);

        Layout room = StructureShapes.minimum("clean_room", Family.ROOM, 5, 7);
        assertEquals(217, room.counts().get(Role.CASING));
        assertEquals(125, room.counts().get(Role.AIR));
        assertEquals(new Cell(0, 1, 0, Role.CONTROLLER), room.controller());
        assertEquals(6, room.maxY());
        assertEquals(6, room.maxZ());

        Layout flare = StructureShapes.minimum("flare_stack", Family.FLARE, 8, 16);
        assertEquals(25, flare.counts().get(Role.CASING));
        assertEquals(9, flare.maxY());
        assertTrue(flare.cells().contains(new Cell(0, 9, 1, Role.CASING)));
        assertEquals(2, flare.maxX() - flare.minX()); // 3-wide base

        Layout quench = StructureShapes.minimum("quench_well", Family.QUENCH, 4, 8);
        assertEquals(4, quench.counts().get(Role.BODY));
        assertEquals(48, quench.counts().get(Role.CASING));
        assertTrue(quench.cells().contains(new Cell(0, 5, 1, Role.CAP)));
        assertEquals(2, quench.maxX() - quench.minX()); // 3-wide well

        Layout regulator = StructureShapes.minimum("regulator_skid", Family.REGULATOR, 1, 1);
        assertEquals(74, regulator.counts().get(Role.CASING));
        assertFalse(regulator.counts().containsKey(Role.AIR));
        assertEquals(2, regulator.maxY());
        assertEquals(4, regulator.maxZ());

        Layout pad = StructureShapes.minimum("calibration_bench", Family.CALIBRATION, 1, 1);
        assertEquals(34, pad.counts().get(Role.CASING));
        assertEquals(2, pad.maxY());
        assertTrue(pad.cells().contains(new Cell(0, 2, 2, Role.CASING)));

        Layout loop = StructureShapes.minimum("fluid_loop", Family.LOOP, 5, 7);
        assertEquals(15, loop.counts().get(Role.BODY));
        assertEquals(1, loop.counts().get(Role.COIL));
        assertTrue(loop.cells().contains(new Cell(2, 1, 0, Role.COIL)));

        Layout cycle = StructureShapes.minimum("cycle_island", Family.CYCLE, 1, 1);
        assertEquals(31, cycle.counts().get(Role.CASING));
        assertEquals(18, cycle.counts().get(Role.AIR));
        assertEquals(1, cycle.maxY());

        Layout charge = StructureShapes.minimum("charging_station", Family.CHARGE, 1, 1);
        assertEquals(5, charge.maxX() - charge.minX() + 1);
        assertEquals(3, charge.maxY() - charge.minY() + 1);
        assertEquals(5, charge.maxZ() - charge.minZ() + 1);
        assertTrue(charge.counts().get(Role.AIR) > 0);

        Layout nest = StructureShapes.minimum("drone_nest", Family.CHARGE, 1, 1);
        assertEquals(56, nest.counts().get(Role.CASING));
        assertEquals(6, nest.maxX() - nest.minX()); // 7-wide pad
        assertEquals(2, nest.maxY());
        assertTrue(nest.maxX() - nest.minX() > charge.maxX() - charge.minX());
    }
}
