package dev.pncwm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ItemOut(String item, int count, int minSize) {
    public static final Codec<ItemOut> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("item").forGetter(ItemOut::item),
            Codec.INT.fieldOf("count").forGetter(ItemOut::count),
            Codec.INT.optionalFieldOf("min_size", 1).forGetter(ItemOut::minSize)
    ).apply(instance, ItemOut::new));

    public ItemStack stack(int parallel) {
        ResourceLocation id = ResourceLocation.tryParse(item);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id), Math.max(1, count) * parallel);
    }
}
