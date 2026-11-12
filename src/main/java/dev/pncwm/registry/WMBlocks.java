package dev.pncwm.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.pncwm.WorkingMedium;
import dev.pncwm.apparatus.ApparatusKind;
import dev.pncwm.block.ApparatusBlock;
import dev.pncwm.block.AsphaltBlock;
import dev.pncwm.block.CasingBlock;
import dev.pncwm.block.CatalogBlock;
import dev.pncwm.block.GaugeBlock;
import dev.pncwm.block.GradeTubeBlock;
import dev.pncwm.block.HatchBlock;
import dev.pncwm.block.HatchType;
import dev.pncwm.block.PartBlock;
import dev.pncwm.block.PartKind;
import dev.pncwm.block.ReliefValveBlock;
import dev.pncwm.block.TransferBlock;
import dev.pncwm.pressure.PressureGrade;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WorkingMedium.MOD_ID);
    public static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();

    public static final Map<PressureGrade, DeferredBlock<CasingBlock>> CASINGS = new EnumMap<>(PressureGrade.class);
    public static final Map<HatchType, DeferredBlock<HatchBlock>> HATCHES = new EnumMap<>(HatchType.class);
    public static final Map<PartKind, DeferredBlock<PartBlock>> PARTS = new EnumMap<>(PartKind.class);
    public static final Map<ApparatusKind, DeferredBlock<ApparatusBlock>> CONTROLLERS = new EnumMap<>(ApparatusKind.class);

    public static final DeferredBlock<GaugeBlock> GAUGE = register("gauge", () -> new GaugeBlock(metal()));
    public static final DeferredBlock<TransferBlock> REDUCER = register("reducer", () -> new TransferBlock(false, metal()));
    public static final DeferredBlock<TransferBlock> BOOSTER = register("booster", () -> new TransferBlock(true, metal()));
    public static final DeferredBlock<ReliefValveBlock> RELIEF = register("relief_valve", () -> new ReliefValveBlock(metal()));
    public static final DeferredBlock<GradeTubeBlock> INDUSTRIAL_TUBE = register("industrial_tube", () -> new GradeTubeBlock(PressureGrade.G3, metal()));
    public static final DeferredBlock<GradeTubeBlock> CRYO_TUBE = register("cryo_tube", () -> new GradeTubeBlock(PressureGrade.G4, metal()));
    public static final DeferredBlock<CatalogBlock> CATALOG = register("catalog", () -> new CatalogBlock(metal()));
    public static final DeferredBlock<AsphaltBlock> ASPHALT = register("asphalt", () -> new AsphaltBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 4f).sound(SoundType.DEEPSLATE).speedFactor(1.35f)));

    static {
        for (PressureGrade grade : PressureGrade.values()) {
            CASINGS.put(grade, register("casing_" + grade.id, () -> new CasingBlock(grade, metal())));
        }
        for (HatchType type : HatchType.values()) {
            HATCHES.put(type, register("hatch_" + type.name().toLowerCase(), () -> new HatchBlock(type, metal())));
        }
        for (PartKind kind : PartKind.values()) {
            PARTS.put(kind, register("part_" + kind.name().toLowerCase(), () -> new PartBlock(kind, metal())));
        }
        for (ApparatusKind kind : ApparatusKind.values()) {
            CONTROLLERS.put(kind, register(kind.id, () -> new ApparatusBlock(kind, metal())));
        }
    }

    private WMBlocks() {
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 8f).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static <T extends Block> DeferredBlock<T> register(String name, java.util.function.Supplier<T> supplier) {
        DeferredBlock<T> block = BLOCKS.register(name, supplier);
        ALL.add(block);
        return block;
    }
}
