package dev.pncwm.blockentity;

import dev.pncwm.block.HatchType;
import dev.pncwm.pressure.Tuning;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Slots 0-3 inputs, 4-7 outputs, 8 seal, 9 catalyst, 10 electrode, program, pump or slide, 11 filter.
 * Tanks 0-3 fluid inputs, 4-7 fluid outputs, 8 lubricant.
 */
public final class MachineIo {
    public static final int SLOTS = 12;
    public static final int TANKS = 9;
    public static final int INPUTS = 4;
    public static final int SEAL = 8;
    public static final int CATALYST = 9;
    public static final int SPECIAL = 10;
    public static final int FILTER = 11;
    public static final int LUBE = 8;

    public final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    public final FluidTank[] tanks = new FluidTank[TANKS];
    private Runnable listener = () -> {
    };

    public MachineIo() {
        for (int i = 0; i < TANKS; i++) {
            tanks[i] = new FluidTank(16_000) {
                @Override
                protected void onContentsChanged() {
                    changed();
                }
            };
        }
    }

    /** Called whenever a slot or tank changes, so the owning block entity is saved with its contents. */
    public void setListener(Runnable listener) {
        this.listener = listener == null ? () -> {
        } : listener;
    }

    private void changed() {
        listener.run();
    }

    /** Lubricant always goes to its own tank, whichever face or hatch it arrives through. */
    public static boolean isLube(FluidStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.FLUID.getKey(stack.getFluid()).getPath().equals("lubricant");
    }

    public static boolean isGas(FluidStack stack) {
        return !stack.isEmpty() && Tuning.isGasPath(BuiltInRegistries.FLUID.getKey(stack.getFluid()).getPath());
    }

