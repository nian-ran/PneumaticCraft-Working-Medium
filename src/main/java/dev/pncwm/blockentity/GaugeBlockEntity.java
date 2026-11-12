package dev.pncwm.blockentity;

import java.util.Collection;
import java.util.List;

import dev.pncwm.block.GaugeBlock;
import dev.pncwm.pressure.Band;
import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import dev.pncwm.registry.WMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GaugeBlockEntity extends AirMachine {
    private int gradeIndex;
    private int signal;

    public GaugeBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.GAUGE.get(), pos, state);
    }

    @Override
    protected PressureGrade airTier() {
        return PressureGrade.G4;
    }

    @Override
    protected int airVolume() {
        return 80;
    }

    @Override
    protected Collection<Direction> airFaces() {
        return List.of(Direction.values());
    }

    public void tick() {
        tickPneumatics(true);
        if (level == null || level.isClientSide || air == null) {
            return;
        }
        PressureGrade grade = PressureGrade.byIndex(gradeIndex);
        Band band = grade == PressureGrade.G5
                ? Band.of(air.getPressure(), 70f, 80f, 82f)
                : Band.of(air.getPressure(), grade.safeBar * 0.45f, grade.safeBar, grade.safeBar);
        BlockState state = getBlockState();
        if (state.getValue(GaugeBlock.BAND) != band) {
            level.setBlock(worldPosition, state.setValue(GaugeBlock.BAND, band), Block.UPDATE_CLIENTS);
        }
        int next = Tuning.pressureSignal(air.getPressure(), grade == PressureGrade.G5 ? 80f : grade.safeBar);
        if (next != signal) {
            signal = next;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Comparator output: 0 when empty, 15 at (or above) the safe pressure of the grade being judged. */
    public int signal() {
        return signal;
    }

    public void cycleGrade(Player player) {
        gradeIndex = (gradeIndex + 1) % PressureGrade.values().length;
        setChanged();
        tell(player);
    }

    public void tell(Player player) {
        if (air == null || heat == null) {
            return;
        }
        PressureGrade grade = PressureGrade.byIndex(gradeIndex);
        player.displayClientMessage(Component.translatable("message.workingmedium.gauge",
                grade.id,
                String.format("%.2f", air.getPressure()),
                PressureGrade.kelvinToCelsius(heat.getTemperature())), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        savePneumatics(tag);
        tag.putInt("Grade", gradeIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadPneumatics(tag);
        gradeIndex = tag.getInt("Grade");
    }
}
