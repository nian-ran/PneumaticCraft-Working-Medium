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

    /** Seal points worn in one running tick. A certificate cuts wear by 25%, a nitrogen purge halves it, but a running tick still wears at least 1. */
    public static int sealWear(double leakMultiplier, boolean certified) {
        return sealWear(leakMultiplier, certified, false);
    }

    public static int sealWear(double leakMultiplier, boolean certified, boolean purged) {
        double factor = purged ? 0.5 : 1.0;
        int wear = (int) Math.max(1, Math.round(leakMultiplier * factor));
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
        String name = fluidName(path);
        if (name.isEmpty()) {
            return false;
        }
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

    /**
     * Air added to the shell this tick. A positive recipe {@code air} value is a cost (this returns a negative
     * change). A negative recipe value, as used by the cycle island, produces air. Certified machines consume
     * 25% less and produce about 33% more. A positive cost of 0 produces no change.
     */
    public static int airChange(int airPerTick, int parallel, double multiplier, boolean certified) {
        int raw = (int) Math.round(airPerTick * Math.max(1, parallel) * multiplier);
        if (raw > 0) {
            return -certifiedCost(raw, certified);
        }
        if (raw < 0) {
            int produced = -raw;
            if (certified) {
                produced = Math.max(produced, Math.round(produced / CERTIFIED_COST));
            }
            return produced;
        }
        return 0;
    }

    /**
     * Heat moved from a hotter body to a colder one this tick. {@code fraction} is the share of the temperature
     * gap (0-1). The move is capped, and a tiny gap is ignored so the two sides do not chatter.
     */
    public static double heatMove(double fromTemp, double toTemp, double fraction, double cap) {
        if (cap <= 0 || fraction <= 0) {
            return 0;
        }
        double gap = fromTemp - toTemp;
        if (Math.abs(gap) < 0.25) {
            return 0;
        }
        double move = gap * Math.min(1.0, fraction);
        if (Math.abs(move) > cap) {
            return Math.copySign(cap, move);
        }
        return move;
    }

    /**
     * How much of the temperature gap a heat gallery transfers each tick. Longer galleries are slower.
     * A 6-bay gallery is the baseline; shorter ones move heat faster.
     */
    public static double galleryFraction(int size) {
        int length = Math.max(1, size);
        return 0.22 / (1.0 + 0.1 * (length - 1));
    }

    /** Heat added to a flare stack from the millibuckets of gas it just burned. */
    public static double flareHeat(int milliBuckets) {
        if (milliBuckets <= 0) {
            return 0;
        }
        return milliBuckets * 0.12;
    }

    /**
     * Damage applied every 10 ticks to a living thing standing in this fluid (by registry path, no namespace).
     * Zero means the fluid is harmless.
     */
    public static float fluidHarm(String path) {
        String name = fluidName(path);
        if (name.isEmpty()) {
            return 0f;
        }
        return switch (name) {
            case "chlorine", "acid_mist" -> 2f;
            case "hydrochloric", "caustic" -> 3f;
            case "acid_feed" -> 2f;
            case "flue_gas" -> 1f;
            default -> 0f;
        };
    }

    /** Poison duration in ticks for a hazardous gas or mist. Zero if it only deals damage. */
    public static int fluidPoisonTicks(String path) {
        String name = fluidName(path);
        if (name.isEmpty()) {
            return 0;
        }
        return switch (name) {
            case "chlorine", "acid_mist" -> 80;
            default -> 0;
        };
    }

    /**
     * Dedicated machine slot for an item path (no namespace). Seals, catalyst, electrodes, program sheets and
     * the upgrade parts (pump, slide, nozzle, cylinder, diaphragm, gripper) each have a home. Returns -1 otherwise.
     */
    public static int dedicatedSlotForPath(String path) {
        if (path == null || path.isEmpty()) {
            return -1;
        }
        if (path.endsWith("_seal")) {
            return 8;
        }
        if (path.equals("catalyst")) {
            return 9;
        }
        if (path.equals("electrode") || path.equals("program_sheet")
                || path.endsWith("_pump") || path.endsWith("_slide")
                || path.endsWith("_nozzle") || path.endsWith("_cylinder")
                || path.endsWith("_diaphragm") || path.endsWith("_gripper")) {
            return 10;
        }
        return -1;
    }

    /**
     * Share of regulator inlet air that arrives at the outlet. The base rate is 0.80. A diaphragm in the special
     * slot raises it (G2 is 0.84, G5 is 0.96). A missing or wrong part leaves the base rate.
     */
    public static float regulatorEfficiency(int diaphragmMultiplier) {
        int steps = Math.max(0, diaphragmMultiplier - 1);
        return Math.min(0.96f, 0.80f + 0.04f * steps);
    }

    /** How many uses a fresh electrode has in the electrolyzer. */
    public static int electrodeLife() {
        return 64;
    }

    /** Millibuckets of purge gas consumed every 20 running ticks when a purge is active. */
    public static final int PURGE_MB = 20;

    public static String fluidName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String name = path;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        return name.startsWith("flowing_") ? name.substring("flowing_".length()) : name;
    }

    /**
     * Gases the flare may actually burn. Nitrogen, argon, oxygen, pure nitrogen and inerts are products,
     * not fuel, and must be refused.
     */
    public static boolean isCombustibleGas(String path) {
        return switch (fluidName(path)) {
            case "hydrogen", "flue_gas", "acid_mist", "chlorine", "dusty_steam" -> true;
            default -> false;
        };
    }

    /** Inert cover gas that can purge a running machine and cut seal wear in half. */
    public static boolean isPurgeGas(String path) {
        return switch (fluidName(path)) {
            case "nitrogen", "pure_nitrogen", "argon", "inerts" -> true;
            default -> false;
        };
    }

    /** Cold process fluids that freeze living things standing in them. */
    public static boolean isCryogenic(String path) {
        return switch (fluidName(path)) {
            case "liquid_co2", "nitrogen", "pure_nitrogen", "argon", "oxygen" -> true;
            default -> false;
        };
    }

    /** Slowness duration in ticks for a cryogenic fluid. Zero if it is not cold. */
    public static int fluidSlowTicks(String path) {
        return isCryogenic(path) ? 80 : 0;
    }

    /** Freeze damage applied every 10 ticks in a cryogenic fluid. Zero if it is not cold. */
    public static float fluidFreeze(String path) {
        return isCryogenic(path) ? 1f : 0f;
    }

    /**
     * Whether a recipe fluid output must leave through a drain hatch. Matches the fluids that hurt
     * anything standing in them (acid, caustic, chlorine, flue gas).
     */
    public static boolean needsDrain(String path) {
        return fluidHarm(path) > 0f;
    }

    /** Comparator strength 0-15 for how full a group of slots or tanks is. Any content gives at least 1. */
    public static int fillSignal(int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        int scaled = 1 + (int) ((long) Math.min(amount, capacity) * 14L / capacity);
        return Math.max(1, Math.min(15, scaled));
    }

    /**
     * Outlet share after a certificate is applied. A certificate cuts the lost fraction by 25%
     * (0.80 becomes 0.85, 0.96 becomes 0.97).
     */
    public static float certifiedEfficiency(float base, boolean certified) {
        float clamped = Math.min(1f, Math.max(0f, base));
        if (!certified) {
            return clamped;
        }
        return 1f - (1f - clamped) * CERTIFIED_COST;
    }

    /** Integer rate after a certificate raises throughput. Always at least the original positive amount. */
    public static int certifiedBoost(int amount, boolean certified) {
        if (amount <= 0 || !certified) {
            return Math.max(0, amount);
        }
        return Math.max(amount, Math.round(amount / CERTIFIED_COST));
    }
}
