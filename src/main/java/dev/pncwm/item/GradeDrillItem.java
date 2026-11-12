package dev.pncwm.item;

import dev.pncwm.registry.WMData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GradeDrillItem extends PickaxeItem {
    public GradeDrillItem(Properties properties) {
        super(Tiers.DIAMOND, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        int heat = stack.getOrDefault(WMData.HEAT.get(), 0);
        int charge = stack.getOrDefault(WMData.CHARGE.get(), 0);
        if (heat >= 100 || charge <= 0) {
            return 0.4f;
        }
        return super.getDestroySpeed(stack, state) * (1f + charge / 40f);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) > 0) {
            int charge = stack.getOrDefault(WMData.CHARGE.get(), 0);
            int heat = stack.getOrDefault(WMData.HEAT.get(), 0);
            if (charge > 0) {
                stack.set(WMData.CHARGE.get(), charge - 1);
            }
            stack.set(WMData.HEAT.get(), Math.min(140, heat + 8));
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        int heat = stack.getOrDefault(WMData.HEAT.get(), 0);
        if (heat > 0) {
            stack.set(WMData.HEAT.get(), heat - 1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stack.getOrDefault(WMData.CHARGE.get(), 0) / 100f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int heat = stack.getOrDefault(WMData.HEAT.get(), 0);
        return heat >= 100 ? 0xFF5533 : 0x55CCAA;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.workingmedium.drill",
                stack.getOrDefault(WMData.CHARGE.get(), 0), stack.getOrDefault(WMData.HEAT.get(), 0)));
    }
}
