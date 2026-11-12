package dev.pncwm.pressure;

import net.minecraft.util.StringRepresentable;

public enum Band implements StringRepresentable {
    OFF("off", 0xFF555555),
    GREEN("green", 0xFF3DDC97),
    YELLOW("yellow", 0xFFE0C040),
    RED("red", 0xFFDD4455);

    public final String name;
    public final int color;

    Band(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Band of(float pressure, float low, float high, float danger) {
        if (pressure < 0.05f) {
            return OFF;
        }
        if (pressure > danger || pressure > high) {
            return RED;
        }
        if (pressure < low) {
            return YELLOW;
        }
        float span = Math.max(0.1f, high - low);
        float edge = span * 0.15f;
        if (pressure < low + edge || pressure > high - edge) {
            return YELLOW;
        }
        return GREEN;
    }
}
