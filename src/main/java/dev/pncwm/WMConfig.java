package dev.pncwm;

import dev.pncwm.pressure.WindowBounds;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class WMConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue EXPERT;
    public static final ModConfigSpec.DoubleValue WINDOW_TIGHTNESS;
    public static final ModConfigSpec.DoubleValue AIR_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue LEAK_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue SELL_GATE_CHEMICALS;
    public static final ModConfigSpec.IntValue CERTIFICATION_TICKS;
    public static final ModConfigSpec.IntValue TOWER_EXTRA_TRAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("PneumaticCraft: Working Medium").push("process");
        EXPERT = builder.comment("Tighten windows, raise air use and leaks, and stop the catalog selling gate chemicals.").define("expert", false);
        WINDOW_TIGHTNESS = builder.comment("1 shrinks each pressure and temperature window toward its center. 0 leaves the authored window.").defineInRange("windowTightness", 0.0, 0.0, 0.8);
        AIR_MULTIPLIER = builder.defineInRange("airMultiplier", 1.0, 0.1, 10.0);
        LEAK_MULTIPLIER = builder.defineInRange("leakMultiplier", 1.0, 0.0, 10.0);
        SELL_GATE_CHEMICALS = builder.comment("Catalog and the intent of Amadron: sell chemicals the player has not automated yet.").define("sellGateChemicals", true);
        CERTIFICATION_TICKS = builder.comment("How long the calibration bench must see three steady machines. 20 ticks is one second.").defineInRange("certificationTicks", 6000, 200, 72000);
        TOWER_EXTRA_TRAYS = builder.comment("Added to the minimum tray count of every tower. Expert presets raise this.").defineInRange("towerExtraTrays", 0, 0, 4);
        builder.pop();
        SPEC = builder.build();
    }

    private WMConfig() {
    }

    public static boolean sellGateChemicals() {
        return SELL_GATE_CHEMICALS.get() && !EXPERT.get();
    }

    public static double tightness() {
        return EXPERT.get() ? Math.max(WINDOW_TIGHTNESS.get(), WindowBounds.EXPERT_FLOOR) : WINDOW_TIGHTNESS.get();
    }

    public static double airMultiplier() {
        return AIR_MULTIPLIER.get() * (EXPERT.get() ? 1.5 : 1.0);
    }

    public static double leakMultiplier() {
        return LEAK_MULTIPLIER.get() * (EXPERT.get() ? 2.0 : 1.0);
    }

    public static int towerExtra() {
        return TOWER_EXTRA_TRAYS.get() + (EXPERT.get() ? 1 : 0);
    }
}
