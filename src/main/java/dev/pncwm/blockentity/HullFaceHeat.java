package dev.pncwm.blockentity;

import java.util.ArrayList;
import java.util.List;

import dev.pncwm.multiblock.HeatHull;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Lava, ice, furnaces and other PneumaticCraft heat behaviours only attach when
 * {@link IHeatExchangerLogic#initializeAsHull} inspects a neighbouring block. That method
 * resets earlier behaviours, so the controller's own call cannot also cover the rest of
 * the shell. Each hull cell that still has unscanned outward faces gets a small auxiliary
 * exchanger initialised at that cell. The auxiliaries are kept at the same temperature
 * and capacity as the controller, then their heat change is written back, so the shell
 * stays one thermal mass.
 *
 * <p>Heat pipes are attached on the controller itself (they expose a heat capability),
 * so they are detached from these auxiliaries to avoid exchanging twice. Empty air is
 * left out so a larger casing does not dump heat into the atmosphere from every face.
 */
public final class HullFaceHeat {
    private final List<CellLogic> cells = new ArrayList<>();

    private record CellLogic(BlockPos pos, Direction[] faces, IHeatExchangerLogic logic) {
    }

    public void clear() {
        cells.clear();
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public void rebuild(Level level, IHeatExchangerLogic main, List<HeatHull.CellFaces> groups) {
        clear();
        if (level == null || level.isClientSide || main == null || groups == null || groups.isEmpty()) {
            return;
        }
        var registry = PneumaticRegistry.getInstance().getHeatRegistry();
        for (HeatHull.CellFaces group : groups) {
            if (group.faces().isEmpty()) {
                continue;
            }
            BlockPos pos = new BlockPos(group.cell().x(), group.cell().y(), group.cell().z());
            Direction[] faces = new Direction[group.faces().size()];
            for (int i = 0; i < faces.length; i++) {
                HeatHull.Face face = group.faces().get(i);
                faces[i] = directionOf(face.dx(), face.dy(), face.dz());
            }
            IHeatExchangerLogic aux = registry.makeHeatExchangerLogic();
            aux.setTemperature(main.getTemperature());
            aux.setThermalCapacity(main.getThermalCapacity());
            aux.setThermalResistance(main.getThermalResistance());
            aux.initializeAsHull(level, pos, HullFaceHeat::notEmpty, faces);
            detachCapabilityNeighbors(level, aux, pos, faces);
            cells.add(new CellLogic(pos, faces, aux));
        }
    }

    /**
     * Run each auxiliary hull from the controller's current temperature, then apply the
     * combined heat change once, so every face sees the same starting temperature this tick.
     */
    public void tick(IHeatExchangerLogic main) {
        if (main == null || cells.isEmpty()) {
            return;
        }
        double capacity = main.getThermalCapacity();
        double resistance = main.getThermalResistance();
        double start = main.getTemperature();
        double totalHeat = 0;
        for (CellLogic cell : cells) {
            IHeatExchangerLogic aux = cell.logic();
            aux.setThermalCapacity(capacity);
            aux.setThermalResistance(resistance);
            aux.setTemperature(start);
            aux.tick();
            totalHeat += (aux.getTemperature() - start) * capacity;
        }
        if (totalHeat != 0) {
            main.addHeat(totalHeat);
        }
    }

    static boolean notEmpty(LevelAccessor world, BlockPos pos) {
        return !world.isEmptyBlock(pos);
    }

    /**
     * {@code initializeAsHull} also connects neighbour heat pipes. Those are already on the
     * controller via hull extras, so drop the duplicate link.
     */
    static void detachCapabilityNeighbors(Level level, IHeatExchangerLogic aux, BlockPos pos, Direction[] faces) {
        for (Direction face : faces) {
            IHeatExchangerLogic other = level.getCapability(
                    PNCCapabilities.HEAT_EXCHANGER_BLOCK, pos.relative(face), face.getOpposite());
            if (other != null) {
                aux.removeConnectedExchanger(other);
            }
        }
    }

    private static Direction directionOf(int dx, int dy, int dz) {
        if (dx == 1) {
            return Direction.EAST;
        }
        if (dx == -1) {
            return Direction.WEST;
        }
        if (dy == 1) {
            return Direction.UP;
        }
        if (dy == -1) {
            return Direction.DOWN;
        }
        if (dz == 1) {
            return Direction.SOUTH;
        }
        return Direction.NORTH;
    }
}
