package dev.pncwm.multiblock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.pncwm.apparatus.Family;

/**
 * Minimum valid shapes used by JEI and tests. Coordinates match {@link StructureMatcher}:
 * x is right, y is up, z is forward from the controller's facing.
 */
public final class StructureShapes {
    private StructureShapes() {
    }

    public enum Role {
        CONTROLLER,
        CASING,
        TRAY,
        COIL,
        STAGE,
        INTERCOOLER,
        BAY,
        BODY,
        CAP,
        AIR
    }

    public record Cell(int x, int y, int z, Role role) {
    }

    public record Layout(String id, Family family, int minSize, int maxSize, List<Cell> cells) {
        public Map<Role, Integer> counts() {
            Map<Role, Integer> counts = new EnumMap<>(Role.class);
            for (Cell cell : cells) {
                counts.merge(cell.role(), 1, Integer::sum);
            }
            return counts;
        }

        public int minX() {
            return cells.stream().mapToInt(Cell::x).min().orElse(0);
        }

        public int maxX() {
            return cells.stream().mapToInt(Cell::x).max().orElse(0);
        }

        public int minY() {
            return cells.stream().mapToInt(Cell::y).min().orElse(0);
        }

        public int maxY() {
            return cells.stream().mapToInt(Cell::y).max().orElse(0);
        }

        public int minZ() {
            return cells.stream().mapToInt(Cell::z).min().orElse(0);
        }

        public int maxZ() {
            return cells.stream().mapToInt(Cell::z).max().orElse(0);
        }

        public boolean scalable() {
            return maxSize > minSize;
        }

        public Cell controller() {
            return cells.stream().filter(cell -> cell.role() == Role.CONTROLLER).findFirst().orElseThrow();
        }
    }

    public static Layout minimum(String id, Family family, int minSize, int maxSize) {
        return switch (family) {
            case CHAMBER -> "cyclone".equals(id)
                    ? shell(id, family, minSize, maxSize, -2, 2, 0, 6, 0, 4, 0, 3, 0, true)
                    : shell(id, family, minSize, maxSize, -2, 2, 0, 4, 0, 4, 0, 2, 0, true);
            case TOWER -> tower(id, family, minSize, maxSize);
            case COLUMN -> column(id, family, minSize, maxSize);
            case GALLERY, MANIFOLD, HEAT_LINE -> gallery(id, family, minSize, maxSize);
            case KETTLE -> kettle(id, family, minSize, maxSize);
            case ROOM -> room(id, family, minSize, maxSize);
            case STORAGE -> storage(id, family, minSize, maxSize);
            case FLARE -> flare(id, family, minSize, maxSize);
            case QUENCH -> quench(id, family, minSize, maxSize);
            case REGULATOR -> shell(id, family, minSize, maxSize, -2, 2, 0, 2, 0, 4, 0, 0, 0, false);
            case CALIBRATION -> bench(id, family, minSize, maxSize);
            case CHARGE -> "drone_nest".equals(id)
                    ? nest(id, family, minSize, maxSize)
                    : shell(id, family, minSize, maxSize, -2, 2, 0, 2, 0, 4, 0, 1, 0, true);
            case CYCLE -> ring(id, family, minSize, maxSize);
            case LOOP -> loop(id, family, minSize, maxSize);
        };
    }

