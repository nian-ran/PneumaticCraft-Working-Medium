package dev.pncwm.client;

import dev.pncwm.WorkingMedium;
import dev.pncwm.item.ManualItem;
import dev.pncwm.registry.WMSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class ManualScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "textures/gui/manual.png");
    private int page;

    public ManualScreen() {
        super(Component.translatable("item.workingmedium.manual"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ManualScreen());
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(width / 2 - 110, height - 40, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(width / 2 + 90, height - 40, 20, 20).build());
    }

    private void turn(int delta) {
        int next = Math.max(0, Math.min(ManualItem.PAGES - 1, page + delta));
        if (next != page && minecraft != null && minecraft.player != null) {
            minecraft.player.playNotifySound(WMSounds.UI_CLICK.get(), SoundSource.MASTER, 0.4f, 1.0f);
        }
        page = next;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = width / 2 - 128;
        int y = 24;
        int panelTop = 16;
        graphics.blit(BACKGROUND, x - 8, panelTop, 0, 0, 256, Math.min(200, height - 64));
        graphics.drawString(font, title, x, y, 0xFFC8C0B4, false);
        String body = Component.translatable("manual.workingmedium.page." + page).getString();
        int line = y + 16;
        for (String paragraph : body.split("\n")) {
            for (var split : font.split(Component.literal(paragraph), 240)) {
                graphics.drawString(font, split, x, line, 0xFFDDD2C0, false);
                line += 10;
            }
            line += 4;
        }
        graphics.drawString(font, (page + 1) + " / " + ManualItem.PAGES, width / 2 - 16, height - 36, 0xFFC8C0B4, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
