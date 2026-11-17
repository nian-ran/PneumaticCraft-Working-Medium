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
            tooltip.add(Component.translatable("tooltip.workingmedium.stored_transfer",
                    stored.air(), bar(stored.air(), 4000),
                    stored.secondAir(), bar(stored.secondAir(), 4000)).withStyle(ChatFormatting.GRAY));
        } else {
            int volume = volumeOf(block);
            if (volume > 0) {
                tooltip.add(Component.translatable("tooltip.workingmedium.stored_air",
                        stored.air(), bar(stored.air(), volume)).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.workingmedium.stored_air_amount", stored.air())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (Math.abs(stored.temperature() - StoredPneumatics.AMBIENT_KELVIN) >= 0.5) {
            tooltip.add(Component.translatable("tooltip.workingmedium.stored_heat",
                    PressureGrade.kelvinToCelsius(stored.temperature())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String bar(int air, int volume) {
        if (volume <= 0) {
            return "0.00";
        }
        return String.format("%.2f", air / (float) volume);
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
