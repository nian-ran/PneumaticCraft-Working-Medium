package dev.pncwm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PartBlock extends Block {
    public final PartKind kind;

    public PartBlock(PartKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        HullNeighbors.notifyControllers(level, pos);
    }
}
