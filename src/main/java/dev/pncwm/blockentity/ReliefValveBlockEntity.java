package dev.pncwm.blockentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import dev.pncwm.registry.WMBlockEntities;
import dev.pncwm.registry.WMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ReliefValveBlockEntity extends AirMachine {
    private static final float[] SETPOINTS = {5f, 10f, 18f, 20f, 50f, 80f, 120f};
    private int index = 2;
    private int lastSignal = -1;

    public ReliefValveBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.RELIEF.get(), pos, state);
    }

    @Override
    protected PressureGrade airTier() {
        return PressureGrade.G4;
    }

    @Override
    protected int airVolume() {
        return 1000;
    }

    @Override
    protected Collection<Direction> airFaces() {
        Direction vent = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        List<Direction> faces = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction != vent) {
                faces.add(direction);
            }
        }
        return faces;
    }

    public void tick() {
        tickPneumatics(true);
        if (air != null) {
            Direction vent = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            float setpoint = SETPOINTS[index];
            air.enableSafetyVenting(pressure -> pressure > setpoint, vent);
            if (level != null && !level.isClientSide && air.getPressure() > setpoint && level.getGameTime() % 40 == 0) {
                WMSounds.play(level, worldPosition, WMSounds.VENT.get(), SoundSource.BLOCKS, 0.45f, 1.05f);
            }
            int signal = signal();
            if (level != null && !level.isClientSide && signal != lastSignal) {
                lastSignal = signal;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    /** Comparator output against the current relief setpoint. */
    public int signal() {
        if (air == null) {
            return 0;
        }
        return Tuning.pressureSignal(air.getPressure(), SETPOINTS[index]);
    }

    public void cycle(Player player) {
        index = (index + 1) % SETPOINTS.length;
        setChanged();
        tell(player);
    }

    public void tell(Player player) {
        player.displayClientMessage(Component.translatable("message.workingmedium.relief", SETPOINTS[index]), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        savePneumatics(tag);
        tag.putInt("Setpoint", index);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadPneumatics(tag);
        index = Math.floorMod(tag.getInt("Setpoint"), SETPOINTS.length);
    }
}
