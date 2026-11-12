package dev.pncwm.pressure;

/**
 * The pressure and temperature window check used by apparatus ticks.
 * Tightness of 0 keeps the authored band. The expert floor pulls both edges toward the center.
 */
public final class WindowBounds {
    /** Floor applied when the expert preset is on. Matches {@code WMConfig.tightness()}. */
    public static final double EXPERT_FLOOR = 0.35;

    private WindowBounds() {
    }

    public static boolean contains(float pressureMin, float pressureMax, int tempMin, int tempMax,
                                   float pressure, double celsius, double tightness) {
        float pressureLow = edge(pressureMin, pressureMax, true, tightness);
        float pressureHigh = edge(pressureMin, pressureMax, false, tightness);
        float tempLow = edge(tempMin, tempMax, true, tightness);
        float tempHigh = edge(tempMin, tempMax, false, tightness);
        return pressure >= pressureLow && pressure <= pressureHigh
                && celsius >= tempLow && celsius <= tempHigh;
    }

    public static float edge(float low, float high, boolean wantLow, double tightness) {
        float clamped = (float) Math.max(0.0, Math.min(0.8, tightness));
        float mid = (low + high) * 0.5f;
        float half = (high - low) * 0.5f * (1f - clamped);
        return wantLow ? mid - half : mid + half;
    }
}
