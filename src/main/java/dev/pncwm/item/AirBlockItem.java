package dev.pncwm.item;

import java.util.List;

import dev.pncwm.block.GaugeBlock;
import dev.pncwm.block.GradeTubeBlock;
import dev.pncwm.block.ReliefValveBlock;
import dev.pncwm.block.TransferBlock;
import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.registry.WMData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Tube, gauge, relief valve, reducer or booster item that can carry leftover air and heat. */
public class AirBlockItem extends BlockItem {
    public AirBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StoredPneumatics stored = stack.get(WMData.MACHINE_PNEUMATICS.get());
        if (stored == null || stored.isEmpty()) {
            return;
        }
        Block block = getBlock();
        if (block instanceof TransferBlock) {
            String key = StoredPneumatics.transferTooltipKey(stored.air(), stored.secondAir());
            boolean vacuumOnly = "tooltip.workingmedium.stored_transfer_vacuum".equals(key);
            tooltip.add(Component.translatable(key,
                    vacuumOnly ? StoredPneumatics.displayAmount(stored.air()) : stored.air(),
                    vacuumOnly ? StoredPneumatics.displayBar(stored.air(), 4000) : StoredPneumatics.signedBar(stored.air(), 4000),
                    vacuumOnly ? StoredPneumatics.displayAmount(stored.secondAir()) : stored.secondAir(),
                    vacuumOnly ? StoredPneumatics.displayBar(stored.secondAir(), 4000) : StoredPneumatics.signedBar(stored.secondAir(), 4000))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            int volume = volumeOf(block);
            String key = StoredPneumatics.storedAirTooltipKey(stored.air(), volume > 0);
            if (key != null) {
                if (volume > 0) {
                    tooltip.add(Component.translatable(key,
                            StoredPneumatics.displayAmount(stored.air()),
                            StoredPneumatics.displayBar(stored.air(), volume)).withStyle(ChatFormatting.GRAY));
                } else {
                    tooltip.add(Component.translatable(key, StoredPneumatics.displayAmount(stored.air()))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (Math.abs(stored.temperature() - StoredPneumatics.AMBIENT_KELVIN) >= 0.5) {
            tooltip.add(Component.translatable("tooltip.workingmedium.stored_heat",
                    PressureGrade.kelvinToCelsius(stored.temperature())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }


    private static int volumeOf(Block block) {
        if (block instanceof GradeTubeBlock) {
            return 400;
        }
        if (block instanceof GaugeBlock) {
            return 80;
        }
        if (block instanceof ReliefValveBlock) {
            return 1000;
        }
        return 0;
    }
}
