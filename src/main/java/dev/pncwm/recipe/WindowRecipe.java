package dev.pncwm.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.pncwm.WMConfig;
import dev.pncwm.pressure.WindowBounds;
import dev.pncwm.registry.WMRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class WindowRecipe implements Recipe<RecipeInput> {
    public static final MapCodec<WindowRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("machine").forGetter(recipe -> recipe.machine),
            Ingredient.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(recipe -> recipe.items),
            Codec.INT.listOf().optionalFieldOf("item_counts", List.of()).forGetter(recipe -> recipe.itemCounts),
            FluidSpec.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(recipe -> recipe.fluids),
            ItemOut.CODEC.listOf().optionalFieldOf("item_outputs", List.of()).forGetter(recipe -> recipe.itemOutputs),
            FluidSpec.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(recipe -> recipe.fluidOutputs),
            Codec.FLOAT.fieldOf("pressure_min").forGetter(recipe -> recipe.pressureMin),
            Codec.FLOAT.fieldOf("pressure_max").forGetter(recipe -> recipe.pressureMax),
            Codec.INT.fieldOf("temp_min").forGetter(recipe -> recipe.tempMinC),
            Codec.INT.fieldOf("temp_max").forGetter(recipe -> recipe.tempMaxC),
            Codec.INT.fieldOf("air").forGetter(recipe -> recipe.airPerTick),
            Codec.INT.fieldOf("time").forGetter(recipe -> recipe.time),
            Codec.INT.optionalFieldOf("lube", 0).forGetter(recipe -> recipe.lubeMb),
            Codec.DOUBLE.optionalFieldOf("heat", 0.0).forGetter(recipe -> recipe.heatPerTick),
            Codec.BOOL.optionalFieldOf("void_outside", false).forGetter(recipe -> recipe.voidOutside),
            Codec.INT.optionalFieldOf("min_size", 1).forGetter(recipe -> recipe.minSize)
    ).apply(instance, WindowRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WindowRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public final String machine;
    public final List<Ingredient> items;
    public final List<Integer> itemCounts;
    public final List<FluidSpec> fluids;
    public final List<ItemOut> itemOutputs;
    public final List<FluidSpec> fluidOutputs;
    public final float pressureMin;
    public final float pressureMax;
    public final int tempMinC;
    public final int tempMaxC;
    public final int airPerTick;
    public final int time;
    public final int lubeMb;
    public final double heatPerTick;
    public final boolean voidOutside;
    public final int minSize;

    public WindowRecipe(String machine, List<Ingredient> items, List<Integer> itemCounts, List<FluidSpec> fluids,
                        List<ItemOut> itemOutputs, List<FluidSpec> fluidOutputs, float pressureMin, float pressureMax,
                        int tempMinC, int tempMaxC, int airPerTick, int time, int lubeMb, double heatPerTick,
                        boolean voidOutside, int minSize) {
        this.machine = machine;
        this.items = items;
        this.itemCounts = itemCounts;
        this.fluids = fluids;
        this.itemOutputs = itemOutputs;
        this.fluidOutputs = fluidOutputs;
        this.pressureMin = pressureMin;
        this.pressureMax = pressureMax;
        this.tempMinC = tempMinC;
        this.tempMaxC = tempMaxC;
        this.airPerTick = airPerTick;
        this.time = time;
        this.lubeMb = lubeMb;
        this.heatPerTick = heatPerTick;
        this.voidOutside = voidOutside;
        this.minSize = minSize;
    }

    public int requiredCoils() {
        if (tempMinC >= 800) {
            return 2;
        }
        if (tempMinC >= 300 || tempMaxC <= -40) {
            return 1;
        }
        return 0;
    }

    /** How many must be in the machine. A listed count of 0 still requires one item. */
    public int presentCount(int index) {
        return RecipeCounts.present(itemCounts, index);
    }

    /** How many to consume. A listed count of 0 means held in a dedicated slot, not consumed. */
    public int consumeCount(int index) {
        return RecipeCounts.consume(itemCounts, index);
    }

    public int countFor(int index) {
        return presentCount(index);
    }

    public boolean accepts(float pressure, double celsius) {
        return WindowBounds.contains(pressureMin, pressureMax, tempMinC, tempMaxC, pressure, celsius, WMConfig.tightness());
    }

    public float pressureLow() {
        return WindowBounds.edge(pressureMin, pressureMax, true, WMConfig.tightness());
    }

    public float pressureHigh() {
        return WindowBounds.edge(pressureMin, pressureMax, false, WMConfig.tightness());
    }

    public int tempLow() {
        return Math.round(WindowBounds.edge(tempMinC, tempMaxC, true, WMConfig.tightness()));
    }

    public int tempHigh() {
        return Math.round(WindowBounds.edge(tempMinC, tempMaxC, false, WMConfig.tightness()));
    }

    public ItemStack result() {
        if (itemOutputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return itemOutputs.getFirst().stack(1);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WMRecipes.WINDOW_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return WMRecipes.WINDOW.get();
    }
}
