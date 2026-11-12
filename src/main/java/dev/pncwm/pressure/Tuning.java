package dev.pncwm.pressure;

import java.util.Collection;
import java.util.HashSet;

/** Pure number rules shared by block entities. No game classes, so they can be unit tested. */
public final class Tuning {
    /** Seal points at which a spare seal in the seal slot is fitted. */
    public static final int SEAL_SWAP_AT = 800;

    /** Certified machines use this share of air and seal wear (25% discount). */
    public static final float CERTIFIED_COST = 0.75f;

    private Tuning() {
    }

    /** Comparator strength 0-15 for a pressure judged against a safe pressure. Any positive pressure gives at least 1. */
    public static int pressureSignal(float pressure, float safeBar) {
        if (pressure <= 0.01f || safeBar <= 0f) {
            return 0;
        }
        float ratio = Math.min(1f, pressure / safeBar);
        return Math.min(15, 1 + (int) Math.floor(ratio * 14f));
    }

    /** Comparator strength 0-15 for batch progress. 0 when nothing is running. */
    public static int progressSignal(int progress, int time, boolean running) {
        if (!running || time <= 0) {
            return 0;
        }
        int scaled = 1 + (int) ((long) Math.max(0, progress) * 14L / time);
        return Math.max(1, Math.min(15, scaled));
    }

    /** Fresh seal life for a seal of the given grade index. G2 lasts 1000 ticks of running, each grade above adds 500. */
    public static int sealLife(int gradeIndex) {
        int clamped = Math.max(2, Math.min(5, gradeIndex));
        return 1000 + 500 * (clamped - 2);
    }

    /** Air the regulator skid bleeds off in one tick when the shell sits above its setpoint. */
    public static int regulatorBleed(float pressure, float target, int volume) {
        if (pressure <= target + 0.5f || volume <= 0) {
            return 0;
        }
        return Math.min(600, Math.round((pressure - target) * volume * 0.2f));
    }

    /** Share of damage kept by a pressure curtain of the given grade index. Higher grades absorb more. */
    public static float curtainKept(int gradeIndex) {
        return switch (gradeIndex) {
            case 2 -> 0.85f;
            case 3 -> 0.7f;
            case 4 -> 0.55f;
            case 5 -> 0.4f;
            default -> 0.9f;
        };
    }

    /** How many different positions are in a list (packed block positions). Repeats count once. */
    public static int distinct(Collection<Long> packed) {
        return new HashSet<>(packed).size();
    }

    /** Air (or other integer) cost after a certificate is applied. Always at least 1 if the original cost was positive. */
    public static int certifiedCost(int amount, boolean certified) {
        if (amount <= 0 || !certified) {
            return Math.max(0, amount);
        }
        return Math.max(1, Math.round(amount * CERTIFIED_COST));
    }

    /** Seal points worn in one running tick. A certificate cuts wear by 25%, but a running tick still wears at least 1. */
    public static int sealWear(double leakMultiplier, boolean certified) {
        int wear = (int) Math.max(1, Math.round(leakMultiplier));
        return certifiedCost(wear, certified);
    }

    /**
     * Throughput multiplier for a G2-G5 upgrade part (pump or slide). Missing or below G2 leaves the base rate of 1.
     * G2 doubles it, G5 is five times the base rate.
     */
    public static int gradeMultiplier(int gradeIndex) {
        if (gradeIndex < 2) {
            return 1;
        }
        return Math.min(5, gradeIndex);
    }

    /**
     * {@link #gradeMultiplier} if {@code path} ends with {@code suffix}, otherwise the base rate of 1.
     * Used for vane pumps ({@code _pump}) and slides ({@code _slide}).
     */
    public static int partMultiplier(String path, String suffix, int gradeIndex) {
        if (path == null || suffix == null || !path.endsWith(suffix)) {
            return 1;
        }
        return gradeMultiplier(gradeIndex);
    }

    /**
     * Gases vs liquids, from a fluid registry path (no namespace). Used so a gas hatch refuses oil and a fluid hatch
     * refuses hydrogen, and so the flare stack can burn any waste gas.
     */
    public static boolean isGasPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String name = path.startsWith("flowing_") ? path.substring("flowing_".length()) : path;
        if (name.contains("gas") || name.contains("steam") || name.contains("mist")
                || name.contains("vapour") || name.contains("vapor")) {
            return true;
        }
        return switch (name) {
            case "hydrogen", "chlorine", "nitrogen", "pure_nitrogen", "argon", "oxygen", "inerts" -> true;
            default -> false;
        };
    }

    /** How many full buckets a tank of this size drops when a controller is broken. The leftover millibuckets are lost. */
    public static int bucketsFromMilli(int milliBuckets) {
        if (milliBuckets < 1000) {
            return 0;
        }
        return milliBuckets / 1000;
    }
}
