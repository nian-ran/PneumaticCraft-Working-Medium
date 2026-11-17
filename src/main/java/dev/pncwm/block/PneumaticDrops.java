package dev.pncwm.block;

import java.util.List;

import dev.pncwm.blockentity.AirMachine;
import dev.pncwm.blockentity.TransferBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/** Copies air and heat from a part onto its dropped or middle-clicked item, and back again when placed. */
public final class PneumaticDrops {
    private PneumaticDrops() {
    }

    public static List<ItemStack> withStored(List<ItemStack> drops, Block block, LootParams.Builder params) {
        return withStored(drops, block, params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
    }

    public static List<ItemStack> withStored(List<ItemStack> drops, Block block, @Nullable BlockEntity be) {
        for (ItemStack drop : drops) {
            if (drop.getItem() instanceof BlockItem item && item.getBlock() == block) {
                write(be, drop);
                break;
            }
        }
        return drops;
    }

    public static void write(@Nullable BlockEntity be, ItemStack stack) {
        if (be instanceof AirMachine machine) {
            machine.savePneumaticsToItem(stack);
        } else if (be instanceof TransferBlockEntity transfer) {
            transfer.savePneumaticsToItem(stack);
        }
    }

    public static void read(@Nullable BlockEntity be, ItemStack stack) {
        if (be instanceof AirMachine machine) {
            machine.loadPneumaticsFromItem(stack);
        } else if (be instanceof TransferBlockEntity transfer) {
            transfer.loadPneumaticsFromItem(stack);
        }
    }
}