    private static Layout shell(String id, Family family, int minSize, int maxSize,
                                int r0, int r1, int u0, int u1, int f0, int f1,
                                int ctrlR, int ctrlU, int ctrlF, boolean hollow) {
        List<Cell> cells = new ArrayList<>();
        for (int r = r0; r <= r1; r++) {
            for (int u = u0; u <= u1; u++) {
                for (int f = f0; f <= f1; f++) {
                    boolean edge = r == r0 || r == r1 || u == u0 || u == u1 || f == f0 || f == f1;
                    if (r == ctrlR && u == ctrlU && f == ctrlF) {
                        cells.add(new Cell(r, u, f, Role.CONTROLLER));
                        continue;
                    }
                    if (!edge) {
                        if (hollow) {
                            cells.add(new Cell(r, u, f, Role.AIR));
                        } else {
                            cells.add(new Cell(r, u, f, Role.CASING));
                        }
                        continue;
                    }
                    cells.add(new Cell(r, u, f, Role.CASING));
                }
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout tower(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int trays = Math.max(1, minSize);
        // 5x5 foundation
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        // 3x3 shaft centered on the base (forward 1..3)
        for (int y = 1; y <= trays; y++) {
            for (int r = -1; r <= 1; r++) {
                for (int f = 1; f <= 3; f++) {
                    boolean center = r == 0 && f == 2;
                    cells.add(new Cell(r, y, f, center ? Role.TRAY : Role.CASING));
                }
            }
        }
        int capY = trays + 1;
        for (int r = -1; r <= 1; r++) {
            for (int f = 1; f <= 3; f++) {
                boolean center = r == 0 && f == 2;
                cells.add(new Cell(r, capY, f, center ? Role.CAP : Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout column(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int stages = Math.max(1, minSize);
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        int y = 1;
        for (int i = 0; i < stages; i++) {
            addJacket(cells, y, Role.STAGE);
            y++;
            if (i < stages - 1) {
                addJacket(cells, y, Role.INTERCOOLER);
                y++;
            }
        }
        addJacket(cells, y, Role.CAP);
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static void addJacket(List<Cell> cells, int y, Role center) {
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                boolean mid = r == 0 && f == 1;
                cells.add(new Cell(r, y, f, mid ? center : Role.CASING));
            }
        }
    }

    private static Layout gallery(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int bays = Math.max(1, minSize);
        // front wall, controller in the middle of the face
        for (int r = -1; r <= 1; r++) {
            for (int u = 0; u <= 2; u++) {
                if (r == 0 && u == 1) {
                    cells.add(new Cell(0, 1, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, u, 0, Role.CASING));
                }
            }
        }
        for (int z = 1; z <= bays; z++) {
            for (int r = -1; r <= 1; r++) {
                for (int u = 0; u <= 2; u++) {
                    boolean mid = r == 0 && u == 1;
                    cells.add(new Cell(r, u, z, mid ? Role.BAY : Role.CASING));
                }
            }
        }
        int capZ = bays + 1;
        for (int r = -1; r <= 1; r++) {
            for (int u = 0; u <= 2; u++) {
                boolean mid = r == 0 && u == 1;
                cells.add(new Cell(r, u, capZ, mid ? Role.CAP : Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout kettle(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int bodies = Math.max(1, minSize);
        // 5x5 floor
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        for (int y = 1; y <= bodies; y++) {
            for (int r = -2; r <= 2; r++) {
                for (int f = 0; f <= 4; f++) {
                    boolean edge = r == -2 || r == 2 || f == 0 || f == 4;
                    if (r == 0 && f == 2) {
                        cells.add(new Cell(r, y, f, Role.BODY));
                    } else if (edge) {
                        cells.add(new Cell(r, y, f, Role.CASING));
                    } else {
                        cells.add(new Cell(r, y, f, Role.AIR));
                    }
                }
            }
        }
        int capY = bodies + 1;
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                boolean mid = r == 0 && f == 2;
                cells.add(new Cell(r, capY, f, mid ? Role.CAP : Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout room(String id, Family family, int minSize, int maxSize) {
        int span = Math.max(3, minSize);
        int outer = span + 2;
        int half = outer / 2;
        return shell(id, family, minSize, maxSize, -half, half, 0, outer - 1, 0, outer - 1, 0, 1, 0, true);
    }

    private static Layout storage(String id, Family family, int minSize, int maxSize) {
        int span = Math.max(5, minSize);
        int half = span / 2;
        List<Cell> cells = new ArrayList<>();
        for (int r = -half; r <= half; r++) {
            for (int u = 0; u < span; u++) {
                for (int f = 0; f < span; f++) {
                    if (r == 0 && u == 0 && f == 0) {
                        cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                    } else {
                        cells.add(new Cell(r, u, f, Role.CASING));
                    }
                }
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout flare(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int shaft = Math.max(1, minSize);
        for (int y = 0; y <= 1; y++) {
            for (int r = -1; r <= 1; r++) {
                for (int f = 0; f <= 2; f++) {
                    if (r == 0 && f == 0 && y == 0) {
                        cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                    } else {
                        cells.add(new Cell(r, y, f, Role.CASING));
                    }
                }
            }
        }
        for (int y = 2; y <= shaft + 1; y++) {
            cells.add(new Cell(0, y, 1, Role.CASING));
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout quench(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int depth = Math.max(1, minSize);
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        for (int y = 1; y <= depth; y++) {
            for (int r = -1; r <= 1; r++) {
                for (int f = 0; f <= 2; f++) {
                    boolean mid = r == 0 && f == 1;
                    cells.add(new Cell(r, y, f, mid ? Role.BODY : Role.CASING));
                }
            }
        }
        int capY = depth + 1;
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                boolean mid = r == 0 && f == 1;
                cells.add(new Cell(r, capY, f, mid ? Role.CAP : Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout bench(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        for (int r = -2; r <= 2; r++) {
            for (int f = -2; f <= 2; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        // two-tall backboard along the back edge
        for (int y = 1; y <= 2; y++) {
            for (int r = -2; r <= 2; r++) {
                cells.add(new Cell(r, y, 2, Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout nest(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        for (int r = -3; r <= 3; r++) {
            for (int f = -3; f <= 3; f++) {
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.CASING));
                }
            }
        }
        for (int r : new int[]{-3, 3}) {
            for (int f : new int[]{-3, 3}) {
                cells.add(new Cell(r, 1, f, Role.CASING));
                cells.add(new Cell(r, 2, f, Role.CASING));
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout ring(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y <= 1; y++) {
            for (int r = -2; r <= 2; r++) {
                for (int f = 0; f <= 4; f++) {
                    boolean edge = r == -2 || r == 2 || f == 0 || f == 4;
                    if (r == 0 && f == 0 && y == 0) {
                        cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                    } else if (edge) {
                        cells.add(new Cell(r, y, f, Role.CASING));
                    } else {
                        cells.add(new Cell(r, y, f, Role.AIR));
                    }
                }
            }
        }
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }

    private static Layout loop(String id, Family family, int minSize, int maxSize) {
        List<Cell> cells = new ArrayList<>();
        int span = Math.max(5, minSize);
        int half = span / 2;
        for (int r = -half; r <= half; r++) {
            for (int f = 0; f < span; f++) {
                boolean edge = r == -half || r == half || f == 0 || f == span - 1;
                if (!edge) {
                    continue;
                }
                if (r == 0 && f == 0) {
                    cells.add(new Cell(0, 0, 0, Role.CONTROLLER));
                } else {
                    cells.add(new Cell(r, 0, f, Role.BODY));
                }
            }
        }
        // optional-looking but present in the minimum schematic: one coil on the first body
        cells.add(new Cell(half, 1, 0, Role.COIL));
        return new Layout(id, family, minSize, maxSize, List.copyOf(cells));
    }
}
