package dev.pncwm.item;

import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.registry.WMData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ProbeItem extends Item {
    public ProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof ApparatusBlockEntity apparatus)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (apparatus.kind().id.equals("calibration_bench")) {
            Long saved = context.getItemInHand().get(WMData.PROBE_POS.get());
            if (saved != null) {
                BlockPos target = BlockPos.of(saved);
                if (!(level.getBlockEntity(target) instanceof ApparatusBlockEntity)) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_missing", target.toShortString()), true);
                } else if (apparatus.watch(target)) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_bound", target.toShortString()), true);
                } else {
                    context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_duplicate", target.toShortString()), true);
                }
            } else {
                context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_empty"), true);
            }
        } else {
            context.getItemInHand().set(WMData.PROBE_POS.get(), pos.asLong());
            context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_saved",
                    Component.translatable("block.workingmedium." + apparatus.kind().id),
                    String.format("%.1f", apparatus.air() == null ? 0 : apparatus.air().getPressure())), true);
        }
        return InteractionResult.CONSUME;
    }
}
