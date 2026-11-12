package dev.pncwm.blockentity;

import java.util.Collection;
import java.util.List;

import dev.pncwm.block.GradeTubeBlock;
import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.registry.WMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GradeTubeBlockEntity extends AirMachine {
    private boolean connectionsReady;

    public GradeTubeBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.TUBE.get(), pos, state);
    }

    public GradeTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected PressureGrade airTier() {
        if (getBlockState().getBlock() instanceof GradeTubeBlock tube) {
            return tube.grade;
        }
        return PressureGrade.G3;
    }

    @Override
    protected int airVolume() {
        return 400;
    }

    @Override
    protected Collection<Direction> airFaces() {
        return List.of(Direction.values());
    }

    public void tick() {
        if (!connectionsReady && level != null) {
            connectionsReady = true;
            if (getBlockState().getBlock() instanceof GradeTubeBlock tube) {
                BlockState updated = tube.connections(level, worldPosition, getBlockState());
                if (updated != getBlockState()) {
                    level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
        tickPneumatics(true);
    }

    public void tell(Player player) {
        if (air == null) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.workingmedium.tube",
                String.format("%.2f", air.getPressure()), airTier().safeBar), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        savePneumatics(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadPneumatics(tag);
    }
}
