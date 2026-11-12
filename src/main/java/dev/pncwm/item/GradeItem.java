package dev.pncwm.item;

import java.util.List;

import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.pressure.Tuning;
import dev.pncwm.registry.WMData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class GradeItem extends Item {
    public final PressureGrade grade;

    public GradeItem(PressureGrade grade, Properties properties) {
        super(properties);
        this.grade = grade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.workingmedium.grade." + grade.id));
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        if (path.equals("microfluidic_chip")) {
            if (Boolean.TRUE.equals(stack.get(WMData.CLEANED))) {
                tooltip.add(Component.translatable("tooltip.workingmedium.cleaned").withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.translatable("tooltip.workingmedium.dirty_chip").withStyle(ChatFormatting.GRAY));
            }
        }
        if (path.endsWith("_seal")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.seal_life", Tuning.sealLife(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_pump")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.pump", Tuning.gradeMultiplier(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_slide")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.slide", Tuning.gradeMultiplier(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_nozzle")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.nozzle", Tuning.gradeMultiplier(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_cylinder")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.cylinder", Tuning.gradeMultiplier(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_diaphragm")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.diaphragm",
                    String.format("%.0f", Tuning.regulatorEfficiency(Tuning.gradeMultiplier(grade.index)) * 100f))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (path.endsWith("_gripper")) {
            tooltip.add(Component.translatable("tooltip.workingmedium.gripper", Tuning.gradeMultiplier(grade.index))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
