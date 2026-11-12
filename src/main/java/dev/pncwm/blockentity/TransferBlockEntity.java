package dev.pncwm.blockentity;

import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import dev.pncwm.registry.WMBlockEntities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Pressure break. Front face is the outlet, back face is the inlet. The two handlers never equalize through this block.
 */
public class TransferBlockEntity extends BlockEntity implements IManoMeasurable {
    private final boolean booster;
    private IAirHandlerMachine inlet;
    private IAirHandlerMachine outlet;
    private IHeatExchangerLogic heat;
    private CompoundTag savedInlet;
    private CompoundTag savedOutlet;
    private CompoundTag savedHeat;
    private boolean hullReady;
    private long lastSaved = Long.MIN_VALUE;
    private int lastSignal = -1;

    public TransferBlockEntity(BlockPos pos, BlockState state, boolean booster) {
        super(WMBlockEntities.TRANSFER.get(), pos, state);
        this.booster = booster;
    }

    public boolean booster() {
        return booster;
    }

    public IAirHandlerMachine air(Direction side) {
        open();
        if (side == null) {
            return inlet;
        }
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (side == facing) {
            return outlet;
        }
        if (side == facing.getOpposite()) {
            return inlet;
        }
        return null;
    }

    public IHeatExchangerLogic heat() {
        open();
        return heat;
    }

    private void open() {
        if (inlet != null) {
            return;
        }
        var factory = PneumaticRegistry.getInstance().getAirHandlerMachineFactory();
        inlet = factory.createAirHandler(PressureGrade.G4, 4000);
        outlet = factory.createAirHandler(booster ? PressureGrade.G4 : PressureGrade.G2, 4000);
        heat = PneumaticRegistry.getInstance().getHeatRegistry().makeHeatExchangerLogic();
        if (savedInlet != null) {
            inlet.deserializeNBT(savedInlet);
            savedInlet = null;
        }
        if (savedOutlet != null) {
            outlet.deserializeNBT(savedOutlet);
            savedOutlet = null;
        }
        if (savedHeat != null) {
            heat.deserializeNBT(savedHeat);
            savedHeat = null;
        }
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        open();
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        inlet.setConnectableFaces(List.of(facing.getOpposite()));
        outlet.setConnectableFaces(List.of(facing));
        if (!hullReady) {
            heat.initializeAsHull(level, worldPosition, IHeatExchangerLogic.ALL_BLOCKS, Direction.values());
            hullReady = true;
        }
        inlet.tick(this);
        outlet.tick(this);
        heat.tick();
        if (booster) {
            boost();
        } else {
            reduce();
        }
        if (level.getGameTime() % 20 == 0) {
            // Mark dirty when either side changed, so both pressures and the heat are actually written to disk.
            long now = ((long) inlet.getAir() << 32) ^ (outlet.getAir() & 0xFFFFFFFFL) ^ Math.round(heat.getTemperature());
            if (now != lastSaved) {
                lastSaved = now;
                setChanged();
            }
        }
        int signal = signal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Comparator output from outlet pressure against the outlet's rating (G2 on a reducer, G4 on a booster). */
    public int signal() {
        if (outlet == null) {
            return 0;
        }
        float safe = booster ? PressureGrade.G4.safeBar : PressureGrade.G2.safeBar;
        return Tuning.pressureSignal(outlet.getPressure(), safe);
    }

    private void reduce() {
        PressureGrade source = PressureGrade.fromPressure(inlet.getPressure());
        float target = source.previous().safeBar;
        if (source == PressureGrade.G0) {
            target = 2.5f;
        }
        if (outlet.getPressure() >= target || inlet.getPressure() < target + 0.4f) {
            return;
        }
        int room = (int) ((target - outlet.getPressure()) * outlet.getVolume());
        int spare = inlet.getAir() - (int) (target * inlet.getVolume());
        int move = Math.min(800, Math.min(room, Math.max(0, spare)));
        if (move <= 0) {
            return;
        }
        inlet.addAir(-move);
        outlet.addAir((int) (move * 0.75f));
        heat.addHeat(move * 0.08);
    }

    private void boost() {
        PressureGrade source = PressureGrade.fromPressure(inlet.getPressure());
        PressureGrade dest = source.next();
        if (dest == source) {
            return;
        }
        float target = dest.safeBar;
        if (outlet.getPressure() >= target) {
            return;
        }
        int produced = Math.min(200, (int) ((target - outlet.getPressure()) * outlet.getVolume()));
        int take = produced * 4;
        int floor = (int) (source.safeBar * 0.5f * inlet.getVolume());
        if (inlet.getAir() - take < floor || produced <= 0) {
            return;
        }
        inlet.addAir(-take);
        outlet.addAir(produced);
        heat.addHeat(take * 0.12);
    }

    public void tell(Player player) {
        open();
        player.displayClientMessage(Component.translatable(booster ? "message.workingmedium.booster" : "message.workingmedium.reducer",
                String.format("%.1f", inlet.getPressure()), String.format("%.1f", outlet.getPressure())), true);
    }

    @Override
    public void printManometerMessage(Player player, List<Component> curInfo) {
        if (inlet == null) {
            return;
        }
        curInfo.add(Component.translatable("tooltip.workingmedium.transfer",
                String.format("%.2f", inlet.getPressure()), String.format("%.2f", outlet.getPressure())));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (inlet != null) {
            if (inlet.serializeNBT() instanceof CompoundTag compound) {
                tag.put("Inlet", compound);
            }
            if (outlet.serializeNBT() instanceof CompoundTag compound) {
                tag.put("Outlet", compound);
            }
            tag.put("Heat", heat.serializeNBT());
        } else {
            if (savedInlet != null) {
                tag.put("Inlet", savedInlet);
            }
            if (savedOutlet != null) {
                tag.put("Outlet", savedOutlet);
            }
            if (savedHeat != null) {
                tag.put("Heat", savedHeat);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("Inlet") instanceof CompoundTag compound) {
            savedInlet = compound;
        }
        if (tag.get("Outlet") instanceof CompoundTag compound) {
            savedOutlet = compound;
        }
        if (tag.get("Heat") instanceof CompoundTag compound) {
            savedHeat = compound;
        }
    }
}
