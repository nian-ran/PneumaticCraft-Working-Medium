package dev.pncwm.pressure;

import com.mojang.serialization.Codec;

import me.desht.pneumaticcraft.api.pressure.PressureTier;
import net.minecraft.util.StringRepresentable;

/**
 * Six pressure grades. G0-G4 are rising safe pressures. G5 is the supercritical window, and it is not a higher tube rating.
 */
public enum PressureGrade implements PressureTier, StringRepresentable {
    G0("g0", 0, 5f, 7f, 0xFFC8C8C8),
    G1("g1", 1, 10f, 12f, 0xFFE39B56),
    G2("g2", 2, 20f, 25f, 0xFF8AA0B2),
    G3("g3", 3, 50f, 58f, 0xFFD2B48C),
    G4("g4", 4, 120f, 135f, 0xFF7ED0D6),
    G5("g5", 5, 85f, 90f, 0xFFF2F7FF);

    public final String id;
    public final int index;
    public final float safeBar;
    public final float criticalBar;
    public static final Codec<PressureGrade> CODEC = StringRepresentable.fromEnum(PressureGrade::values);

    public final int color;

    PressureGrade(String id, int index, float safeBar, float criticalBar, int color) {
        this.id = id;
        this.index = index;
        this.safeBar = safeBar;
        this.criticalBar = criticalBar;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    @Override
    public float getDangerPressure() {
        return safeBar;
    }

    @Override
    public float getCriticalPressure() {
        return criticalBar;
    }

    public PressureGrade next() {
        if (this == G4 || this == G5) {
            return G4;
        }
        return values()[index + 1];
    }

    public PressureGrade previous() {
        return index == 0 ? G0 : values()[index - 1];
    }

    /**
     * Highest rising grade whose safe pressure the reading has reached. G5 is a window, so it is never selected here.
     */
    public static PressureGrade fromPressure(float bar) {
        PressureGrade found = G0;
        for (PressureGrade grade : values()) {
            if (grade == G5) {
                continue;
            }
            if (bar + 0.02f >= grade.safeBar) {
                found = grade;
            }
        }
        return found;
    }

    public static PressureGrade byIndex(int index) {
        if (index < 0) {
            return G0;
        }
        if (index >= values().length) {
            return G5;
        }
        return values()[index];
    }

    public static int celsiusToKelvin(double celsius) {
        return (int) Math.round(celsius + 273.15);
    }

    public static int kelvinToCelsius(double kelvin) {
        return (int) Math.round(kelvin - 273.15);
    }
}
