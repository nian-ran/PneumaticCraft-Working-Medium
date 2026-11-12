package dev.pncwm.registry;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import dev.pncwm.WorkingMedium;
import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.block.HatchType;
import dev.pncwm.block.PartKind;
import dev.pncwm.pressure.PressureGrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorkingMedium.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.workingmedium"))
            .icon(() -> new ItemStack(WMBlocks.GAUGE.get()))
            .displayItems((parameters, output) -> {
                Set<Item> shown = new LinkedHashSet<>();
                Consumer<ItemLike> add = item -> {
                    Item resolved = item.asItem();
                    if (shown.add(resolved)) {
                        output.accept(resolved);
                    }
                };

                add.accept(WMItems.MANUAL);
                add.accept(WMItems.PROBE);
                add.accept(WMItems.DRILL);
                add.accept(WMItems.PROGRAM_SHEET);
                add.accept(WMItems.CERTIFICATE);

                for (PressureGrade grade : PressureGrade.values()) {
                    add.accept(WMBlocks.CASINGS.get(grade));
                }
                for (HatchType type : HatchType.values()) {
                    add.accept(WMBlocks.HATCHES.get(type));
                }
                for (PartKind kind : PartKind.values()) {
                    add.accept(WMBlocks.PARTS.get(kind));
                }

                add.accept(WMBlocks.GAUGE);
                add.accept(WMBlocks.REDUCER);
                add.accept(WMBlocks.BOOSTER);
                add.accept(WMBlocks.RELIEF);
                add.accept(WMBlocks.INDUSTRIAL_TUBE);
                add.accept(WMBlocks.CRYO_TUBE);
                add.accept(WMBlocks.CATALOG);
                add.accept(WMBlocks.ASPHALT);

                for (ApparatusKind kind : ApparatusKind.values()) {
                    add.accept(WMBlocks.CONTROLLERS.get(kind));
                }

                add.accept(WMItems.COMPRESSED_STEEL);
                add.accept(WMItems.CERAMIC_ALLOY);
                add.accept(WMItems.CRYO_ALLOY);
                add.accept(WMItems.CRITICAL_CORE);
                add.accept(WMItems.ENGINEERING_PLASTIC);
                add.accept(WMItems.ELECTRODE);
                add.accept(WMItems.SUBSTRATE);
                add.accept(WMItems.MICROFLUIDIC_CHIP);
                add.accept(WMItems.CATALYST);
                add.accept(WMItems.HEAVY_WAX);
                add.accept(WMItems.PLASTICIZER);
                add.accept(WMItems.CRUSHED_IRON);
                add.accept(WMItems.CRUSHED_COPPER);
                add.accept(WMItems.CRUSHED_GOLD);
                add.accept(WMItems.CRUSHED_ANCIENT);
                add.accept(WMItems.PURIFIED_IRON);
                add.accept(WMItems.PURIFIED_COPPER);
                add.accept(WMItems.PURIFIED_GOLD);
                add.accept(WMItems.STONE_DUST);
                add.accept(WMItems.SLAG);
                add.accept(WMItems.METAL_ASH);
                add.accept(WMItems.FILTER_CAKE);
                add.accept(WMItems.WASTE_CHIP);

                add.accept(WMItems.CURTAIN_G2);
                add.accept(WMItems.CURTAIN_G3);
                add.accept(WMItems.CURTAIN_G4);
                add.accept(WMItems.CURTAIN_G5);

                for (var part : WMItems.GRADE_PARTS) {
                    add.accept(part);
                }

                for (WMFluids.FluidSet fluid : WMFluids.ALL) {
                    add.accept(fluid.bucket);
                }

                // Anything registered later, except fluid *block* items (buckets are the placeable form).
                for (var entry : WMItems.ITEMS.getEntries()) {
                    Item item = entry.get();
                    if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof LiquidBlock) {
                        continue;
                    }
                    add.accept(item);
                }
            })
            .build());

    private WMTabs() {
    }
}
