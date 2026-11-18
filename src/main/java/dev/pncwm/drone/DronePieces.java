package dev.pncwm.drone;

import dev.pncwm.WorkingMedium;
import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.blockentity.GaugeBlockEntity;
import dev.pncwm.blockentity.HatchBlockEntity;
import dev.pncwm.pressure.PressureGrade;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.drone.IBlockInteractHandler;
import me.desht.pneumaticcraft.api.drone.ICustomBlockInteract;
import me.desht.pneumaticcraft.api.drone.IDrone;
import me.desht.pneumaticcraft.api.misc.IGlobalVariableHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DronePieces {
    private DronePieces() {
    }

    public static void register() {
        var registry = PneumaticRegistry.getInstance().getDroneRegistry();
        registry.registerCustomBlockInteractor(new ReadGauge());
        registry.registerCustomBlockInteractor(new HoldWindow());
    }

    /** A bound hatch answers with the same controller the official manometer now reads. */
    private static ApparatusBlockEntity apparatusAt(IDrone drone, BlockPos pos) {
        BlockEntity be = drone.getDroneLevel().getBlockEntity(pos);
        if (be instanceof ApparatusBlockEntity apparatus) {
            return apparatus;
        }
        if (be instanceof HatchBlockEntity hatch) {
            return hatch.controller();
        }
        return null;
    }

    private static void writeReading(IDrone drone, ApparatusBlockEntity apparatus) {
        float bar = apparatus.air() == null ? 0 : apparatus.air().getPressure();
        int celsius = apparatus.heat() == null ? 0 : PressureGrade.kelvinToCelsius(apparatus.heat().getTemperature());
        writeReading(drone, bar, celsius, apparatus.status().inOperatingWindow());
    }

    private static void writeReading(IDrone drone, float bar, int celsius, boolean inWindow) {
        IGlobalVariableHelper variables = PneumaticRegistry.getInstance().getMiscHelpers().getGlobalVariableHelper();
        int hundredths = Math.round(bar * 100f);
        variables.setPos(drone.getOwnerUUID(), "%wm_reading", new BlockPos(hundredths, celsius, inWindow ? 1 : 0));
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
            BlockEntity be = drone.getDroneLevel().getBlockEntity(pos);
            if (be instanceof GaugeBlockEntity gauge) {
                if (gauge.air() == null) {
                    return false;
                }
                if (simulate) {
                    return true;
                }
                int celsius = gauge.heat() == null ? 0 : PressureGrade.kelvinToCelsius(gauge.heat().getTemperature());
                writeReading(drone, gauge.air().getPressure(), celsius, false);
                return false;
            }
            ApparatusBlockEntity apparatus = apparatusAt(drone, pos);
            if (apparatus == null || apparatus.air() == null) {
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
            ApparatusBlockEntity apparatus = apparatusAt(drone, pos);
            if (apparatus == null) {
                return false;
            }
            boolean held = apparatus.status().inOperatingWindow();
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
