package dev.pncwm.blockentity;

import java.util.Collection;

import dev.pncwm.pressure.PressureGrade;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.api.tileentity.IManoMeasurable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * One PneumaticCraft air handler and one heat exchanger, created on first tick because the API is not ready during registration.
 */
public abstract class AirMachine extends BlockEntity implements IManoMeasurable {
    protected IAirHandlerMachine air;
    protected IHeatExchangerLogic heat;
    private CompoundTag savedAir;
    private CompoundTag savedHeat;
    private boolean hullReady;
    private PressureGrade builtTier;
    private int lastSavedAir = Integer.MIN_VALUE;
    private int lastSavedKelvin = Integer.MIN_VALUE;

    protected AirMachine(BlockEntityType<?> type, net.minecraft.core.BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract PressureGrade airTier();

    protected abstract int airVolume();

    protected abstract Collection<Direction> airFaces();

    public IAirHandlerMachine air() {
        openAir();
        return air;
    }

    public IHeatExchangerLogic heat() {
        openAir();
        return heat;
    }

    protected void openAir() {
        if (air != null && builtTier == airTier()) {
            return;
        }
        int kept = air == null ? 0 : air.getAir();
        double keptTemp = heat == null ? Double.NaN : heat.getTemperature();
        var api = PneumaticRegistry.getInstance();
        builtTier = airTier();
        air = api.getAirHandlerMachineFactory().createAirHandler(builtTier, airVolume());
        heat = api.getHeatRegistry().makeHeatExchangerLogic();
        if (!Double.isNaN(keptTemp)) {
            heat.setTemperature(keptTemp);
        }
        if (savedAir != null) {
            air.deserializeNBT(savedAir);
            savedAir = null;
            kept = 0;
        }
        if (savedHeat != null) {
            heat.deserializeNBT(savedHeat);
            savedHeat = null;
        }
        if (kept > 0) {
            air.addAir(kept);
        }
        hullReady = false;
    }

    public void tickPneumatics(boolean tickHeat) {
        if (level == null) {
            return;
        }
        openAir();
        Collection<Direction> faces = airFaces();
        air.setConnectableFaces(faces);
        if (!hullReady) {
            heat.initializeAsHull(level, worldPosition, IHeatExchangerLogic.ALL_BLOCKS, faces.toArray(Direction[]::new));
            hullReady = true;
        }
        air.tick(this);
        if (tickHeat && !level.isClientSide) {
            heat.tick();
        }
        if (!level.isClientSide && level.getGameTime() % 20 == 0) {
            // Tubes, gauges and relief valves never marked themselves dirty, so pressure and heat could be lost on reload.
            int nowAir = air.getAir();
            int nowKelvin = (int) Math.round(heat.getTemperature());
            if (nowAir != lastSavedAir || nowKelvin != lastSavedKelvin) {
                lastSavedAir = nowAir;
                lastSavedKelvin = nowKelvin;
                setChanged();
            }
        }
    }

    public void neighborChanged() {
        hullReady = false;
    }

    public void savePneumatics(CompoundTag tag) {
        if (air != null) {
            Tag saved = air.serializeNBT();
            if (saved instanceof CompoundTag compound) {
                tag.put("Air", compound);
            }
        } else if (savedAir != null) {
            tag.put("Air", savedAir);
        }
        if (heat != null) {
            tag.put("Heat", heat.serializeNBT());
        } else if (savedHeat != null) {
            tag.put("Heat", savedHeat);
        }
    }

    public void loadPneumatics(CompoundTag tag) {
        if (tag.get("Air") instanceof CompoundTag compound) {
            savedAir = compound;
            if (air != null) {
                air.deserializeNBT(compound);
                savedAir = null;
            }
        }
        if (tag.get("Heat") instanceof CompoundTag compound) {
            savedHeat = compound;
            if (heat != null) {
                heat.deserializeNBT(compound);
                savedHeat = null;
            }
        }
    }

    @Override
    public void printManometerMessage(Player player, List<Component> curInfo) {
        if (air == null) {
            return;
        }
        curInfo.add(Component.translatable("tooltip.workingmedium.pressure", String.format("%.2f", air.getPressure()), airTier().id));
        if (heat != null) {
            curInfo.add(Component.translatable("tooltip.workingmedium.temperature", PressureGrade.kelvinToCelsius(heat.getTemperature())));
        }
    }

    protected void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }
}
