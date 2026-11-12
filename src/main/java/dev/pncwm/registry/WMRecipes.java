package dev.pncwm.registry;

import dev.pncwm.WorkingMedium;
import dev.pncwm.recipe.WindowRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, WorkingMedium.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, WorkingMedium.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<WindowRecipe>> WINDOW = TYPES.register("window", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return "workingmedium:window";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WindowRecipe>> WINDOW_SERIALIZER =
            SERIALIZERS.register("window", () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<WindowRecipe> codec() {
                    return WindowRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, WindowRecipe> streamCodec() {
                    return WindowRecipe.STREAM_CODEC;
                }
            });

    private WMRecipes() {
    }
}
