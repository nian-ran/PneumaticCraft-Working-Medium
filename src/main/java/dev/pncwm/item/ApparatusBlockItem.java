package dev.pncwm.item;

import java.util.List;

import dev.pncwm.pressure.PressureGrade;
import dev.pncwm.registry.WMData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Controller item that can carry leftover tank contents, air and heat. Fluids and pneumatics
 * saved here are restored when the block is placed; air stays pending until the shell forms.
 */
public class ApparatusBlockItem extends BlockItem {
    public ApparatusBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<WMData.StoredFluid> stored = stack.get(WMData.MACHINE_FLUIDS.get());
        if (stored != null && !stored.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.workingmedium.stored_fluids").withStyle(ChatFormatting.GRAY));
            for (WMData.StoredFluid entry : stored) {
                if (entry.amount() <= 0) {
                    continue;
                }
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(entry.fluid()));
                Component name = fluid == Fluids.EMPTY
                        ? Component.literal(entry.fluid())
                        : Component.translatable(fluid.getFluidType().getDescriptionId());
                tooltip.add(Component.translatable("tooltip.workingmedium.stored_fluid", name, entry.amount())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        StoredPneumatics pneumatics = stack.get(WMData.MACHINE_PNEUMATICS.get());
        if (pneumatics == null || pneumatics.isEmpty()) {
            return;
        }
        String airKey = StoredPneumatics.controllerAirTooltipKey(pneumatics.air());
        if (airKey != null) {
            // Do not convert air into a pressure: the unformed brick does not yet have the hull volume.
            tooltip.add(Component.translatable(airKey, StoredPneumatics.displayAmount(pneumatics.air()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (Math.abs(pneumatics.temperature() - StoredPneumatics.AMBIENT_KELVIN) >= 0.5) {
            tooltip.add(Component.translatable("tooltip.workingmedium.stored_heat",
                    PressureGrade.kelvinToCelsius(pneumatics.temperature())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
