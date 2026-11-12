package dev.pncwm.registry;

import java.util.ArrayList;
import java.util.List;

import dev.pncwm.WorkingMedium;
import dev.pncwm.item.CertificateItem;
import dev.pncwm.item.CurtainModuleItem;
import dev.pncwm.item.GradeDrillItem;
import dev.pncwm.item.GradeItem;
import dev.pncwm.item.ManualItem;
import dev.pncwm.item.ProbeItem;
import dev.pncwm.pressure.PressureGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WorkingMedium.MOD_ID);
    public static final List<DeferredItem<Item>> GRADE_PARTS = new ArrayList<>();

    public static final DeferredItem<Item> STONE_DUST = item("stone_dust");
    public static final DeferredItem<Item> SLAG = item("slag");
    public static final DeferredItem<Item> METAL_ASH = item("metal_ash");
    public static final DeferredItem<Item> HEAVY_WAX = item("heavy_wax");
    public static final DeferredItem<Item> PLASTICIZER = item("plasticizer");
    public static final DeferredItem<Item> COMPRESSED_STEEL = grade("compressed_steel", PressureGrade.G2);
    public static final DeferredItem<Item> CERAMIC_ALLOY = grade("ceramic_alloy", PressureGrade.G3);
    public static final DeferredItem<Item> CRYO_ALLOY = grade("cryo_alloy", PressureGrade.G4);
    public static final DeferredItem<Item> CRITICAL_CORE = grade("critical_core", PressureGrade.G5);
    public static final DeferredItem<Item> CATALYST = item("catalyst");
    public static final DeferredItem<Item> FILTER_CAKE = item("filter_cake");
    public static final DeferredItem<Item> WASTE_CHIP = item("waste_chip");
    public static final DeferredItem<Item> ENGINEERING_PLASTIC = grade("engineering_plastic", PressureGrade.G3);
    public static final DeferredItem<Item> ELECTRODE = ITEMS.register("electrode",
            () -> new Item(new Item.Properties().durability(dev.pncwm.pressure.Tuning.electrodeLife())) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.workingmedium.electrode",
                            dev.pncwm.pressure.Tuning.electrodeLife()).withStyle(ChatFormatting.GRAY));
                }
            });
    public static final DeferredItem<Item> SUBSTRATE = item("substrate");
    public static final DeferredItem<Item> MICROFLUIDIC_CHIP = grade("microfluidic_chip", PressureGrade.G4);
    public static final DeferredItem<Item> CRUSHED_IRON = item("crushed_iron");
    public static final DeferredItem<Item> CRUSHED_COPPER = item("crushed_copper");
    public static final DeferredItem<Item> CRUSHED_GOLD = item("crushed_gold");
    public static final DeferredItem<Item> CRUSHED_ANCIENT = item("crushed_ancient");
    public static final DeferredItem<Item> PURIFIED_IRON = item("purified_iron");
    public static final DeferredItem<Item> PURIFIED_COPPER = item("purified_copper");
    public static final DeferredItem<Item> PURIFIED_GOLD = item("purified_gold");
    public static final DeferredItem<Item> PROGRAM_SHEET = ITEMS.register("program_sheet",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.workingmedium.program_sheet").withStyle(ChatFormatting.GRAY));
                }
            });
    public static final DeferredItem<CertificateItem> CERTIFICATE = ITEMS.register("certificate",
            () -> new CertificateItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> ASPHALT = ITEMS.register("asphalt",
            () -> new BlockItem(WMBlocks.ASPHALT.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.workingmedium.asphalt").withStyle(ChatFormatting.GRAY));
                }
            });
    public static final DeferredItem<ManualItem> MANUAL = ITEMS.register("manual", () -> new ManualItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ProbeItem> PROBE = ITEMS.register("venturi_probe", () -> new ProbeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<GradeDrillItem> DRILL = ITEMS.register("grade_drill", () -> new GradeDrillItem(new Item.Properties().durability(0).stacksTo(1)
            .attributes(net.minecraft.world.item.PickaxeItem.createAttributes(Tiers.DIAMOND, 1f, -2.8f))));
    public static final DeferredItem<Item> CURTAIN_G2 = curtain("curtain_g2", PressureGrade.G2);
    public static final DeferredItem<Item> CURTAIN_G3 = curtain("curtain_g3", PressureGrade.G3);
    public static final DeferredItem<Item> CURTAIN_G4 = curtain("curtain_g4", PressureGrade.G4);
    public static final DeferredItem<Item> CURTAIN_G5 = curtain("curtain_g5", PressureGrade.G5);

    static {
        for (var block : WMBlocks.ALL) {
            if (block == WMBlocks.ASPHALT) {
                continue;
            }
            ITEMS.registerSimpleBlockItem(block);
        }
        for (PressureGrade grade : new PressureGrade[] {PressureGrade.G2, PressureGrade.G3, PressureGrade.G4, PressureGrade.G5}) {
            for (String shape : new String[] {"plate", "tube", "seal", "nozzle", "diaphragm", "cylinder", "pump", "gripper", "slide"}) {
                String name = grade.id + "_" + shape;
                GRADE_PARTS.add(ITEMS.register(name, () -> new GradeItem(grade, new Item.Properties())));
            }
        }
    }

    private WMItems() {
    }

    private static DeferredItem<Item> item(String name) {
        return ITEMS.registerSimpleItem(name);
    }

    private static DeferredItem<Item> grade(String name, PressureGrade grade) {
        return ITEMS.register(name, () -> new GradeItem(grade, new Item.Properties()));
    }

    private static DeferredItem<Item> curtain(String name, PressureGrade grade) {
        return ITEMS.register(name, () -> new CurtainModuleItem(grade, new Item.Properties().durability(256)));
    }
}
