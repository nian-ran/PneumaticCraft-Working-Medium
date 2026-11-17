package dev.pncwm.multiblock;

import java.util.List;

import dev.pncwm.pressure.PressureGrade;
import net.minecraft.core.BlockPos;

public record FormedStructure(
        int size,
        int coils,
        int parallel,
        boolean vent,
        boolean drain,
        boolean missingIntercooler,
        boolean flooded,
        boolean airlockOpen,
        PressureGrade weakest,
        List<BlockPos> hatches,
        BlockPos farEnd,
        List<BlockPos> hull
) {
    public FormedStructure {
        hatches = List.copyOf(hatches);
        hull = List.copyOf(hull);
    }

    public static FormedStructure none() {
        return new FormedStructure(0, 0, 1, false, false, false, false, false, PressureGrade.G0, List.of(), BlockPos.ZERO, List.of());
    }

    public boolean formed() {
        return size > 0;
    }

    public FormedStructure withMissingIntercooler() {
        return new FormedStructure(size, coils, parallel, vent, drain, true, flooded, airlockOpen, weakest, hatches, farEnd, hull);
    }

    public FormedStructure withFlooded() {
        return new FormedStructure(size, coils, parallel, vent, drain, missingIntercooler, true, airlockOpen, weakest, hatches, farEnd, hull);
    }

    public FormedStructure withSize(int size) {
        return new FormedStructure(size, coils, parallel, vent, drain, missingIntercooler, flooded, airlockOpen, weakest, hatches, farEnd, hull);
    }
}
