package dev.pncwm.recipe;

import java.util.List;

/** Pure count rules for window recipes, kept free of game classes so they can be unit tested. */
public final class RecipeCounts {
    private RecipeCounts() {
    }

    static int raw(List<Integer> counts, int index) {
        if (index < 0 || index >= counts.size()) {
            return 1;
        }
        return counts.get(index);
    }

    /** How many must be in the machine. A listed count of 0 still requires one item. */
    public static int present(List<Integer> counts, int index) {
        return Math.max(1, raw(counts, index));
    }

    /** How many to consume. A listed count of 0 means held in a dedicated slot, not consumed. */
    public static int consume(List<Integer> counts, int index) {
        return Math.max(0, raw(counts, index));
    }
}
