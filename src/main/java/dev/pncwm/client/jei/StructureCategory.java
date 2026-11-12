package dev.pncwm.client.jei;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.pncwm.apparatus.Family;
import dev.pncwm.multiblock.StructureShapes.Cell;
import dev.pncwm.multiblock.StructureShapes.Layout;
import dev.pncwm.multiblock.StructureShapes.Role;
import dev.pncwm.registry.WMBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureCategory implements IRecipeCategory<StructureRecipe> {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 142;

    private final IDrawable background;
    private final IDrawable icon;

    public StructureCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(WMBlocks.CASINGS.values().iterator().next().get()));
    }

    @Override
    public RecipeType<StructureRecipe> getRecipeType() {
        return WorkingMediumJei.STRUCTURE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.workingmedium.structure");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StructureRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> stacks = recipe.ingredients();
        int x = 118;
        int y = 6;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            var role = i == 0 ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT;
            builder.addSlot(role, x, y).addItemStack(stack);
            y += 18;
            if (y > HEIGHT - 22) {
                y = 6;
                x += 18;
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StructureRecipe recipe, IFocusGroup focuses) {
        Layout layout = recipe.layout();
        Component size = layout.scalable()
                ? Component.translatable("jei.workingmedium.structure.size", layout.minSize(), layout.minSize(), layout.maxSize())
                : Component.translatable("jei.workingmedium.structure.fixed");
        List<FormattedText> lines = new ArrayList<>();
        lines.add(Component.translatable("block.workingmedium." + recipe.kind().id));
        lines.add(size);
        lines.add(Component.translatable("jei.workingmedium.structure.hatches"));
        if (recipe.kind().family == Family.TOWER) {
            lines.add(Component.translatable("jei.workingmedium.structure.optional.tower"));
        } else if (recipe.kind().family == Family.KETTLE) {
            lines.add(Component.translatable("jei.workingmedium.structure.optional.kettle"));
        }
        builder.addText(lines, 4, 88, 112, 50);
    }

    @Override
    public void draw(StructureRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Layout layout = recipe.layout();
        float midX = (layout.minX() + layout.maxX() + 1) / 2f;
        float midY = (layout.minY() + layout.maxY() + 1) / 2f;
        float midZ = (layout.minZ() + layout.maxZ() + 1) / 2f;
        int span = Math.max(layout.maxX() - layout.minX(), Math.max(layout.maxY() - layout.minY(), layout.maxZ() - layout.minZ())) + 1;
        float scale = Math.max(8f, 52f / Math.max(2, span));
        float yaw = (System.currentTimeMillis() % 12000L) / 12000f * 360f;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(56, 52, 80);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(25));
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 35));
        pose.translate(-midX, -midY, -midZ);

        Minecraft minecraft = Minecraft.getInstance();
        var dispatcher = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderSystem.enableDepthTest();
        for (Cell cell : layout.cells()) {
            if (cell.role() == Role.AIR) {
                continue;
            }
            BlockState state = recipe.stateFor(cell.role());
            pose.pushPose();
            pose.translate(cell.x(), cell.y(), cell.z());
            dispatcher.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
        RenderSystem.disableDepthTest();
        pose.popPose();
    }
}
