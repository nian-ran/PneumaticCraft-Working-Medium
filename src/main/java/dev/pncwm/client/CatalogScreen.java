package dev.pncwm.client;

import dev.pncwm.WorkingMedium;
import dev.pncwm.menu.CatalogMenu;
import dev.pncwm.registry.WMSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;

public class CatalogScreen extends AbstractContainerScreen<CatalogMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "textures/gui/catalog.png");
    public CatalogScreen(CatalogMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> click(0))
                .bounds(leftPos + 8, topPos + 36, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> click(1))
                .bounds(leftPos + 148, topPos + 36, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.workingmedium.trade"), button -> click(2))
                .bounds(leftPos + 52, topPos + 48, 72, 20).build());
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.player.playNotifySound(WMSounds.UI_CLICK.get(), SoundSource.MASTER, 0.45f, 1.0f);
            menu.clickMenuButton(minecraft.player, button);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFD0C8BE, false);
        CatalogMenu.Offer offer = menu.offer();
        String gate = offer.gate() ? " *" : "";
        graphics.drawString(font, offer.pay().getCount() + " " + offer.pay().getHoverName().getString() + gate, 32, 24, 0xFFD0C8BE, false);
        graphics.drawString(font, offer.goods().getCount() + " " + offer.goods().getHoverName().getString(), 32, 36, 0xFFC8D0C4, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
