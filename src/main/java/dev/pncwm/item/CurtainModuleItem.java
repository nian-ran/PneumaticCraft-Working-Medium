package dev.pncwm.item;

import java.util.List;

import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class CurtainModuleItem extends GradeItem {
    public CurtainModuleItem(PressureGrade grade, Properties properties) {
        super(grade, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int absorbed = Math.round((1f - Tuning.curtainKept(grade.index)) * 100f);
        tooltip.add(Component.translatable("tooltip.workingmedium.curtain", absorbed).withStyle(ChatFormatting.GRAY));
    }

    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.getItem().builtInRegistryHolder().key().location().getPath().contains("pneumatic")) {
            return;
        }
        // Use the strongest intact curtain anywhere in the inventory, not just the first one found.
        ItemStack best = ItemStack.EMPTY;
        int bestGrade = -1;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CurtainModuleItem curtain && stack.getDamageValue() < stack.getMaxDamage()
                    && curtain.grade.index > bestGrade) {
                best = stack;
                bestGrade = curtain.grade.index;
            }
        }
        if (best.isEmpty()) {
            return;
        }
        event.setAmount(event.getAmount() * Tuning.curtainKept(bestGrade));
        // The module lives in the inventory, not an armor slot. Do not use an
        // equipment slot, or breaking the module would delete a held item.
        if (player.level() instanceof ServerLevel serverLevel) {
            ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
            best.hurtAndBreak(1, serverLevel, serverPlayer, item -> {});
        }
    }
}