    /**
     * Seal, catalyst, electrode, drone-sheet and upgrade parts (pump, slide, nozzle, cylinder, diaphragm, gripper) have a dedicated slot.
     * Returns -1 for everything else.
     */
    public static int dedicatedSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"workingmedium".equals(id.getNamespace())) {
            return -1;
        }
        return Tuning.dedicatedSlotForPath(id.getPath());
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Items", items.serializeNBT(registries));
        CompoundTag fluids = new CompoundTag();
        for (int i = 0; i < TANKS; i++) {
            fluids.put("T" + i, tanks[i].writeToNBT(registries, new CompoundTag()));
        }
        tag.put("Fluids", fluids);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.get("Items") instanceof CompoundTag compound) {
            items.deserializeNBT(registries, compound);
        }
        if (tag.get("Fluids") instanceof CompoundTag fluids) {
            for (int i = 0; i < TANKS; i++) {
                if (fluids.get("T" + i) instanceof CompoundTag tank) {
                    tanks[i].readFromNBT(registries, tank);
                }
            }
        }
    }

    /**
     * Sided handler for the controller itself. Hoppers may only insert into input (or dedicated) slots
     * and may only extract from output slots, so they cannot steal ingredients or jam products into the output row.
     */
    public IItemHandler controllerItems() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return INPUTS * 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot >= INPUTS) {
                    return stack;
                }
                int dedicated = dedicatedSlot(stack);
                if (dedicated >= 0) {
                    return items.insertItem(dedicated, stack, simulate);
                }
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < INPUTS) {
                    return ItemStack.EMPTY;
                }
                return items.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= INPUTS) {
                    return false;
                }
                int dedicated = dedicatedSlot(stack);
                return dedicated < 0 || dedicated >= SEAL;
            }
        };
    }

    public IItemHandler itemsFor(HatchType type) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return INPUTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(shift(slot));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (type != HatchType.ITEM_IN) {
                    return stack;
                }
                int dedicated = dedicatedSlot(stack);
                if (dedicated >= 0) {
                    // Seals, catalyst, electrodes, drone sheets, pumps and slides fed by a hopper go to their own slot.
                    return items.insertItem(dedicated, stack, simulate);
                }
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (type != HatchType.ITEM_OUT) {
                    return ItemStack.EMPTY;
                }
                return items.extractItem(slot + INPUTS, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(shift(slot));
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return type == HatchType.ITEM_IN;
            }

            private int shift(int slot) {
                return type == HatchType.ITEM_OUT ? slot + INPUTS : slot;
            }
        };
    }

    public IFluidHandler fluidsFor(HatchType type) {
        boolean drainOnly = type == HatchType.DRAIN;
        boolean gasOrFluid = type == HatchType.FLUID || type == HatchType.GAS || type == HatchType.DRAIN;
        if (!gasOrFluid) {
            return null;
        }
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return Tuning.hatchTankViews(drainOnly, type == HatchType.GAS, type == HatchType.FLUID);
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                int index = viewTank(tank);
                return index < 0 ? FluidStack.EMPTY : tanks[index].getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                int index = viewTank(tank);
                return index < 0 ? 0 : tanks[index].getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                int index = viewTank(tank);
                // Only the input row accepts a fill; lubricant still redirects in fill().
                if (index < 0 || index >= INPUTS) {
                    return false;
                }
                return accepts(type, stack);
            }

            private int viewTank(int tank) {
                return Tuning.hatchTankIndex(drainOnly, type == HatchType.GAS, type == HatchType.FLUID, tank);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (drainOnly || resource.isEmpty() || !accepts(type, resource)) {
                    return 0;
                }
                if (isLube(resource)) {
                    return tanks[LUBE].fill(resource, action);
                }
                return fillInputs(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !acceptsOutput(type, resource)) {
                    return FluidStack.EMPTY;
                }
                return drainMatching(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drainAny(maxDrain, action, type);
            }
        };
    }

    /** Controller-face fluid handler: fill inputs (or the lube tank), drain only the matching output fluid. */
    public IFluidHandler controllerFluids() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return TANKS;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tanks[tank].getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tanks[tank].getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank < INPUTS || tank == LUBE;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return 0;
                }
                if (isLube(resource)) {
                    return tanks[LUBE].fill(resource, action);
                }
                return fillInputs(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return drainMatching(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drainAny(maxDrain, action);
            }
        };
    }

    /**
     * A gas hatch only accepts gases (plus lubricant, so a lube line can still land in the lube tank).
     * A fluid hatch only accepts liquids, including lubricant.
     */
    public static boolean accepts(HatchType type, FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isLube(stack)) {
            return true;
        }
        boolean gas = isGas(stack);
        if (type == HatchType.GAS) {
            return gas;
        }
        if (type == HatchType.FLUID) {
            return !gas;
        }
        return true;
    }

    private int fillInputs(FluidStack resource, IFluidHandler.FluidAction action) {
        int filled = 0;
        FluidStack left = resource.copy();
        for (int i = 0; i < INPUTS && !left.isEmpty(); i++) {
            if (tanks[i].getFluid().isEmpty() || tanks[i].getFluid().is(resource.getFluid())) {
                int step = tanks[i].fill(left, action);
                filled += step;
                left.shrink(step);
            }
        }
        return filled;
    }

    private FluidStack drainMatching(FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int i = 0; i < INPUTS; i++) {
            FluidStack drained = tanks[i + INPUTS].drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        if (isLube(resource)) {
            return tanks[LUBE].drain(resource, action);
        }
        return FluidStack.EMPTY;
    }

    private FluidStack drainAny(int maxDrain, IFluidHandler.FluidAction action) {
        return drainAny(maxDrain, action, null);
    }

    private FluidStack drainAny(int maxDrain, IFluidHandler.FluidAction action, HatchType type) {
        for (int i = 0; i < INPUTS; i++) {
            FluidStack held = tanks[i + INPUTS].getFluid();
            if (held.isEmpty() || (type != null && !acceptsOutput(type, held))) {
                continue;
            }
            FluidStack drained = tanks[i + INPUTS].drain(maxDrain, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        boolean gas = type == HatchType.GAS;
        boolean fluid = type == HatchType.FLUID;
        boolean drain = type == HatchType.DRAIN;
        if (Tuning.mayDrainLube(drain, gas, fluid, type == null) && !tanks[LUBE].getFluid().isEmpty()) {
            return tanks[LUBE].drain(maxDrain, action);
        }
        return FluidStack.EMPTY;
    }

    /**
     * Drain-side filter for a hatch. A gas hatch only yields gases, a fluid hatch only yields liquids,
     * and a drain hatch yields whatever is in the output tanks. Lubricant is drained separately after
     * the product tanks, and only through a fluid hatch, a drain hatch or the controller.
     */
    public static boolean acceptsOutput(HatchType type, FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.FLUID.getKey(stack.getFluid()).getPath();
        return Tuning.hatchMayDrain(type == HatchType.DRAIN, type == HatchType.GAS, type == HatchType.FLUID, path);
    }
}
