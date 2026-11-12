package dev.pncwm.drone;

import dev.pncwm.WorkingMedium;
import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.pressure.MachineStatus;
import dev.pncwm.pressure.PressureGrade;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.drone.IBlockInteractHandler;
import me.desht.pneumaticcraft.api.drone.ICustomBlockInteract;
import me.desht.pneumaticcraft.api.drone.IDrone;
import me.desht.pneumaticcraft.api.misc.IGlobalVariableHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public final class DronePieces {
    private DronePieces() {
    }

    public static void register() {
        var registry = PneumaticRegistry.getInstance().getDroneRegistry();
        registry.registerCustomBlockInteractor(new ReadGauge());
        registry.registerCustomBlockInteractor(new HoldWindow());
    }

    private static void writeReading(IDrone drone, ApparatusBlockEntity apparatus) {
        IGlobalVariableHelper variables = PneumaticRegistry.getInstance().getMiscHelpers().getGlobalVariableHelper();
        int bar = apparatus.air() == null ? 0 : Math.round(apparatus.air().getPressure() * 100f);
        int celsius = apparatus.heat() == null ? 0 : PressureGrade.kelvinToCelsius(apparatus.heat().getTemperature());
        int window = apparatus.status() == MachineStatus.WINDOW || apparatus.status() == MachineStatus.RUNNING ? 1 : 0;
        variables.setPos(drone.getOwnerUUID(), "%wm_reading", new BlockPos(bar, celsius, window));
        drone.onVariableChanged("%wm_reading", true);
    }

    private static final class ReadGauge implements ICustomBlockInteract {
        @Override
        public String getID() {
            return "workingmedium_read_gauge";
        }

        @Override
        public ResourceLocation getTexture() {
            return ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "textures/progwidgets/read_gauge.png");
        }

        @Override
        public boolean doInteract(BlockPos pos, IDrone drone, IBlockInteractHandler handler, boolean simulate) {
            if (!(drone.getDroneLevel().getBlockEntity(pos) instanceof ApparatusBlockEntity apparatus) || apparatus.air() == null) {
                return false;
            }
            if (simulate) {
                return true;
            }
            writeReading(drone, apparatus);
            return false;
        }

        @Override
        public DyeColor getColor() {
            return DyeColor.CYAN;
        }
    }

    private static final class HoldWindow implements ICustomBlockInteract {
        @Override
        public String getID() {
            return "workingmedium_hold_window";
        }

        @Override
        public ResourceLocation getTexture() {
            return ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "textures/progwidgets/hold_window.png");
        }

        @Override
        public boolean doInteract(BlockPos pos, IDrone drone, IBlockInteractHandler handler, boolean simulate) {
            if (!(drone.getDroneLevel().getBlockEntity(pos) instanceof ApparatusBlockEntity apparatus)) {
                return false;
            }
            boolean held = apparatus.status() == MachineStatus.WINDOW || apparatus.status() == MachineStatus.CERTIFIED;
            if (!simulate && held) {
                writeReading(drone, apparatus);
            }
            return held;
        }

        @Override
        public DyeColor getColor() {
            return DyeColor.LIME;
        }
    }
}
