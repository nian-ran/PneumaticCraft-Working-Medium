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
        BlockPos farEnd
) {
    public static FormedStructure none() {
        return new FormedStructure(0, 0, 1, false, false, false, false, false, PressureGrade.G0, List.of(), BlockPos.ZERO);
    }

    public boolean formed() {
        return size > 0;
    }
}
