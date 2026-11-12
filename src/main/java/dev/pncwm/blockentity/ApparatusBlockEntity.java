package dev.pncwm.blockentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.pncwm.WMConfig;
import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.apparatus.Family;
import dev.pncwm.block.ApparatusBlock;
import dev.pncwm.block.HatchBlock;
import dev.pncwm.block.HatchType;
import dev.pncwm.menu.ApparatusMenu;
import dev.pncwm.multiblock.FormedStructure;
import dev.pncwm.multiblock.StructureMatcher;
import dev.pncwm.pressure.MachineStatus;
import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import dev.pncwm.recipe.FluidSpec;
import dev.pncwm.recipe.ItemOut;
import dev.pncwm.recipe.SlotPlan;
import dev.pncwm.recipe.WindowRecipe;
import dev.pncwm.item.GradeItem;
import dev.pncwm.registry.WMBlockEntities;
import dev.pncwm.registry.WMItems;
import dev.pncwm.registry.WMRecipes;
import dev.pncwm.registry.WMSounds;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ApparatusBlockEntity extends AirMachine implements MenuProvider {
    public final MachineIo io = new MachineIo();
    private FormedStructure formed = FormedStructure.none();
    private final List<BlockPos> bound = new ArrayList<>();
    private MachineStatus status = MachineStatus.UNFORMED;
    private int progress;
    private int workTime;
    private int seal = 1000;
    private int fault;
    private int steady;
    private boolean certified;
    private final BlockPos[] watched = new BlockPos[3];
    private int setpoint = 2;
    private static final float[] SETPOINTS = {5f, 10f, 16f, 18f, 20f, 50f};
    private boolean scanned;
    private int lastSignal = -1;
    /** Recipe the current batch belongs to. Not saved: after a reload the first match keeps the saved progress. */
    @Nullable
    private WindowRecipe active;

    public ApparatusBlockEntity(BlockPos pos, BlockState state) {
        super(WMBlockEntities.APPARATUS.get(), pos, state);
        io.setListener(this::setChanged);
    }

    /**
     * Active recipe window for the GUI: 0 low bar*100, 1 high bar*100, 2 low C, 3 high C, 4 whether a window is known.
     */
    public int windowField(int index) {
        WindowRecipe recipe = active;
        if (recipe == null) {
            return 0;
        }
        return switch (index) {
            case 0 -> Math.round(recipe.pressureLow() * 100f);
            case 1 -> Math.round(recipe.pressureHigh() * 100f);
            case 2 -> recipe.tempLow();
            case 3 -> recipe.tempHigh();
            case 4 -> 1;
            default -> 0;
        };
    }

    /**
     * Comparator output. Recipe machines report batch progress (0 when nothing runs). Storage, columns, regulators and
     * other non-recipe machines report shell pressure against their rating.
     */
    public int comparatorSignal() {
        if (!formed.formed()) {
            return 0;
        }
        if (kind().usesRecipes()) {
            boolean running = status == MachineStatus.RUNNING || status == MachineStatus.WINDOW;
            return Tuning.progressSignal(progress, workTime, running);
        }
        if (air == null) {
            return 0;
        }
        return Tuning.pressureSignal(air.getPressure(), airTier().safeBar);
    }

    /** Spill every slot when the controller is broken, so machine contents are not deleted. */
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < MachineIo.SLOTS; slot++) {
            ItemStack stack = io.items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack.copy());
                io.items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        // Full buckets of whatever was in the tanks, including lubricant. Leftover millibuckets are lost.
        for (int tank = 0; tank < MachineIo.TANKS; tank++) {
            FluidStack fluid = io.tanks[tank].getFluid();
            int buckets = Tuning.bucketsFromMilli(fluid.getAmount());
            var bucketItem = fluid.isEmpty() ? Items.AIR : fluid.getFluid().getBucket();
            if (buckets > 0 && bucketItem != Items.AIR && bucketItem != Items.BUCKET) {
                for (int n = 0; n < buckets; n++) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(bucketItem));
                }
            }
            io.tanks[tank].setFluid(FluidStack.EMPTY);
        }
        for (BlockPos pos : bound) {
            if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) {
                hatch.unbind();
            }
        }
        bound.clear();
    }

    public ApparatusKind kind() {
        if (getBlockState().getBlock() instanceof ApparatusBlock block) {
            return block.kind;
        }
        return ApparatusKind.IMPACT_CRUSHER;
    }

    public MachineStatus status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int workTime() {
        return workTime;
    }

    public int seal() {
        return seal;
    }

    public FormedStructure formed() {
        return formed;
    }

    public boolean certified() {
        return certified;
    }

    public Direction facing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    @Override
    protected PressureGrade airTier() {
        if (!formed.formed()) {
            return kind().grade == PressureGrade.G5 ? PressureGrade.G4 : kind().grade;
        }
        PressureGrade shell = formed.weakest();
        if (kind() == ApparatusKind.COMPRESSOR_COLUMN) {
            return lower(shell, stageGrade(formed.size()));
        }
        return shell;
    }

    @Override
    protected int airVolume() {
        int size = Math.max(1, formed.size());
        return switch (kind().family) {
            case STORAGE -> 4000 + size * 8000;
            case COLUMN -> 3000 + size * 2000;
            case KETTLE, QUENCH -> 4000 + size * 2000;
            default -> 5000 + size * 400;
        };
    }

    /**
     * Compressor/expander columns and the regulator skid work across their back face: they pull from (or push into)
     * the block behind them. If that face also joined the air network, the shell simply equalized with the source and
     * the machine could not hold a different pressure. That face is now isolated from the network.
     */
    private boolean isolatedBack() {
        return kind().family == Family.COLUMN || kind().family == Family.REGULATOR;
    }

    @Override
    protected Collection<Direction> airFaces() {
        if (!isolatedBack()) {
            return List.of(Direction.values());
        }
        Direction back = facing().getOpposite();
        List<Direction> faces = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction != back) {
                faces.add(direction);
            }
        }
        return faces;
    }

    /** Air capability by side. The isolated back face of columns and regulators exposes nothing. */
    @Nullable
    public IAirHandlerMachine airOn(@Nullable Direction side) {
        if (side != null && isolatedBack() && side == facing().getOpposite()) {
            return null;
        }
        return air();
    }

    public void serverTick() {
        if (!scanned || level.getGameTime() % 10 == 0) {
            rescan();
            scanned = true;
        }
        tickPneumatics(true);
        if (air != null && air.getBaseVolume() != airVolume()) {
            air.setBaseVolume(airVolume());
        }
        if (!formed.formed()) {
            setStatus(MachineStatus.UNFORMED);
            progress = 0;
            workTime = 0;
            active = null;
            return;
        }
        switch (kind().family) {
            case COLUMN -> tickColumn();
            case REGULATOR -> tickRegulator();
            case STORAGE -> setStatus(MachineStatus.IDLE);
            case FLARE -> tickFlare();
            case MANIFOLD -> tickManifold();
            case HEAT_LINE -> tickHeatLine();
            case CHARGE -> tickCharge();
            case LOOP -> tickLoop();
            case CALIBRATION -> tickCalibration();
            default -> tickRecipe();
        }
        notifyComparators();
        if ((status == MachineStatus.RUNNING || status == MachineStatus.WINDOW) && level.getGameTime() % 40 == 0) {
            WMSounds.play(level, worldPosition, WMSounds.MACHINE_RUN.get(), SoundSource.BLOCKS, 0.28f, 1.0f);
        }
        if (level.getGameTime() % 10 == 0) {
            sync();
        }
    }

    public void clientTick() {
    }

    public void shiftUse(Player player) {
        if (status == MachineStatus.COKING || status == MachineStatus.FROZEN) {
            fault = 0;
            setStatus(MachineStatus.IDLE);
            player.displayClientMessage(Component.translatable("message.workingmedium.cleared"), true);
            return;
        }
        if (kind().family == Family.REGULATOR) {
            setpoint = (setpoint + 1) % SETPOINTS.length;
            player.displayClientMessage(Component.translatable("message.workingmedium.setpoint", SETPOINTS[setpoint]), true);
            setChanged();
            return;
        }
        Direction facing = facing().getClockWise();
        level.setBlock(worldPosition, getBlockState().setValue(HorizontalDirectionalBlock.FACING, facing), 3);
        // The isolated back face moved with the controller; refresh cached air capabilities and the heat hull.
        level.invalidateCapabilities(worldPosition);
        neighborChanged();
        rescan();
    }

    /**
     * Adds a machine to the calibration watch list. Returns false if that machine is already watched (or is this
     * bench), so one machine can no longer be bound three times to fake three steady machines.
     */
    public boolean watch(BlockPos pos) {
        if (pos.equals(worldPosition)) {
            return false;
        }
        for (BlockPos existing : watched) {
            if (pos.equals(existing)) {
                return false;
            }
        }
        for (int i = 0; i < watched.length; i++) {
            if (watched[i] == null) {
                watched[i] = pos.immutable();
                setChanged();
                return true;
            }
        }
        watched[0] = watched[1];
        watched[1] = watched[2];
        watched[2] = pos.immutable();
        setChanged();
        return true;
    }

    public IItemHandler hatchItems(HatchType type) {
        if (type == HatchType.ITEM_IN || type == HatchType.ITEM_OUT) {
            return io.itemsFor(type);
        }
        return null;
    }

    public IFluidHandler hatchFluids(HatchType type) {
        return io.fluidsFor(type);
    }

    public IItemHandler directItems() {
        return io.controllerItems();
    }

    public IFluidHandler directFluids() {
        return io.controllerFluids();
    }

    /** Right-click with a certificate: the machine uses 25% less air and the seal wears 25% slower. */
    public boolean applyCertificate() {
        if (!formed.formed() || certified) {
            return false;
        }
        certified = true;
        setChanged();
        sync();
        return true;
    }

    private void rescan() {
        FormedStructure next = StructureMatcher.match(level, worldPosition, facing(), kind());
        if (!sameShape(formed, next)) {
            for (BlockPos pos : bound) {
                if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) {
                    hatch.unbind();
                }
            }
            bound.clear();
            if (next.formed()) {
                for (BlockPos pos : next.hatches()) {
                    if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) {
                        hatch.bind(worldPosition);
                        bound.add(pos.immutable());
                    }
                }
            }
            formed = next;
            neighborChanged();
            setChanged();
        } else {
            formed = next;
        }
    }

    private void tickRecipe() {
        if (formed.flooded()) {
            setStatus(MachineStatus.FLOOD);
            return;
        }
        if (kind() == ApparatusKind.CLEAN_ROOM && (formed.airlockOpen() || air.getPressure() < 0.15f || air.getPressure() > 2.5f)) {
            if (progress > 0) {
                scrapWafer();
                progress = 0;
                setStatus(MachineStatus.SCRAPPED);
            } else {
                setStatus(MachineStatus.PRESSURE);
            }
            return;
        }
        WindowRecipe recipe = findRecipe();
        if (recipe == null) {
            setStatus(MachineStatus.IDLE);
            progress = 0;
            workTime = 0;
            active = null;
            return;
        }
        if (active != null && active != recipe) {
            // A different recipe took over (inputs changed). Do not finish it with the old batch's progress.
            progress = 0;
        }
        active = recipe;
        workTime = recipe.time;
        if (formed.size() < recipe.minSize || formed.coils() < recipe.requiredCoils()) {
            setStatus(MachineStatus.MISSING_PART);
            return;
        }
        int coilLimit = 250 + formed.coils() * 400;
        if (recipe.tempMinC > coilLimit) {
            setStatus(MachineStatus.TEMPERATURE);
            return;
        }
        float pressure = air.getPressure();
        int celsius = PressureGrade.kelvinToCelsius(heat.getTemperature());
        if (!recipe.accepts(pressure, celsius)) {
            boolean pressureOk = pressure >= recipe.pressureLow() && pressure <= recipe.pressureHigh();
            if (recipe.voidOutside && progress > 0) {
                progress = 0;
                setStatus(MachineStatus.SCRAPPED);
                return;
            }
            if (kind() == ApparatusKind.CRACKER && celsius > recipe.tempHigh()) {
                fault++;
                if (fault > 60) {
                    setStatus(MachineStatus.COKING);
                    return;
                }
            }
            if (kind() == ApparatusKind.CRYO_TOWER && celsius > recipe.tempHigh()) {
                fault++;
                if (fault > 80) {
                    setStatus(MachineStatus.FROZEN);
                    return;
                }
            }
            setStatus(!pressureOk ? MachineStatus.PRESSURE : MachineStatus.TEMPERATURE);
            return;
        }
        fault = 0;
        if (status == MachineStatus.COKING || status == MachineStatus.FROZEN) {
            return;
        }
        if (recipe.heatPerTick > 0 && !formed.vent() && formed.size() > 1) {
            setStatus(MachineStatus.NO_VENT);
            if (level.getGameTime() % 40 == 0 && level.random.nextFloat() < 0.15f * formed.size()) {
                level.destroyBlock(worldPosition.relative(facing()), true);
            }
            return;
        }
        // Do not wear the seal while the machine is clogged, dry or short of air.
        if (seal <= 0 && io.items.getStackInSlot(MachineIo.SEAL).isEmpty()) {
            setStatus(MachineStatus.LEAK);
            return;
        }
        if (recipe.lubeMb > 0 && level.getGameTime() % 20 == 0) {
            int lubeNeed = recipe.lubeMb * formed.parallel();
            FluidStack drained = io.tanks[MachineIo.LUBE].drain(lubeNeed, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() < lubeNeed) {
                heat.addHeat(30);
                setStatus(MachineStatus.NO_LUBE);
                return;
            }
            io.tanks[MachineIo.LUBE].drain(lubeNeed, IFluidHandler.FluidAction.EXECUTE);
        }
        if (!outputsFit(recipe)) {
            setStatus(MachineStatus.CLOGGED);
            return;
        }
        int airCost = Tuning.certifiedCost((int) (recipe.airPerTick * formed.parallel() * WMConfig.airMultiplier()), certified);
        if (air.getAir() < airCost) {
            setStatus(MachineStatus.NO_AIR);
            return;
        }
        air.addAir(-airCost);
        heat.addHeat(recipe.heatPerTick * formed.parallel());
        if (!wearSeal()) {
            setStatus(MachineStatus.LEAK);
            return;
        }
        progress++;
        setStatus(recipe.voidOutside ? MachineStatus.WINDOW : MachineStatus.RUNNING);
        if (progress >= recipe.time) {
            consume(recipe);
            produce(recipe);
            progress = 0;
            WMSounds.play(level, worldPosition, WMSounds.MACHINE_DONE.get(), SoundSource.BLOCKS, 0.55f, 1.0f);
        }
    }

    private void tickColumn() {
        int stages = formed.size();
        if (formed.missingIntercooler()) {
            heat.addHeat(40.0 * stages);
            if (PressureGrade.kelvinToCelsius(heat.getTemperature()) > 650) {
                air.setSideLeaking(facing());
                setStatus(MachineStatus.TEMPERATURE);
                return;
            }
        } else if (air.getSideLeaking() != null && seal > 400) {
            air.setSideLeaking(null);
        }
        Direction back = facing().getOpposite();
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(back));
        var neighborAir = neighbor == null ? null : me.desht.pneumaticcraft.api.PNCCapabilities.getAirHandler(neighbor, facing()).orElse(null);
        if (neighborAir == null) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        if (kind() == ApparatusKind.COMPRESSOR_COLUMN) {
            int produced = Tuning.certifiedCost(40 * stages, certified);
            int take = produced * 3;
            if (neighborAir.getAir() < take) {
                setStatus(MachineStatus.NO_AIR);
                return;
            }
            neighborAir.addAir(-take);
            air.addAir((int) (produced * (formed.missingIntercooler() ? 0.45f : 0.8f)));
            heat.addHeat(produced * 0.15);
            setStatus(MachineStatus.RUNNING);
        } else {
            int take = Tuning.certifiedCost(80 * stages, certified);
            if (air.getAir() < take) {
                setStatus(MachineStatus.NO_AIR);
                return;
            }
            air.addAir(-take);
            neighborAir.addAir(take / 3);
            heat.addHeat(formed.missingIntercooler() ? 10 : -take * 0.25);
            setStatus(MachineStatus.RUNNING);
        }
    }

    private void tickRegulator() {
        float target = SETPOINTS[setpoint];
        Direction back = facing().getOpposite();
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(back));
        var source = neighbor == null ? null : me.desht.pneumaticcraft.api.PNCCapabilities.getAirHandler(neighbor, facing()).orElse(null);
        if (source == null) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        if (air.getPressure() < target && source.getPressure() > target + 0.3f) {
            int move = Math.min(500, source.getAir() / 8);
            source.addAir(-move);
            air.addAir((int) (move * 0.8f));
            heat.addHeat(move * 0.05);
        }
        int bleed = Tuning.regulatorBleed(air.getPressure(), target, air.getVolume());
        if (bleed > 0) {
            // Over the setpoint (setpoint lowered, or back-fed by a downstream line): bleed down instead of sitting high.
            air.addAir(-bleed);
            if (level.getGameTime() % 40 == 0) {
                WMSounds.play(level, worldPosition, WMSounds.VENT.get(), SoundSource.BLOCKS, 0.35f, 1.1f);
            }
        }
        setStatus(Math.abs(air.getPressure() - target) < 1.2f ? MachineStatus.WINDOW : MachineStatus.RUNNING);
    }

    private void tickFlare() {
        int rate = 20 * formed.size();
        boolean burned = false;
        boolean blocked = false;
        for (int i = 0; i < MachineIo.INPUTS; i++) {
            FluidStack fluid = io.tanks[i].getFluid();
            if (fluid.isEmpty()) {
                continue;
            }
            if (!isLightGas(fluid)) {
                blocked = true;
                continue;
            }
            io.tanks[i].drain(rate, IFluidHandler.FluidAction.EXECUTE);
            burned = true;
        }
        setStatus(blocked && !burned ? MachineStatus.CLOGGED : burned ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    private void tickManifold() {
        if (level.getGameTime() % 8 != 0) {
            return;
        }
        ItemStack filter = io.items.getStackInSlot(MachineIo.FILTER);
        int moved = 0;
        int limit = Math.max(1, formed.size() * upgradeMultiplier("_slide"));
        for (int i = 0; i < MachineIo.INPUTS && moved < limit; i++) {
            ItemStack stack = io.items.getStackInSlot(i);
            if (stack.isEmpty() || (!filter.isEmpty() && !ItemStack.isSameItem(filter, stack))) {
                continue;
            }
            ItemStack one = stack.copyWithCount(1);
            for (int out = 0; out < MachineIo.INPUTS; out++) {
                ItemStack leftover = io.items.insertItem(MachineIo.INPUTS + out, one, false);
                if (leftover.isEmpty()) {
                    stack.shrink(1);
                    moved++;
                    break;
                }
            }
        }
        setStatus(moved > 0 ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    private void tickHeatLine() {
        heat.setThermalResistance(2.0 + formed.size() * 2.5);
        setStatus(MachineStatus.RUNNING);
    }

    private void tickCharge() {
        int budget = Tuning.certifiedCost(200, certified);
        boolean charged = false;
        for (int i = 0; i < MachineIo.INPUTS && budget > 0; i++) {
            ItemStack stack = io.items.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            var handler = stack.getCapability(me.desht.pneumaticcraft.api.PNCCapabilities.AIR_HANDLER_ITEM);
            if (handler != null) {
                // Stop at the item's own rating instead of pushing it past its maximum pressure.
                if (air.getAir() > 500 && handler.getPressure() < handler.maxPressure() - 0.05f) {
                    int give = Math.min(budget, 100);
                    handler.addAir(give);
                    air.addAir(-give);
                    budget -= give;
                    charged = true;
                }
            } else if (stack.is(WMItems.DRILL.get())) {
                int charge = stack.getOrDefault(dev.pncwm.registry.WMData.CHARGE, 0);
                int heatLevel = stack.getOrDefault(dev.pncwm.registry.WMData.HEAT, 0);
                int cost = Tuning.certifiedCost(800, certified);
                if (charge < 100 && air.getAir() > cost) {
                    stack.set(dev.pncwm.registry.WMData.CHARGE, charge + 1);
                    air.addAir(-cost);
                    if (heatLevel > 0) {
                        stack.set(dev.pncwm.registry.WMData.HEAT, heatLevel - 1);
                    }
                    charged = true;
                }
            }
        }
        boolean blocked = false;
        for (int i = 0; i < MachineIo.INPUTS; i++) {
            ItemStack stack = io.items.getStackInSlot(i);
            if (!stack.isEmpty() && fullyCharged(stack) && !moveToOutput(i)) {
                blocked = true;
            }
        }
        setStatus(charged ? MachineStatus.RUNNING : blocked ? MachineStatus.CLOGGED : MachineStatus.IDLE);
    }

    private void tickLoop() {
        if (air.getPressure() < 70f || air.getPressure() > 80f) {
            setStatus(MachineStatus.PRESSURE);
            return;
        }
        Direction facing = facing();
        IFluidHandler back = fluidNeighbor(worldPosition.relative(facing.getOpposite()), facing);
        IFluidHandler front = fluidNeighbor(worldPosition.relative(facing), facing.getOpposite());
        if (back == null || front == null) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        FluidStack drained = back.drain(50 * formed.size() * upgradeMultiplier("_pump"), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        int filled = front.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) {
            setStatus(MachineStatus.CLOGGED);
            return;
        }
        back.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        setStatus(MachineStatus.WINDOW);
    }

    private void tickCalibration() {
        List<Long> steadyMachines = new ArrayList<>();
        for (BlockPos pos : watched) {
            if (pos != null && !pos.equals(worldPosition) && level.getBlockEntity(pos) instanceof ApparatusBlockEntity other
                    && (other.status == MachineStatus.RUNNING || other.status == MachineStatus.WINDOW)) {
                steadyMachines.add(pos.asLong());
            }
        }
        // Old saves could hold the same machine twice; it still only counts once.
        int ready = Tuning.distinct(steadyMachines);
        if (ready >= 3) {
            steady++;
            setStatus(MachineStatus.WINDOW);
            if (steady >= WMConfig.CERTIFICATION_TICKS.get()) {
                // Each completed watch period writes another certificate. The bench is not limited to one.
                steady = 0;
                insertOutput(new ItemStack(WMItems.CERTIFICATE.get()));
                setStatus(MachineStatus.CERTIFIED);
            }
        } else {
            steady = 0;
            setStatus(ready == 0 ? MachineStatus.IDLE : MachineStatus.PRESSURE);
        }
    }

    private WindowRecipe findRecipe() {
        if (level == null) {
            return null;
        }
        WindowRecipe fallback = null;
        for (RecipeHolder<WindowRecipe> holder : level.getRecipeManager().getAllRecipesFor(WMRecipes.WINDOW.get())) {
            WindowRecipe recipe = holder.value();
            if (!recipe.machine.equals(kind().id)) {
                continue;
            }
            if (formed.size() < recipe.minSize) {
                continue;
            }
            if (!hasItems(recipe) || !hasFluids(recipe)) {
                continue;
            }
            if (fallback == null || moreSpecific(recipe, fallback)) {
                fallback = recipe;
            }
        }
        return fallback;
    }

    private static final int[] INGREDIENT_SLOTS = {0, 1, 2, 3, MachineIo.CATALYST, MachineIo.SPECIAL};

    private boolean hasItems(WindowRecipe recipe) {
        boolean[] used = new boolean[MachineIo.SLOTS];
        for (int ingredient = 0; ingredient < recipe.items.size(); ingredient++) {
            Ingredient test = recipe.items.get(ingredient);
            boolean held = recipe.consumeCount(ingredient) <= 0;
            int need = held ? recipe.presentCount(ingredient) : recipe.presentCount(ingredient) * formed.parallel();
            boolean found = false;
            for (int slot : INGREDIENT_SLOTS) {
                if (used[slot]) {
                    continue;
                }
                ItemStack stack = io.items.getStackInSlot(slot);
                if (!stack.isEmpty() && test.test(stack) && stack.getCount() >= need) {
                    used[slot] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFluids(WindowRecipe recipe) {
        for (FluidSpec spec : recipe.fluids) {
            FluidStack need = spec.stack(formed.parallel());
            if (need.isEmpty()) {
                continue;
            }
            int have = 0;
            for (int i = 0; i < MachineIo.INPUTS; i++) {
                if (sameFluid(io.tanks[i].getFluid(), need)) {
                    have += io.tanks[i].getFluidAmount();
                }
            }
            if (have < need.getAmount()) {
                return false;
            }
        }
        return true;
    }

    /** All outputs of one batch must fit together; checking them one by one let a second output overflow and vanish. */
    private boolean outputsFit(WindowRecipe recipe) {
        List<ItemStack> outItems = new ArrayList<>();
        for (ItemOut out : recipe.itemOutputs) {
            if (formed.size() >= out.minSize()) {
                ItemStack stack = out.stack(formed.parallel());
                if (!stack.isEmpty()) {
                    outItems.add(stack);
                }
            }
        }
        if (kind() == ApparatusKind.POLYMERIZER && io.items.getStackInSlot(MachineIo.CATALYST).isEmpty()) {
            outItems.add(new ItemStack(WMItems.SLAG.get()));
        }
        List<ItemStack> slotItems = new ArrayList<>();
        int[] itemCounts = new int[MachineIo.INPUTS];
        for (int i = 0; i < MachineIo.INPUTS; i++) {
            ItemStack held = io.items.getStackInSlot(MachineIo.INPUTS + i);
            slotItems.add(held.isEmpty() ? null : held);
            itemCounts[i] = held.getCount();
        }
        int[] outItemCounts = outItems.stream().mapToInt(ItemStack::getCount).toArray();
        int slotLimit = io.items.getSlotLimit(MachineIo.INPUTS);
        if (!SlotPlan.fitsAll(slotItems, itemCounts, stack -> Math.min(slotLimit, stack.getMaxStackSize()),
                outItems, outItemCounts, ItemStack::isSameItemSameComponents)) {
            return false;
        }
        List<FluidStack> outFluids = new ArrayList<>();
        for (FluidSpec spec : recipe.fluidOutputs) {
            if (formed.size() >= spec.minSize()) {
                FluidStack stack = spec.stack(formed.parallel());
                if (!stack.isEmpty()) {
                    outFluids.add(stack);
                }
            }
        }
        List<FluidStack> tankFluids = new ArrayList<>();
        int[] tankCounts = new int[MachineIo.INPUTS];
        for (int i = 0; i < MachineIo.INPUTS; i++) {
            FluidStack held = io.tanks[MachineIo.INPUTS + i].getFluid();
            tankFluids.add(held.isEmpty() ? null : held);
            tankCounts[i] = held.getAmount();
        }
        int capacity = io.tanks[MachineIo.INPUTS].getCapacity();
        int[] outFluidCounts = outFluids.stream().mapToInt(FluidStack::getAmount).toArray();
        return SlotPlan.fitsAll(tankFluids, tankCounts, stack -> capacity, outFluids, outFluidCounts,
                ApparatusBlockEntity::sameFluid);
    }

    private void consume(WindowRecipe recipe) {
        boolean[] used = new boolean[MachineIo.SLOTS];
        for (int ingredient = 0; ingredient < recipe.items.size(); ingredient++) {
            Ingredient test = recipe.items.get(ingredient);
            int need = recipe.consumeCount(ingredient) * formed.parallel();
            if (need <= 0) {
                continue;
            }
            for (int slot : INGREDIENT_SLOTS) {
                if (used[slot]) {
                    continue;
                }
                ItemStack stack = io.items.getStackInSlot(slot);
                if (!stack.isEmpty() && test.test(stack) && stack.getCount() >= need) {
                    stack.shrink(need);
                    used[slot] = true;
                    break;
                }
            }
        }
        for (FluidSpec spec : recipe.fluids) {
            int left = spec.stack(formed.parallel()).getAmount();
            Fluid fluid = spec.resolve();
            for (int i = 0; i < MachineIo.INPUTS && left > 0; i++) {
                if (io.tanks[i].getFluid().getFluid() == fluid) {
                    FluidStack drained = io.tanks[i].drain(left, IFluidHandler.FluidAction.EXECUTE);
                    left -= drained.getAmount();
                }
            }
        }
        if (kind() == ApparatusKind.ELECTROLYZER) {
            ItemStack electrode = io.items.getStackInSlot(MachineIo.SPECIAL);
            if (!electrode.isEmpty()) {
                electrode.shrink(1);
            }
        }
    }

    private void scrapWafer() {
        for (int slot = 0; slot < MachineIo.INPUTS; slot++) {
            ItemStack stack = io.items.getStackInSlot(slot);
            if (stack.is(WMItems.MICROFLUIDIC_CHIP.get())) {
                stack.shrink(1);
                insertOutput(new ItemStack(WMItems.WASTE_CHIP.get()));
                return;
            }
        }
    }

    private void produce(WindowRecipe recipe) {
        for (ItemOut out : recipe.itemOutputs) {
            if (formed.size() < out.minSize()) {
                continue;
            }
            insertOutput(out.stack(formed.parallel()));
        }
        for (FluidSpec spec : recipe.fluidOutputs) {
            if (formed.size() < spec.minSize()) {
                continue;
            }
            fillOutput(spec.stack(formed.parallel()));
        }
        if (kind() == ApparatusKind.POLYMERIZER && io.items.getStackInSlot(MachineIo.CATALYST).isEmpty()) {
            insertOutput(new ItemStack(WMItems.SLAG.get()));
        }
    }

    private void insertOutput(ItemStack stack) {
        ItemStack remain = stack;
        for (int i = 0; i < MachineIo.INPUTS && !remain.isEmpty(); i++) {
            remain = io.items.insertItem(MachineIo.INPUTS + i, remain, false);
        }
        if (!remain.isEmpty() && level != null) {
            // Certificates, scrap wafers and similar extras are never deleted: spill them on top of the controller.
            Block.popResource(level, worldPosition.above(), remain);
        }
    }

    private void fillOutput(FluidStack stack) {
        FluidStack remain = stack.copy();
        for (int i = 0; i < MachineIo.INPUTS && !remain.isEmpty(); i++) {
            int filled = io.tanks[MachineIo.INPUTS + i].fill(remain, IFluidHandler.FluidAction.EXECUTE);
            remain.shrink(filled);
        }
    }

    private boolean wearSeal() {
        seal -= Tuning.sealWear(WMConfig.leakMultiplier(), certified);
        ItemStack sealStack = io.items.getStackInSlot(MachineIo.SEAL);
        if (seal < Tuning.SEAL_SWAP_AT && !sealStack.isEmpty()) {
            int gradeIndex = sealStack.getItem() instanceof GradeItem gradeItem ? gradeItem.grade.index : 2;
            sealStack.shrink(1);
            seal = Tuning.sealLife(gradeIndex);
            air.setSideLeaking(null);
            setChanged();
        }
        if (seal < 400) {
            air.setSideLeaking(facing());
        }
        if (seal <= 0) {
            seal = 0;
            return false;
        }
        return true;
    }

    private void setStatus(MachineStatus next) {
        if (status != next) {
            boolean started = next == MachineStatus.RUNNING || next == MachineStatus.WINDOW;
            boolean wasRunning = status == MachineStatus.RUNNING || status == MachineStatus.WINDOW;
            status = next;
            setChanged();
            if (started && !wasRunning) {
                WMSounds.play(level, worldPosition, WMSounds.MACHINE_RUN.get(), SoundSource.BLOCKS, 0.45f, 1.0f);
            }
        }
    }

    @Nullable
    private IFluidHandler fluidNeighbor(BlockPos pos, Direction side) {
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
    }

    private static PressureGrade stageGrade(int stages) {
        return switch (Math.max(1, Math.min(4, stages))) {
            case 1 -> PressureGrade.G1;
            case 2 -> PressureGrade.G2;
            case 3 -> PressureGrade.G3;
            default -> PressureGrade.G4;
        };
    }

    private static PressureGrade lower(PressureGrade a, PressureGrade b) {
        return a.safeBar <= b.safeBar ? a : b;
    }

    private static boolean sameFluid(FluidStack left, FluidStack right) {
        return !left.isEmpty() && !right.isEmpty() && left.getFluid() == right.getFluid();
    }

    private static boolean isLightGas(FluidStack stack) {
        return MachineIo.isGas(stack);
    }

    /** Vane pump (fluid loop) or slide (item gallery) in the special slot. Missing part leaves the base rate of 1. */
    private int upgradeMultiplier(String suffix) {
        ItemStack stack = io.items.getStackInSlot(MachineIo.SPECIAL);
        if (stack.isEmpty() || !(stack.getItem() instanceof GradeItem gradeItem)) {
            return 1;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return Tuning.partMultiplier(path, suffix, gradeItem.grade.index);
    }

    private boolean fullyCharged(ItemStack stack) {
        var handler = stack.getCapability(me.desht.pneumaticcraft.api.PNCCapabilities.AIR_HANDLER_ITEM);
        if (handler != null) {
            return handler.getPressure() >= handler.maxPressure() - 0.05f;
        }
        if (stack.is(WMItems.DRILL.get())) {
            return stack.getOrDefault(dev.pncwm.registry.WMData.CHARGE, 0) >= 100;
        }
        return false;
    }

    /** Move a finished item from an input slot into the output row. Returns false if it would not all fit. */
    private boolean moveToOutput(int slot) {
        ItemStack taken = io.items.extractItem(slot, 64, false);
        if (taken.isEmpty()) {
            return true;
        }
        ItemStack remain = taken;
        for (int out = 0; out < MachineIo.INPUTS && !remain.isEmpty(); out++) {
            remain = io.items.insertItem(MachineIo.INPUTS + out, remain, false);
        }
        if (!remain.isEmpty()) {
            io.items.insertItem(slot, remain, false);
            return false;
        }
        return true;
    }

    private void notifyComparators() {
        if (level == null || level.isClientSide) {
            return;
        }
        int signal = comparatorSignal();
        if (signal == lastSignal) {
            return;
        }
        lastSignal = signal;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        for (BlockPos pos : bound) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof HatchBlock hatch && hatch.type == HatchType.GAUGE) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }
    }

    private static boolean moreSpecific(WindowRecipe recipe, WindowRecipe fallback) {
        if (recipe.minSize != fallback.minSize) {
            return recipe.minSize > fallback.minSize;
        }
        int left = recipe.items.size() + recipe.fluids.size();
        int right = fallback.items.size() + fallback.fluids.size();
        return left > right;
    }

    private static boolean sameShape(FormedStructure left, FormedStructure right) {
        return left.size() == right.size() && left.formed() == right.formed() && left.hatches().equals(right.hatches());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.workingmedium." + kind().id);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ApparatusMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        savePneumatics(tag);
        io.save(tag, registries);
        tag.putInt("Progress", progress);
        tag.putInt("WorkTime", workTime);
        tag.putInt("Seal", seal);
        tag.putInt("Status", status.ordinal());
        tag.putInt("Fault", fault);
        tag.putInt("Steady", steady);
        tag.putBoolean("Certified", certified);
        tag.putInt("Setpoint", setpoint);
        for (int i = 0; i < watched.length; i++) {
            if (watched[i] != null) {
                tag.putLong("Watch" + i, watched[i].asLong());
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadPneumatics(tag);
        io.load(tag, registries);
        readProcess(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Progress", progress);
        tag.putInt("WorkTime", workTime);
        tag.putInt("Seal", seal);
        tag.putInt("Status", status.ordinal());
        tag.putInt("Size", formed.size());
        tag.putBoolean("Formed", formed.formed());
        tag.putBoolean("Certified", certified);
        if (air != null) {
            tag.putFloat("Bar", air.getPressure());
        }
        if (heat != null) {
            tag.putInt("Celsius", PressureGrade.kelvinToCelsius(heat.getTemperature()));
        }
        CompoundTag fluids = new CompoundTag();
        for (int i = 0; i < MachineIo.TANKS; i++) {
            fluids.put("T" + i, io.tanks[i].writeToNBT(registries, new CompoundTag()));
        }
        tag.put("GuiFluids", fluids);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        progress = tag.getInt("Progress");
        workTime = tag.getInt("WorkTime");
        seal = tag.getInt("Seal");
        status = MachineStatus.byOrdinal(tag.getInt("Status"));
        certified = tag.getBoolean("Certified");
        if (tag.get("GuiFluids") instanceof CompoundTag fluids) {
            for (int i = 0; i < MachineIo.TANKS; i++) {
                if (fluids.get("T" + i) instanceof CompoundTag tank) {
                    io.tanks[i].readFromNBT(registries, tank);
                }
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void readProcess(CompoundTag tag) {
        progress = tag.getInt("Progress");
        workTime = tag.getInt("WorkTime");
        seal = tag.contains("Seal") ? tag.getInt("Seal") : 1000;
        status = MachineStatus.byOrdinal(tag.getInt("Status"));
        fault = tag.getInt("Fault");
        steady = tag.getInt("Steady");
        certified = tag.getBoolean("Certified");
        setpoint = Math.floorMod(tag.getInt("Setpoint"), SETPOINTS.length);
        for (int i = 0; i < watched.length; i++) {
            if (tag.contains("Watch" + i)) {
                watched[i] = BlockPos.of(tag.getLong("Watch" + i));
            }
        }
    }
}
