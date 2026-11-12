package dev.pncwm.block;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum HatchType implements StringRepresentable {
    ITEM_IN,
    ITEM_OUT,
    FLUID,
    GAS,
    HEAT,
    VENT,
    DRAIN,
    GAUGE,
    MANIFOLD;

    public static final Codec<HatchType> CODEC = StringRepresentable.fromEnum(HatchType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
