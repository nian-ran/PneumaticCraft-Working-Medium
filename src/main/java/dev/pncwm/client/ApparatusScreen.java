package dev.pncwm.client;

import java.util.ArrayList;
import java.util.List;

import dev.pncwm.WorkingMedium;
import dev.pncwm.blockentity.MachineIo;
import dev.pncwm.menu.ApparatusMenu;
import dev.pncwm.pressure.MachineStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class ApparatusScreen extends AbstractContainerScreen<ApparatusMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "textures/gui/apparatus.png");
    private static final int WELL = 0xFF141210;
    private static final int TRIM = 0xFF888888;

    public ApparatusScreen(ApparatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 188;
        this.inventoryLabelY = 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            int mark = slot.index < MachineIo.SLOTS ? roleColor(slot.index) : 0xFF3A3534;
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, mark);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, WELL);
        }
        int time = menu.data(6);
        int filled = time <= 0 ? 0 : Math.min(80, menu.data(0) * 80 / time);
        graphics.fill(x + 86, y + 62, x + 168, y + 68, WELL);
        graphics.fill(x + 87, y + 63, x + 87 + filled, y + 67, TRIM);
        if (menu.apparatus != null) {
            for (int i = 0; i < MachineIo.TANKS; i++) {
                int tx = x + 8 + i * 18;
                int ty = y + 74;
                graphics.fill(tx - 1, ty - 1, tx + 17, ty + 17, tankColor(i));
                graphics.fill(tx, ty, tx + 16, ty + 16, WELL);
                FluidStack fluid = menu.apparatus.io.tanks[i].getFluid();
                if (!fluid.isEmpty()) {
                    int height = Math.max(1, fluid.getAmount() * 14 / 16_000);
                    int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor();
                    if ((color >>> 24) == 0) {
                        color |= 0xFF000000;
                    }
                    graphics.fill(tx + 1, ty + 15 - height, tx + 15, ty + 15, color);
                }
            }
        }
    }

    private int tankAt(double mouseX, double mouseY) {
        for (int i = 0; i < MachineIo.TANKS; i++) {
            int tx = leftPos + 8 + i * 18;
            int ty = topPos + 74;
            if (mouseX >= tx && mouseX < tx + 16 && mouseY >= ty && mouseY < ty + 16) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0 && minecraft != null && minecraft.gameMode != null && !menu.getCarried().isEmpty()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tank);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component heading = title;
        if (menu.apparatus != null && menu.apparatus.certified()) {
            heading = title.copy().append(Component.literal(" · ")).append(Component.translatable("tooltip.workingmedium.certified"));
        }
        graphics.drawString(font, heading, 8, 6, 0xFFD0C8BE, false);
        MachineStatus status = MachineStatus.byOrdinal(menu.data(1));
        int line = 18;
        for (FormattedCharSequence text : font.split(status.text(), 82)) {
            graphics.drawString(font, text, 86, line, status.color, false);
            line += 9;
            if (line > 42) {
                break;
            }
        }
        graphics.drawString(font, Component.translatable("tooltip.workingmedium.pressure_short", menu.data(4) / 100f), 86, 44, 0xFFD0C8BE, false);
        graphics.drawString(font, Component.translatable("tooltip.workingmedium.temperature_short", menu.data(5)), 86, 52, 0xFFD0C8BE, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB7A99A, false);
        Component sizeSeal = Component.translatable("tooltip.workingmedium.size_seal", menu.data(3), menu.data(2));
        graphics.drawString(font, sizeSeal, imageWidth - 8 - font.width(sizeSeal), inventoryLabelY, 0xFFD0C8BE, false);
    }

    private boolean overProgress(double mouseX, double mouseY) {
        int x = leftPos + 86;
        int y = topPos + 62;
        return mouseX >= x && mouseX < x + 82 && mouseY >= y && mouseY < y + 6;
    }

    private boolean overStatus(double mouseX, double mouseY) {
        int x = leftPos + 86;
        int y = topPos + 18;
        return mouseX >= x && mouseX < x + 82 && mouseY >= y && mouseY < y + 42;
    }

    /** Status, what to do about it, and the active recipe window when one is known. */
    private List<Component> statusTooltip() {
        MachineStatus status = MachineStatus.byOrdinal(menu.data(1));
        List<Component> lines = new ArrayList<>();
        lines.add(status.text().withStyle(style -> style.withColor(status.color & 0xFFFFFF)));
        lines.add(Component.translatable("hint.workingmedium." + status.id).withStyle(ChatFormatting.GRAY));
        if (menu.apparatus != null && menu.apparatus.certified()) {
            lines.add(Component.translatable("tooltip.workingmedium.certified").withStyle(ChatFormatting.AQUA));
        }
        if (menu.data(11) == 1) {
            lines.add(Component.translatable("tooltip.workingmedium.active_window",
                    String.format("%.1f", menu.data(7) / 100f), String.format("%.1f", menu.data(8) / 100f),
                    menu.data(9), menu.data(10)).withStyle(ChatFormatting.DARK_AQUA));
        }
        return lines;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (overStatus(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, statusTooltip(), mouseX, mouseY);
            return;
        }
        if (overProgress(mouseX, mouseY)) {
            int time = Math.max(1, menu.data(6));
            graphics.renderTooltip(font, Component.translatable("tooltip.workingmedium.progress",
                    menu.data(0) + "/" + time, menu.data(3), menu.data(2)), mouseX, mouseY);
            return;
        }
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0 && menu.apparatus != null) {
            FluidStack fluid = menu.apparatus.io.tanks[tank].getFluid();
            Component tip = fluid.isEmpty()
                    ? Component.translatable(tankKey(tank))
                    : Component.translatable("gui.workingmedium.fluid", fluid.getHoverName(), fluid.getAmount());
            graphics.renderTooltip(font, tip, mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < MachineIo.SLOTS) {
            graphics.renderTooltip(font, Component.translatable(roleKey(hoveredSlot.index)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private static int tankColor(int index) {
        if (index < MachineIo.INPUTS) {
            return 0xFF6E6862;
        }
        if (index == MachineIo.LUBE) {
            return TRIM;
        }
        return 0xFF3D6B4A;
    }

    private static String tankKey(int index) {
        if (index < MachineIo.INPUTS) {
            return "slot.workingmedium.fluid_in";
        }
        if (index == MachineIo.LUBE) {
            return "slot.workingmedium.lube";
        }
        return "slot.workingmedium.fluid_out";
    }

    private static int roleColor(int index) {
        if (index < MachineIo.INPUTS) {
            return 0xFF6E6862;
        }
        if (index < MachineIo.SEAL) {
            return 0xFF3D6B4A;
        }
        if (index == MachineIo.SEAL) {
            return TRIM;
        }
        if (index == MachineIo.CATALYST) {
            return 0xFF5E8A62;
        }
        if (index == MachineIo.SPECIAL) {
            return 0xFF5E7C8A;
        }
        return 0xFF8A6A3A;
    }

    private static String roleKey(int index) {
        if (index < MachineIo.INPUTS) {
            return "slot.workingmedium.input";
        }
        if (index < MachineIo.SEAL) {
            return "slot.workingmedium.output";
        }
        if (index == MachineIo.SEAL) {
            return "slot.workingmedium.seal";
        }
        if (index == MachineIo.CATALYST) {
            return "slot.workingmedium.catalyst";
        }
        if (index == MachineIo.SPECIAL) {
            return "slot.workingmedium.special";
        }
        return "slot.workingmedium.filter";
    }
}
