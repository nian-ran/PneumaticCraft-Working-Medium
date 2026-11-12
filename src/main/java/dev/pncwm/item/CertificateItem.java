package dev.pncwm.item;

import java.util.List;

import dev.pncwm.blockentity.ApparatusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Written by the calibration bench. Use on a formed machine to cut its air use and seal wear by 25%. */
public class CertificateItem extends Item {
    public CertificateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ApparatusBlockEntity apparatus)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!apparatus.formed().formed()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.workingmedium.certify_unformed"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (!apparatus.applyCertificate()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.workingmedium.already_certified"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.workingmedium.certified",
                    Component.translatable("block.workingmedium." + apparatus.kind().id)), true);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        } else {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.workingmedium.certificate").withStyle(ChatFormatting.GRAY));
    }
}
