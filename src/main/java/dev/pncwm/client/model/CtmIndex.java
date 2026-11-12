package dev.pncwm.client.model;

/**
 * Compact CTM sheet addressing. A 64x64 sheet holds 16 tiles in mask order:
 * bit 0 = texture-up, bit 1 = right, bit 2 = down, bit 3 = left.
 */
public final class CtmIndex {
    public static final int UP = 1;
    public static final int RIGHT = 2;
    public static final int DOWN = 4;
    public static final int LEFT = 8;

    private CtmIndex() {
    }

    public static int pack(boolean up, boolean right, boolean down, boolean left) {
        int mask = 0;
        if (up) {
            mask |= UP;
        }
        if (right) {
            mask |= RIGHT;
        }
        if (down) {
            mask |= DOWN;
        }
        if (left) {
            mask |= LEFT;
        }
        return mask;
    }

    public static int u0(int mask) {
        return (mask & 3) * 4;
    }

    public static int v0(int mask) {
        return (mask >> 2) * 4;
    }
}
