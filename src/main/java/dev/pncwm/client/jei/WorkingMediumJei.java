package dev.pncwm.client.jei;

import java.util.Arrays;
import java.util.List;

import dev.pncwm.WorkingMedium;
import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.recipe.FluidSpec;
import dev.pncwm.recipe.ItemOut;
import dev.pncwm.recipe.WindowRecipe;
import dev.pncwm.registry.WMBlocks;
import dev.pncwm.registry.WMRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Loaded only by JEI's client plugin scan. The mod does not reference this class.
 */
@JeiPlugin
public class WorkingMediumJei implements IModPlugin {
    public static final RecipeType<WindowRecipe> TYPE = RecipeType.create(WorkingMedium.MOD_ID, "window", WindowRecipe.class);
    public static final RecipeType<StructureRecipe> STRUCTURE = RecipeType.create(WorkingMedium.MOD_ID, "structure", StructureRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(WorkingMedium.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new WindowCategory(gui), new StructureCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(STRUCTURE, Arrays.stream(ApparatusKind.values()).map(StructureRecipe::of).toList());
        RecipeManager manager = clientRecipeManager();
        if (manager == null) {
            return;
        }
        List<WindowRecipe> recipes = manager.getAllRecipesFor(WMRecipes.WINDOW.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(TYPE, recipes);
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.CERTIFICATE.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.certificate"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.WASTE_CHIP.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.waste_chip"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.PLASTICIZER.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.plasticizer"));
        for (var part : dev.pncwm.registry.WMItems.GRADE_PARTS) {
            if (part.getId().getPath().endsWith("_seal")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.seal"));
            }
        }
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.GAUGE.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.gauge"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.ASPHALT.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.asphalt"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.HEAT).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_heat"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.GAUGE).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_gauge"));
        for (var part : dev.pncwm.registry.WMItems.GRADE_PARTS) {
            String path = part.getId().getPath();
            if (path.endsWith("_pump")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.pump"));
            }
            if (path.endsWith("_slide")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.slide"));
            }
            if (path.endsWith("_nozzle")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.nozzle"));
            }
            if (path.endsWith("_cylinder")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.cylinder"));
            }
            if (path.endsWith("_diaphragm")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.diaphragm"));
            }
            if (path.endsWith("_gripper")) {
                registration.addIngredientInfo(new ItemStack(part.get()), VanillaTypes.ITEM_STACK,
                        Component.translatable("jei.workingmedium.info.gripper"));
            }
        }
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.ELECTRODE.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.electrode"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.MICROFLUIDIC_CHIP.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.chip"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.PROGRAM_SHEET.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.program_sheet"));
        for (var fluid : dev.pncwm.registry.WMFluids.ALL) {
            if (dev.pncwm.pressure.Tuning.fluidHarm(fluid.name) > 0) {
                registration.addIngredientInfo(new net.neoforged.neoforge.fluids.FluidStack(fluid.source.get(), 1000),
                        NeoForgeTypes.FLUID_STACK, Component.translatable("jei.workingmedium.info.hazard_fluid"));
            }
            if (dev.pncwm.pressure.Tuning.isCryogenic(fluid.name)) {
                registration.addIngredientInfo(new net.neoforged.neoforge.fluids.FluidStack(fluid.source.get(), 1000),
                        NeoForgeTypes.FLUID_STACK, Component.translatable("jei.workingmedium.info.cryo_fluid"));
            }
            if (dev.pncwm.pressure.Tuning.isPurgeGas(fluid.name)) {
                registration.addIngredientInfo(new net.neoforged.neoforge.fluids.FluidStack(fluid.source.get(), 1000),
                        NeoForgeTypes.FLUID_STACK, Component.translatable("jei.workingmedium.info.purge"));
            }
        }
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.CONTROLLERS.get(ApparatusKind.FLARE_STACK).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.flare"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.DRAIN).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_drain"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.ITEM_IN).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_io"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.ITEM_OUT).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_io"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.FLUID).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_io"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.HATCHES.get(dev.pncwm.block.HatchType.GAS).get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.hatch_io"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.REDUCER.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.redstone_pause"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.BOOSTER.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.redstone_pause"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMBlocks.RELIEF.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.redstone_pause"));
        registration.addIngredientInfo(new ItemStack(dev.pncwm.registry.WMItems.STONE_DUST.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.workingmedium.info.stone_dust"));
    }

    /**
     * JEI often starts during login before {@code minecraft.level} is set, while the
     * connection already holds the synced recipe manager.
     */
    public static RecipeManager clientRecipeManager() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.getRecipeManager();
        }
        var connection = minecraft.getConnection();
        if (connection != null) {
            return connection.getRecipeManager();
        }
        return null;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(WMBlocks.GAUGE.get()), TYPE);
        for (ApparatusKind kind : ApparatusKind.values()) {
            ItemStack controller = new ItemStack(WMBlocks.CONTROLLERS.get(kind).get());
            registration.addRecipeCatalyst(controller, STRUCTURE);
            if (kind.usesRecipes()) {
                registration.addRecipeCatalyst(controller, TYPE);
            }
        }
        for (var casing : WMBlocks.CASINGS.values()) {
            registration.addRecipeCatalyst(new ItemStack(casing.get()), STRUCTURE);
        }
    }

    /**
     * Both windows the category draws for a recipe. JEI calls this from {@link WindowCategory}.
     */
    public static List<FormattedText> windowLines(WindowRecipe recipe) {
        List<FormattedText> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("block.workingmedium." + recipe.machine));
        lines.add(Component.translatable("jei.workingmedium.pressure", trim(recipe.pressureMin), trim(recipe.pressureMax)));
        lines.add(Component.translatable("jei.workingmedium.temperature", recipe.tempMinC, recipe.tempMaxC));
        if (recipe.minSize > 1) {
            lines.add(Component.translatable("jei.workingmedium.min_size", recipe.minSize));
        }
        int coils = recipe.requiredCoils();
        if (coils > 0) {
            lines.add(Component.translatable("jei.workingmedium.coils", coils));
        }
        if (recipe.lubeMb > 0) {
            lines.add(Component.translatable("jei.workingmedium.lube", recipe.lubeMb));
        }
        if (recipe.airPerTick < 0) {
            lines.add(Component.translatable("jei.workingmedium.air_produce", -recipe.airPerTick));
        } else if (recipe.airPerTick > 0) {
            lines.add(Component.translatable("jei.workingmedium.air_cost", recipe.airPerTick));
        }
        return lines;
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return Float.toString(value);
    }

    private static final class WindowCategory implements IRecipeCategory<WindowRecipe> {
        private final IDrawable background;
        private final IDrawable icon;
        private final WindowTextBox textBox;

        private WindowCategory(IGuiHelper gui) {
            this.textBox = WindowTextBox.category();
            this.background = gui.createBlankDrawable(WindowTextBox.BACKGROUND_WIDTH, WindowTextBox.BACKGROUND_HEIGHT);
            this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(WMBlocks.GAUGE.get()));
        }

        @Override
        public RecipeType<WindowRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.workingmedium.window");
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
        public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder, WindowRecipe recipe, IFocusGroup focuses) {
            int inputX = 4;
            for (FluidSpec spec : recipe.fluids) {
                FluidStack stack = spec.stack(1);
                if (!stack.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.INPUT, inputX, 4)
                            .addIngredient(NeoForgeTypes.FLUID_STACK, stack)
                            .setFluidRenderer(Math.max(1, stack.getAmount()), false, 16, 16);
                    inputX += 18;
                }
            }
            for (int i = 0; i < recipe.items.size(); i++) {
                var ingredient = recipe.items.get(i);
                int count = recipe.presentCount(i);
                boolean held = recipe.consumeCount(i) <= 0;
                var role = held ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT;
                java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
                for (ItemStack stack : ingredient.getItems()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(count);
                    if ("critical_kettle".equals(recipe.machine) && copy.is(dev.pncwm.registry.WMItems.MICROFLUIDIC_CHIP.get())) {
                        copy.set(dev.pncwm.registry.WMData.CLEANED, true);
                    }
                    stacks.add(copy);
                }
                var slot = builder.addSlot(role, inputX, 4).addItemStacks(stacks);
                if (held) {
                    slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.workingmedium.held")));
                }
                inputX += 18;
            }
            int outputX = 96;
            for (ItemOut out : recipe.itemOutputs) {
                ItemStack stack = out.stack(1);
                if ("clean_room".equals(recipe.machine) && stack.is(dev.pncwm.registry.WMItems.MICROFLUIDIC_CHIP.get())) {
                    stack.set(dev.pncwm.registry.WMData.CLEANED, true);
                }
                if (!stack.isEmpty()) {
                    var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, 4).addItemStack(stack);
                    if (out.minSize() > recipe.minSize) {
                        int size = out.minSize();
                        slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.workingmedium.size_gate", size)));
                    }
                    outputX += 18;
                }
            }
            for (FluidSpec spec : recipe.fluidOutputs) {
                FluidStack stack = spec.stack(1);
                if (!stack.isEmpty()) {
                    var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, 4)
                            .addIngredient(NeoForgeTypes.FLUID_STACK, stack)
                            .setFluidRenderer(Math.max(1, stack.getAmount()), false, 16, 16);
                    if (spec.minSize() > recipe.minSize) {
                        int size = spec.minSize();
                        slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.workingmedium.size_gate", size)));
                    }
                    outputX += 18;
                }
            }
        }

        @Override
        public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, WindowRecipe recipe, IFocusGroup focuses) {
            builder.addText(windowLines(recipe), textBox.x, textBox.y, textBox.width, textBox.height);
        }
    }
}
