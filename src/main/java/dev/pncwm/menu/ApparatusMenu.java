package dev.pncwm.menu;

import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.blockentity.MachineIo;
import dev.pncwm.registry.WMData;
import dev.pncwm.registry.WMMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class ApparatusMenu extends AbstractContainerMenu {
    /** progress, status, seal, size, bar*100, celsius, work time, then the active window: low/high bar*100, low/high C, known flag. */
    public static final int FIELDS = 12;
    public final ApparatusBlockEntity apparatus;
    private final ContainerData data;

    public ApparatusMenu(int id, Inventory inventory, ApparatusBlockEntity apparatus) {
        super(WMMenus.APPARATUS.get(), id);
        this.apparatus = apparatus;
        this.data = new ContainerData() {
            private final int[] fields = new int[FIELDS];

            @Override
            public int get(int index) {
                if (apparatus != null && apparatus.getLevel() != null && !apparatus.getLevel().isClientSide()) {
                    return switch (index) {
                        case 0 -> apparatus.progress();
                        case 1 -> apparatus.status().ordinal();
                        case 2 -> apparatus.seal();
                        case 3 -> apparatus.formed().size();
                        case 4 -> apparatus.air() == null ? 0 : (int) (apparatus.air().getPressure() * 100);
                        case 5 -> apparatus.heat() == null ? 0 : dev.pncwm.pressure.PressureGrade.kelvinToCelsius(apparatus.heat().getTemperature());
                        case 6 -> apparatus.workTime();
                        case 7 -> apparatus.windowField(0);
                        case 8 -> apparatus.windowField(1);
                        case 9 -> apparatus.windowField(2);
                        case 10 -> apparatus.windowField(3);
                        case 11 -> apparatus.windowField(4);
                        default -> 0;
                    };
                }
                return index >= 0 && index < fields.length ? fields[index] : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < fields.length) {
                    fields[index] = value;
                }
            }

            @Override
            public int getCount() {
                return FIELDS;
            }
        };
        addSlots(inventory);
        addDataSlots(data);
    }

    public ApparatusMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, read(inventory, buf.readBlockPos()));
    }

    private static ApparatusBlockEntity read(Inventory inventory, BlockPos pos) {
        Level level = inventory.player.level();
        if (level.getBlockEntity(pos) instanceof ApparatusBlockEntity apparatus) {
            return apparatus;
        }
        return null;
    }

    private void addSlots(Inventory inventory) {
        if (apparatus == null) {
            return;
        }
        var container = new net.minecraft.world.Container() {
                @Override
                public int getContainerSize() {
                    return MachineIo.SLOTS;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public ItemStack getItem(int slot) {
                    return apparatus.io.items.getStackInSlot(slot);
                }

                @Override
                public ItemStack removeItem(int slot, int amount) {
                    return apparatus.io.items.extractItem(slot, amount, false);
                }

                @Override
                public ItemStack removeItemNoUpdate(int slot) {
                    ItemStack stack = getItem(slot);
                    apparatus.io.items.setStackInSlot(slot, ItemStack.EMPTY);
                    return stack;
                }

                @Override
                public void setItem(int slot, ItemStack stack) {
                    apparatus.io.items.setStackInSlot(slot, stack);
                }

                @Override
                public void setChanged() {
                    apparatus.setChanged();
                }

                @Override
                public boolean stillValid(Player player) {
                    return true;
                }

                @Override
                public void clearContent() {
                }
        };
        for (int i = 0; i < MachineIo.SLOTS; i++) {
            int x = 8 + (i % 4) * 18;
            int y = 18 + (i / 4) * 18;
            if (i == MachineIo.FILTER) {
                addSlot(new FilterSlot(container, i, x, y));
            } else {
                addSlot(new MachineSlot(container, i, x, y));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 164));
        }
    }

    public int data(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (apparatus == null || id < 0 || id >= MachineIo.TANKS) {
            return false;
        }
        FluidTank tank = apparatus.io.tanks[id];
        ItemStack carried = getCarried();
        boolean inlet = id < MachineIo.INPUTS || id == MachineIo.LUBE;
        if (inlet) {
            FluidActionResult emptied = FluidUtil.tryEmptyContainer(carried, tank, 1000, player, true);
            if (emptied.isSuccess()) {
                setCarried(emptied.getResult());
                apparatus.setChanged();
                return true;
            }
        }
        FluidActionResult filled = FluidUtil.tryFillContainer(carried, tank, 1000, player, true);
        if (filled.isSuccess()) {
            setCarried(filled.getResult());
            apparatus.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = MachineIo.SLOTS;
        int playerEnd = slots.size();
        if (index == MachineIo.FILTER) {
            if (isGhost(stack)) {
                slot.set(ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }
        }
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!acceptMachineItem(stack)) {
            int hotbar = playerStart + 27;
            if (index < hotbar) {
                if (!moveItemStackTo(stack, hotbar, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, playerStart, hotbar, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean acceptMachineItem(ItemStack stack) {
        int dedicated = dedicatedSlot(stack);
        if (dedicated >= 0 && moveItemStackTo(stack, dedicated, dedicated + 1, false)) {
            return true;
        }
        if (dedicated >= 0) {
            return false;
        }
        return moveItemStackTo(stack, 0, MachineIo.INPUTS, false);
    }

    static int dedicatedSlot(ItemStack stack) {
        return MachineIo.dedicatedSlot(stack);
    }

    private class MachineSlot extends Slot {
        private MachineSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            int index = getSlotIndex();
            if (index == MachineIo.FILTER) {
                return true;
            }
            if (index >= MachineIo.INPUTS && index < MachineIo.SEAL) {
                return false;
            }
            int dedicated = dedicatedSlot(stack);
            if (index == MachineIo.SEAL || index == MachineIo.CATALYST || index == MachineIo.SPECIAL) {
                return dedicated == index;
            }
            return dedicated < 0;
        }
    }

    private static boolean isGhost(ItemStack stack) {
        return !stack.isEmpty() && Boolean.TRUE.equals(stack.get(WMData.GHOST));
    }

    private static ItemStack ghostCopy(ItemStack stack) {
        ItemStack ghost = stack.copy();
        ghost.setCount(1);
        ghost.set(WMData.GHOST.get(), true);
        return ghost;
    }

    /**
     * Filter slot holds a phantom copy. Placing an item does not consume it; clearing the slot does not
     * give a ghost back. A leftover real item from an older world is still returned to the player.
     */
    private final class FilterSlot extends MachineSlot {
        private FilterSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int increment) {
            if (stack.isEmpty() || !mayPlace(stack)) {
                return stack;
            }
            // Do not consume the carried stack. Leftover real items are returned in clicked().
            setByPlayer(ghostCopy(stack));
            return stack;
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            if (stack.isEmpty()) {
                super.setByPlayer(ItemStack.EMPTY);
                return;
            }
            super.setByPlayer(isGhost(stack) ? stack : ghostCopy(stack));
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack existing = getItem();
            set(ItemStack.EMPTY);
            if (existing.isEmpty() || isGhost(existing)) {
                return ItemStack.EMPTY;
            }
            return existing;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == MachineIo.FILTER && slotId < slots.size() && slots.get(slotId) instanceof FilterSlot slot) {
            if (type == ClickType.SWAP) {
                filterSwap(slot, button, player);
                return;
            }
            if (type == ClickType.CLONE) {
                ItemStack existing = slot.getItem();
                if (GhostFilter.mayClone(existing.isEmpty(), isGhost(existing))
                        && player.getAbilities().instabuild
                        && getCarried().isEmpty()) {
                    setCarried(existing.copyWithCount(existing.getMaxStackSize()));
                }
                return;
            }
            if (type == ClickType.PICKUP) {
                filterPickup(slot, player);
                return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    /**
     * Number-key swap used getItem(), so a ghost mark could be copied into the hotbar as a real item,
     * and a hotbar stack placed into the filter was consumed. Handle the slot ourselves.
     */
    private void filterSwap(FilterSlot slot, int button, Player player) {
        if (button < 0 || (button > 8 && button != 40)) {
            return;
        }
        var inventory = player.getInventory();
        ItemStack hotbar = inventory.getItem(button);
        ItemStack existing = slot.getItem();
        switch (GhostFilter.swap(existing.isEmpty(), isGhost(existing), hotbar.isEmpty())) {
            case NONE -> {
            }
            case CLEAR_GHOST -> slot.set(ItemStack.EMPTY);
            case GIVE_REAL -> {
                inventory.setItem(button, existing);
                slot.set(ItemStack.EMPTY);
            }
            case PLACE_KEEP -> slot.setByPlayer(ghostCopy(hotbar));
            case PLACE_RETURN_REAL -> {
                ItemStack real = existing.copy();
                slot.setByPlayer(ghostCopy(hotbar));
                if (!inventory.add(real)) {
                    player.drop(real, false);
                }
            }
        }
    }

    /**
     * Place a ghost without taking the carried item. A leftover real filter from an older world
     * is given back if this click would replace it.
     */
    private void filterPickup(FilterSlot slot, Player player) {
        ItemStack carried = getCarried();
        ItemStack existing = slot.getItem();
        if (carried.isEmpty()) {
            if (!existing.isEmpty() && !isGhost(existing)) {
                slot.set(ItemStack.EMPTY);
                setCarried(existing);
            } else {
                slot.set(ItemStack.EMPTY);
            }
            return;
        }
        ItemStack real = (!existing.isEmpty() && !isGhost(existing)) ? existing.copy() : ItemStack.EMPTY;
        slot.setByPlayer(ghostCopy(carried));
        if (!real.isEmpty() && !player.getInventory().add(real)) {
            player.drop(real, false);
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot instanceof FilterSlot) {
            return GhostFilter.mayPickAll(isGhost(slot.getItem()));
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof FilterSlot) && super.canDragTo(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        if (apparatus == null || apparatus.isRemoved() || apparatus.getLevel() != player.level()) {
            return false;
        }
        var pos = apparatus.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
