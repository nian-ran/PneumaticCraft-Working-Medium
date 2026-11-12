package dev.pncwm.block;

import dev.pncwm.pressure.PressureGrade;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CasingBlock extends Block {
    public final PressureGrade grade;

    public CasingBlock(PressureGrade grade, Properties properties) {
        super(properties);
        this.grade = grade;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.getBlock() instanceof CasingBlock other && other.grade == grade;
    }
}
