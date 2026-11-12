package dev.pncwm.multiblock;

import java.util.ArrayList;
import java.util.List;

import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.block.CasingBlock;
import dev.pncwm.block.HatchBlock;
import dev.pncwm.block.HatchType;
import dev.pncwm.block.PartBlock;
import dev.pncwm.block.PartKind;
import dev.pncwm.pressure.PressureGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureMatcher {
    private StructureMatcher() {
    }

    public static FormedStructure match(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        return switch (kind.family) {
            case CHAMBER -> chamber(level, controller, facing, kind);
            case TOWER -> tower(level, controller, facing, kind);
            case COLUMN -> column(level, controller, facing, kind);
            case GALLERY, MANIFOLD, HEAT_LINE -> gallery(level, controller, facing, kind);
            case KETTLE -> kettle(level, controller, facing, kind);
            case ROOM -> room(level, controller, facing, kind);
            case STORAGE -> storage(level, controller, facing, kind);
            case FLARE -> flare(level, controller, facing, kind);
            case QUENCH -> quench(level, controller, facing, kind);
            case REGULATOR -> shell(level, controller, facing, -2, 2, 0, 2, 0, 4, 0, 0, 0, false);
            case CALIBRATION -> bench(level, controller, facing);
            case CHARGE -> "drone_nest".equals(kind.id)
                    ? nest(level, controller, facing)
                    : shell(level, controller, facing, -2, 2, 0, 2, 0, 4, 0, 1, 0, true);
            case CYCLE -> ring(level, controller, facing);
            case LOOP -> loop(level, controller, facing, kind);
        };
    }

    public static BlockPos world(BlockPos origin, Direction facing, int right, int up, int forward) {
        return origin.relative(facing.getClockWise(), right).relative(Direction.UP, up).relative(facing, forward);
    }

    private static FormedStructure chamber(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        if ("cyclone".equals(kind.id)) {
            return shell(level, controller, facing, -2, 2, 0, 6, 0, 4, 0, 3, 0, true);
        }
        return shell(level, controller, facing, -2, 2, 0, 4, 0, 4, 0, 2, 0, true);
    }

    private static FormedStructure bench(Level level, BlockPos controller, Direction facing) {
        Scan scan = new Scan();
        for (int r = -2; r <= 2; r++) {
            for (int f = -2; f <= 2; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        for (int y = 1; y <= 2; y++) {
            for (int r = -2; r <= 2; r++) {
                if (!scan.shell(level, world(controller, facing, r, y, 2))) {
                    return FormedStructure.none();
                }
            }
        }
        if (scan.weakest == null) {
            return FormedStructure.none();
        }
        return scan.build(1, world(controller, facing, 0, 2, 2));
    }

    private static FormedStructure nest(Level level, BlockPos controller, Direction facing) {
        Scan scan = new Scan();
        for (int r = -3; r <= 3; r++) {
            for (int f = -3; f <= 3; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        for (int r : new int[]{-3, 3}) {
            for (int f : new int[]{-3, 3}) {
                for (int y = 1; y <= 2; y++) {
                    if (!scan.shell(level, world(controller, facing, r, y, f))) {
                        return FormedStructure.none();
                    }
                }
            }
        }
        if (scan.weakest == null) {
            return FormedStructure.none();
        }
        return scan.build(1, controller);
    }

    private static FormedStructure ring(Level level, BlockPos controller, Direction facing) {
        Scan scan = new Scan();
        for (int y = 0; y <= 1; y++) {
            for (int r = -2; r <= 2; r++) {
                for (int f = 0; f <= 4; f++) {
                    boolean edge = r == -2 || r == 2 || f == 0 || f == 4;
                    BlockPos pos = world(controller, facing, r, y, f);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (edge) {
                        if (!scan.shell(level, pos)) {
                            return FormedStructure.none();
                        }
                    } else if (!level.getBlockState(pos).isAir()) {
                        return FormedStructure.none();
                    }
                }
            }
        }
        if (scan.weakest == null) {
            return FormedStructure.none();
        }
        return scan.build(1, world(controller, facing, 0, 1, 4));
    }

    private static FormedStructure shell(Level level, BlockPos controller, Direction facing,
                                         int r0, int r1, int u0, int u1, int f0, int f1,
                                         int ctrlR, int ctrlU, int ctrlF, boolean hollow) {
        Scan scan = new Scan();
        int midR = (r0 + r1) / 2;
        int midU = (u0 + u1) / 2;
        int midF = (f0 + f1) / 2;
        for (int r = r0; r <= r1; r++) {
            for (int u = u0; u <= u1; u++) {
                for (int f = f0; f <= f1; f++) {
                    boolean edge = r == r0 || r == r1 || u == u0 || u == u1 || f == f0 || f == f1;
                    BlockPos pos = shift(controller, facing, r - ctrlR, u - ctrlU, f - ctrlF);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (!edge) {
                        if (hollow) {
                            if (!level.getBlockState(pos).isAir()) {
                                return FormedStructure.none();
                            }
                        } else if (!scan.shell(level, pos)) {
                            return FormedStructure.none();
                        }
                        continue;
                    }
                    if (!scan.shell(level, pos)) {
                        return FormedStructure.none();
                    }
                }
            }
        }
        if (scan.weakest == null) {
            return FormedStructure.none();
        }
        return scan.build(1, shift(controller, facing, midR - ctrlR, midU - ctrlU, midF - ctrlF));
    }

    private static FormedStructure tower(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan floor = new Scan();
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!floor.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        int trays = 0;
        boolean flooded = false;
        int min = kind.minimumSize();
        for (int y = 1; y <= kind.maxSize; y++) {
            Scan ring = new Scan();
            boolean ok = true;
            for (int r = -1; r <= 1; r++) {
                for (int f = 1; f <= 3; f++) {
                    BlockPos pos = world(controller, facing, r, y, f);
                    boolean center = r == 0 && f == 2;
                    if (center) {
                        if (!isPart(level, pos, PartKind.TRAY)) {
                            ok = false;
                            break;
                        }
                    } else if (!ring.shell(level, pos)) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) {
                    break;
                }
            }
            if (!ok) {
                break;
            }
            trays++;
            if (!ring.hasSideDraw()) {
                flooded = true;
            }
            floor.absorb(ring);
        }
        if (trays < min) {
            return FormedStructure.none();
        }
        Scan cap = new Scan();
        for (int r = -1; r <= 1; r++) {
            for (int f = 1; f <= 3; f++) {
                BlockPos pos = world(controller, facing, r, trays + 1, f);
                boolean center = r == 0 && f == 2;
                if (center) {
                    if (!cap.shell(level, pos) && !isPart(level, pos, PartKind.CAP)) {
                        return FormedStructure.none();
                    }
                } else if (!cap.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        floor.absorb(cap);
        if (floor.weakest == null) {
            return FormedStructure.none();
        }
        FormedStructure formed = floor.build(trays, world(controller, facing, 0, trays + 1, 2));
        if (flooded) {
            return new FormedStructure(formed.size(), formed.coils(), formed.parallel(), formed.vent(), formed.drain(),
                    formed.missingIntercooler(), true, formed.airlockOpen(), formed.weakest(), formed.hatches(), formed.farEnd());
        }
        return formed;
    }

    private static FormedStructure column(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan scan = new Scan();
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        int stages = 0;
        boolean missing = false;
        boolean lastWasStage = false;
        BlockPos far = controller;
        int maxY = kind.maxSize * 2 + 2;
        for (int y = 1; y <= maxY; y++) {
            BlockPos center = world(controller, facing, 0, y, 1);
            if (isPart(level, center, PartKind.STAGE)) {
                if (!jacketRing(level, scan, controller, facing, y)) {
                    return FormedStructure.none();
                }
                if (lastWasStage) {
                    missing = true;
                }
                stages++;
                lastWasStage = true;
                far = center;
                if (stages > kind.maxSize) {
                    return FormedStructure.none();
                }
                continue;
            }
            if (isPart(level, center, PartKind.INTERCOOLER)) {
                if (!jacketRing(level, scan, controller, facing, y)) {
                    return FormedStructure.none();
                }
                lastWasStage = false;
                far = center;
                continue;
            }
            if (isPart(level, center, PartKind.CAP) || scan.shell(level, center)) {
                if (!jacketRing(level, scan, controller, facing, y)) {
                    return FormedStructure.none();
                }
                far = center;
                break;
            }
            return FormedStructure.none();
        }
        if (stages < kind.minimumSize()) {
            return FormedStructure.none();
        }
        if (scan.weakest == null) {
            scan.weakest = kind.grade;
        }
        FormedStructure formed = scan.build(stages, far);
        if (!missing) {
            return formed;
        }
        return new FormedStructure(formed.size(), formed.coils(), formed.parallel(), formed.vent(), formed.drain(),
                true, formed.flooded(), formed.airlockOpen(), formed.weakest(), formed.hatches(), formed.farEnd());
    }

    private static boolean jacketRing(Level level, Scan scan, BlockPos controller, Direction facing, int y) {
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                if (r == 0 && f == 1) {
                    continue;
                }
                if (!scan.shell(level, world(controller, facing, r, y, f))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static FormedStructure gallery(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan scan = new Scan();
        for (int r = -1; r <= 1; r++) {
            for (int u = 0; u <= 2; u++) {
                BlockPos pos = world(controller, facing, r, u, 0);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        int count = 0;
        BlockPos far = controller;
        for (int z = 1; z < 16; z++) {
            BlockPos center = world(controller, facing, 0, 1, z);
            if (isPart(level, center, PartKind.BAY)) {
                if (!tunnelRing(level, scan, controller, facing, z)) {
                    return FormedStructure.none();
                }
                count++;
                far = center;
                if (count > kind.maxSize) {
                    return FormedStructure.none();
                }
                continue;
            }
            if (count == 0) {
                return FormedStructure.none();
            }
            if (isPart(level, center, PartKind.CAP) || scan.shell(level, center)) {
                if (!tunnelRing(level, scan, controller, facing, z)) {
                    return FormedStructure.none();
                }
                far = center;
                break;
            }
            break;
        }
        if (count < kind.minimumSize()) {
            return FormedStructure.none();
        }
        if (scan.weakest == null) {
            scan.weakest = kind.grade;
        }
        return scan.build(count, far);
    }

    private static boolean tunnelRing(Level level, Scan scan, BlockPos controller, Direction facing, int z) {
        for (int r = -1; r <= 1; r++) {
            for (int u = 0; u <= 2; u++) {
                if (r == 0 && u == 1) {
                    continue;
                }
                if (!scan.shell(level, world(controller, facing, r, u, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static FormedStructure kettle(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan scan = new Scan();
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        int bodies = 0;
        BlockPos far = controller;
        for (int y = 1; y <= kind.maxSize; y++) {
            if (!isPart(level, world(controller, facing, 0, y, 2), PartKind.BODY)) {
                break;
            }
            if (!vesselLayer(level, scan, controller, facing, y, true)) {
                return FormedStructure.none();
            }
            bodies++;
            far = world(controller, facing, 0, y, 2);
        }
        if (bodies < kind.minimumSize()) {
            return FormedStructure.none();
        }
        int capY = bodies + 1;
        if (!vesselLayer(level, scan, controller, facing, capY, false)) {
            return FormedStructure.none();
        }
        BlockPos cap = world(controller, facing, 0, capY, 2);
        if (!scan.shell(level, cap) && !isPart(level, cap, PartKind.CAP)) {
            return FormedStructure.none();
        }
        far = cap;
        if (scan.weakest == null) {
            scan.weakest = kind.grade;
        }
        return scan.build(bodies, far);
    }

    private static boolean vesselLayer(Level level, Scan scan, BlockPos controller, Direction facing, int y, boolean hollow) {
        for (int r = -2; r <= 2; r++) {
            for (int f = 0; f <= 4; f++) {
                boolean edge = r == -2 || r == 2 || f == 0 || f == 4;
                boolean mid = r == 0 && f == 2;
                BlockPos pos = world(controller, facing, r, y, f);
                if (mid && hollow) {
                    continue;
                }
                if (edge || !hollow) {
                    if (mid) {
                        continue;
                    }
                    if (!scan.shell(level, pos)) {
                        return false;
                    }
                } else if (isPart(level, pos, PartKind.COIL)) {
                    scan.coils++;
                } else if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static FormedStructure quench(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan scan = new Scan();
        for (int r = -1; r <= 1; r++) {
            for (int f = 0; f <= 2; f++) {
                BlockPos pos = world(controller, facing, r, 0, f);
                if (pos.equals(controller)) {
                    continue;
                }
                if (!scan.shell(level, pos)) {
                    return FormedStructure.none();
                }
            }
        }
        int depth = 0;
        BlockPos far = controller;
        for (int y = 1; y <= kind.maxSize; y++) {
            BlockPos mid = world(controller, facing, 0, y, 1);
            if (!isPart(level, mid, PartKind.BODY)) {
                break;
            }
            if (!jacketRing(level, scan, controller, facing, y)) {
                return FormedStructure.none();
            }
            depth++;
            far = mid;
        }
        if (depth < kind.minSize) {
            return FormedStructure.none();
        }
        int capY = depth + 1;
        BlockPos cap = world(controller, facing, 0, capY, 1);
        if (!scan.shell(level, cap) && !isPart(level, cap, PartKind.CAP)) {
            return FormedStructure.none();
        }
        if (!jacketRing(level, scan, controller, facing, capY)) {
            return FormedStructure.none();
        }
        far = cap;
        if (scan.weakest == null) {
            scan.weakest = kind.grade;
        }
        return scan.build(depth, far);
    }

    private static FormedStructure flare(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        Scan scan = new Scan();
        for (int y = 0; y <= 1; y++) {
            for (int r = -1; r <= 1; r++) {
                for (int f = 0; f <= 2; f++) {
                    BlockPos pos = world(controller, facing, r, y, f);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (!scan.shell(level, pos)) {
                        return FormedStructure.none();
                    }
                }
            }
        }
        int height = 0;
        BlockPos far = controller;
        for (int y = 2; y <= kind.maxSize + 1; y++) {
            BlockPos pos = world(controller, facing, 0, y, 1);
            if (!scan.shell(level, pos)) {
                break;
            }
            height++;
            far = pos;
        }
        if (height < kind.minSize || scan.weakest == null) {
            return FormedStructure.none();
        }
        return scan.build(height, far);
    }

    private static FormedStructure room(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        FormedStructure best = FormedStructure.none();
        for (int span = kind.minSize; span <= kind.maxSize; span++) {
            int outer = span + 2;
            int half = outer / 2;
            FormedStructure formed = shell(level, controller, facing, -half, half, 0, outer - 1, 0, outer - 1, 0, 1, 0, true);
            if (formed.formed()) {
                best = new FormedStructure(span, formed.coils(), formed.parallel(), formed.vent(), formed.drain(),
                        formed.missingIntercooler(), formed.flooded(), formed.airlockOpen(), formed.weakest(), formed.hatches(), formed.farEnd());
            } else {
                break;
            }
        }
        return best;
    }

    private static FormedStructure storage(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        FormedStructure best = FormedStructure.none();
        int min = Math.max(5, kind.minSize);
        for (int span = min; span <= kind.maxSize; span++) {
            int half = span / 2;
            Scan scan = new Scan();
            boolean ok = true;
            int casings = 0;
            for (int r = -half; r <= half && ok; r++) {
                for (int u = 0; u < span && ok; u++) {
                    for (int f = 0; f < span && ok; f++) {
                        BlockPos pos = shift(controller, facing, r, u, f);
                        if (pos.equals(controller)) {
                            continue;
                        }
                        if (level.getBlockState(pos).getBlock() instanceof CasingBlock casing) {
                            scan.noteCasing(casing.grade);
                            casings++;
                        } else if (!scan.shell(level, pos)) {
                            ok = false;
                        }
                    }
                }
            }
            if (ok && casings > 0) {
                best = scan.build(casings, controller);
            } else {
                break;
            }
        }
        return best;
    }

    private static FormedStructure loop(Level level, BlockPos controller, Direction facing, ApparatusKind kind) {
        FormedStructure best = FormedStructure.none();
        int min = Math.max(5, kind.minSize);
        for (int span = min; span <= kind.maxSize; span += 2) {
            int half = span / 2;
            Scan scan = new Scan();
            boolean ok = true;
            int bodies = 0;
            BlockPos far = controller;
            for (int r = -half; r <= half && ok; r++) {
                for (int f = 0; f < span && ok; f++) {
                    boolean edge = r == -half || r == half || f == 0 || f == span - 1;
                    BlockPos pos = world(controller, facing, r, 0, f);
                    if (!edge) {
                        continue;
                    }
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (isPart(level, pos, PartKind.BODY)) {
                        bodies++;
                        far = pos;
                        if (isPart(level, pos.above(), PartKind.COIL)) {
                            scan.coils++;
                        }
                    } else {
                        ok = false;
                    }
                }
            }
            if (ok && bodies > 0) {
                if (scan.weakest == null) {
                    scan.weakest = kind.grade;
                }
                best = scan.build(span, far);
            } else {
                break;
            }
        }
        return best;
    }

    private static BlockPos shift(BlockPos origin, Direction facing, int right, int up, int forward) {
        return world(origin, facing, right, up, forward);
    }

    private static boolean isPart(Level level, BlockPos pos, PartKind kind) {
        return level.getBlockState(pos).getBlock() instanceof PartBlock part && part.kind == kind;
    }

    private static final class Scan {
        private PressureGrade weakest;
        private final List<BlockPos> hatches = new ArrayList<>();
        private int coils;
        private int parallel = 1;
        private boolean vent;
        private boolean drain;
        private boolean airlockOpen;

        boolean shell(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof CasingBlock casing) {
                noteCasing(casing.grade);
                return true;
            }
            if (block instanceof HatchBlock hatch) {
                hatches.add(pos.immutable());
                if (hatch.type == HatchType.VENT) {
                    vent = true;
                }
                if (hatch.type == HatchType.DRAIN) {
                    drain = true;
                }
                if (hatch.type == HatchType.MANIFOLD) {
                    parallel++;
                }
                return true;
            }
            if (block instanceof PartBlock part) {
                if (part.kind == PartKind.COIL) {
                    coils++;
                }
                if (part.kind == PartKind.AIRLOCK) {
                    airlockOpen = level.hasNeighborSignal(pos);
                }
                return part.kind == PartKind.CAP || part.kind == PartKind.AIRLOCK;
            }
            return false;
        }

        void noteCasing(PressureGrade grade) {
            if (weakest == null || grade.safeBar < weakest.safeBar) {
                weakest = grade;
            }
        }

        void absorb(Scan other) {
            if (other.weakest != null) {
                noteCasing(other.weakest);
            }
            hatches.addAll(other.hatches);
            coils += other.coils;
            parallel += other.parallel - 1;
            vent |= other.vent;
            drain |= other.drain;
            airlockOpen |= other.airlockOpen;
        }

        boolean hasSideDraw() {
            return !hatches.isEmpty();
        }

        FormedStructure build(int size, BlockPos far) {
            return new FormedStructure(size, coils, Math.max(1, parallel), vent, drain, false, false, airlockOpen,
                    weakest == null ? PressureGrade.G0 : weakest, List.copyOf(hatches), far);
        }
    }
}
