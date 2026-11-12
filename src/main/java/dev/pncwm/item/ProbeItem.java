package dev.pncwm.item;

import java.util.List;

import dev.pncwm.blockentity.ApparatusBlockEntity;
import dev.pncwm.blockentity.GaugeBlockEntity;
import dev.pncwm.blockentity.HatchBlockEntity;
import dev.pncwm.blockentity.ReliefValveBlockEntity;
import dev.pncwm.blockentity.TransferBlockEntity;
import dev.pncwm.registry.WMData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ProbeItem extends Item {
    public ProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HatchBlockEntity hatch) {
            ApparatusBlockEntity bound = hatch.controller();
            if (bound == null) {
                if (!level.isClientSide && context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_unbound"), true);
                }
                return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
            return useOnApparatus(context, bound);
        }
        if (be instanceof ApparatusBlockEntity apparatus) {
            return useOnApparatus(context, apparatus);
        }
        if (level.isClientSide) {
            return be instanceof TransferBlockEntity || be instanceof ReliefValveBlockEntity || be instanceof GaugeBlockEntity
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (be instanceof TransferBlockEntity transfer) {
            transfer.tell(context.getPlayer());
            return InteractionResult.CONSUME;
        }
        if (be instanceof ReliefValveBlockEntity relief) {
            relief.tell(context.getPlayer());
            return InteractionResult.CONSUME;
        }
        if (be instanceof GaugeBlockEntity gauge) {
            gauge.tell(context.getPlayer());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult useOnApparatus(UseOnContext context, ApparatusBlockEntity apparatus) {
        Level level = context.getLevel();
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
            context.getItemInHand().set(WMData.PROBE_POS.get(), apparatus.getBlockPos().asLong());
            context.getPlayer().displayClientMessage(Component.translatable("message.workingmedium.probe_saved",
                    Component.translatable("block.workingmedium." + apparatus.kind().id),
                    String.format("%.1f", apparatus.air() == null ? 0 : apparatus.air().getPressure())), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.workingmedium.probe").withStyle(ChatFormatting.GRAY));
    }
}
