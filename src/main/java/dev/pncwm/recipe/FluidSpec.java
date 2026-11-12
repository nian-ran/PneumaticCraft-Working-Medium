package dev.pncwm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidSpec(String fluid, int amount, int minSize) {
    public static final Codec<FluidSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("fluid").forGetter(FluidSpec::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidSpec::amount),
            Codec.INT.optionalFieldOf("min_size", 1).forGetter(FluidSpec::minSize)
    ).apply(instance, FluidSpec::new));

    public Fluid resolve() {
        ResourceLocation id = ResourceLocation.tryParse(fluid);
        if (id == null) {
            return Fluids.EMPTY;
        }
        if (!BuiltInRegistries.FLUID.containsKey(id)) {
            return Fluids.EMPTY;
        }
        return BuiltInRegistries.FLUID.get(id);
    }

    public FluidStack stack(int parallel) {
        Fluid resolved = resolve();
        if (resolved == Fluids.EMPTY || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(resolved), amount * parallel);
    }
}
