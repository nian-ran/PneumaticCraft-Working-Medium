package dev.pncwm.registry;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.pncwm.WorkingMedium;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WMData {
    public static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WorkingMedium.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE = TYPES.register("charge",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEAT = TYPES.register("heat",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> PROBE_POS = TYPES.register("probe_pos",
            () -> DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CLEANED = TYPES.register("cleaned",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    /** Marks a ghost copy in a machine filter slot so the real item is not consumed or dropped. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GHOST = TYPES.register("ghost",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<StoredFluid>>> MACHINE_FLUIDS = TYPES.register("machine_fluids",
            () -> DataComponentType.<List<StoredFluid>>builder()
                    .persistent(StoredFluid.CODEC.listOf())
                    .networkSynchronized(StoredFluid.STREAM.apply(ByteBufCodecs.list()))
                    .build());

    private WMData() {
    }

    /** One tank's leftover fluid, written onto a dropped or middle-clicked controller. */
    public record StoredFluid(int tank, String fluid, int amount) {
        public static final Codec<StoredFluid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("tank").forGetter(StoredFluid::tank),
                Codec.STRING.fieldOf("fluid").forGetter(StoredFluid::fluid),
                Codec.INT.fieldOf("amount").forGetter(StoredFluid::amount)
        ).apply(instance, StoredFluid::new));

        public static final StreamCodec<FriendlyByteBuf, StoredFluid> STREAM = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StoredFluid::tank,
                ByteBufCodecs.STRING_UTF8, StoredFluid::fluid,
                ByteBufCodecs.VAR_INT, StoredFluid::amount,
                StoredFluid::new);
    }
}
