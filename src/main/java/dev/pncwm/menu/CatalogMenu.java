package dev.pncwm.menu;

import dev.pncwm.WMConfig;
import dev.pncwm.registry.WMBlocks;
import dev.pncwm.registry.WMFluids;
import dev.pncwm.registry.WMItems;
import dev.pncwm.registry.WMMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CatalogMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private int page;

    public CatalogMenu(int id, Inventory inventory, BlockPos pos) {
        super(WMMenus.CATALOG.get(), id);
        this.pos = pos.immutable();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public int page() {
        return page;
    }

    public Offer offer() {
        Offer[] offers = offers();
        return offers[Math.floorMod(page, offers.length)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Offer[] offers = offers();
        if (id == 0) {
            page = Math.floorMod(page - 1, offers.length);
            return true;
        }
        if (id == 1) {
            page = Math.floorMod(page + 1, offers.length);
            return true;
        }
        if (id == 2 && !player.level().isClientSide) {
            trade(player, offer());
        }
        return true;
    }

    private void trade(Player player, Offer offer) {
        if (offer.gate && !WMConfig.sellGateChemicals()) {
            player.displayClientMessage(Component.translatable("message.workingmedium.gate_closed"), true);
            return;
        }
        if (count(player, offer.pay) < offer.payCount()) {
            player.displayClientMessage(Component.translatable("message.workingmedium.cant_afford"), true);
            return;
        }
        remove(player, offer.pay, offer.payCount());
        ItemStack result = offer.goods.copy();
        if (!player.addItem(result)) {
            player.drop(result, false);
        }
    }

    private static int count(Player player, ItemStack sample) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, sample)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void remove(Player player, ItemStack sample, int amount) {
        int left = amount;
        for (int i = 0; i < player.getInventory().items.size() && left > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, sample)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int hotbar = 27;
        if (index < hotbar) {
            if (!moveItemStackTo(stack, hotbar, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, hotbar, false)) {
            return ItemStack.EMPTY;
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

    @Override
    public boolean stillValid(Player player) {
        if (pos == null) {
            return false;
        }
        return player.level().getBlockState(pos).is(WMBlocks.CATALOG.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    public static Offer[] offers() {
        return new Offer[] {
                new Offer(new ItemStack(Items.EMERALD, 4), new ItemStack(WMItems.METAL_ASH.get(), 8), false),
                new Offer(new ItemStack(Items.EMERALD, 6), new ItemStack(WMItems.HEAVY_WAX.get(), 4), false),
                new Offer(new ItemStack(Items.EMERALD, 8), new ItemStack(WMFluids.HYDROCHLORIC.bucket.get()), false),
                new Offer(new ItemStack(WMItems.STONE_DUST.get(), 16), new ItemStack(Items.EMERALD), false),
                new Offer(new ItemStack(WMItems.SLAG.get(), 8), new ItemStack(Items.EMERALD), false),
                new Offer(new ItemStack(WMItems.FILTER_CAKE.get(), 8), new ItemStack(Items.EMERALD), false),
                new Offer(new ItemStack(WMItems.WASTE_CHIP.get(), 8), new ItemStack(Items.EMERALD), false),
                new Offer(new ItemStack(Items.EMERALD, 24), new ItemStack(WMItems.COMPRESSED_STEEL.get(), 2), true),
                new Offer(new ItemStack(Items.EMERALD, 32), new ItemStack(WMItems.ENGINEERING_PLASTIC.get(), 2), true),
                new Offer(new ItemStack(Items.EMERALD, 48), new ItemStack(WMFluids.LIQUID_CO2.bucket.get()), true)
        };
    }

    public record Offer(ItemStack pay, ItemStack goods, boolean gate) {
        public int payCount() {
            return pay.getCount();
        }
    }
}
