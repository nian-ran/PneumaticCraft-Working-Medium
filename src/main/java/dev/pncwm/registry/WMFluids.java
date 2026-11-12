package dev.pncwm.registry;

import java.util.ArrayList;
import java.util.List;
import dev.pncwm.WorkingMedium;
import dev.pncwm.block.HazardLiquidBlock;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class WMFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, WorkingMedium.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, WorkingMedium.MOD_ID);
    public static final List<FluidSet> ALL = new ArrayList<>();

    public static final FluidSet BRINE = set("brine", 0xFF8FB7C9, 300);
    public static final FluidSet CHLORINE = set("chlorine", 0xFFD6F5A3, 280);
    public static final FluidSet HYDROGEN = set("hydrogen", 0xFFE8F7FF, 280);
    public static final FluidSet CAUSTIC = set("caustic", 0xFFE7E2C5, 320);
    public static final FluidSet MONOMER = set("monomer", 0xFFD7C4A3, 320);
    public static final FluidSet HYDROCHLORIC = set("hydrochloric", 0xFFB7E28A, 300);
    public static final FluidSet ACID_MIST = set("acid_mist", 0xFF9CBA62, 310);
    public static final FluidSet METAL_SOLUTION = set("metal_solution", 0xFF6FA8A0, 320);
    public static final FluidSet BYPRODUCT_SOLUTION = set("byproduct_solution", 0xFFC47B4A, 320);
    public static final FluidSet FLUE_GAS = set("flue_gas", 0xFF9AA0A6, 450);
    public static final FluidSet NITROGEN = set("nitrogen", 0xFFD5E7F2, 200);
    public static final FluidSet PURE_NITROGEN = set("pure_nitrogen", 0xFFF4FBFF, 120);
    public static final FluidSet ARGON = set("argon", 0xFFC9B6E8, 180);
    public static final FluidSet OXYGEN = set("oxygen", 0xFFB9D7FF, 180);
    public static final FluidSet INERTS = set("inerts", 0xFFE7E0C8, 200);
    public static final FluidSet LIQUID_CO2 = set("liquid_co2", 0xFFD6FFF6, 250);
    public static final FluidSet DUSTY_STEAM = set("dusty_steam", 0xFFEEEAE4, 400);
    public static final FluidSet ACID_FEED = set("acid_feed", 0xFFC6D36A, 330);

    private WMFluids() {
    }

    private static FluidSet set(String name, int tint, int temperature) {
        FluidSet fluid = new FluidSet(name, tint, temperature);
        ALL.add(fluid);
        return fluid;
    }

    public static final class FluidSet {
        public final String name;
        public final int tint;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<LiquidBlock> block;
        public final DeferredItem<BucketItem> bucket;

        private FluidSet(String name, int tint, int temperature) {
            this.name = name;
            this.tint = tint;
            type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .density(temperature > 350 ? 400 : 900)
                    .viscosity(temperature > 350 ? 200 : 1000)
                    .temperature(temperature)
                    .descriptionId("fluid_type.workingmedium." + name)));
            source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
            block = WMBlocks.BLOCKS.register(name, () -> new HazardLiquidBlock(source.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER).noCollission().strength(100f).noLootTable().replaceable().liquid()));
            bucket = WMItems.ITEMS.register(name + "_bucket", () -> new BucketItem(source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            WMItems.ITEMS.registerSimpleBlockItem(block);
            WMBlocks.ALL.add(block);
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .bucket(bucket)
                    .block(block)
                    .tickRate(5);
        }

        }
}
