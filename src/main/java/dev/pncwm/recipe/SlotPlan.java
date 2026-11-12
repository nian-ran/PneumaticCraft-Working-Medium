package dev.pncwm.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Checks that <b>every</b> output of a batch fits into the output slots or tanks at the same time.
 * Kept free of game classes so the merge rules can be unit tested. Items and fluids use the same rule:
 * an output goes into the first empty slot or the first slot holding the same thing, up to that slot's limit,
 * and the rest spills into the next slot, exactly like {@code insertItem} / {@code FluidTank.fill}.
 */
public final class SlotPlan {
    private SlotPlan() {
    }

    /**
     * @param keys      what each slot currently holds, {@code null} for empty (not modified)
     * @param counts    how much each slot holds (not modified)
     * @param limit     how much one slot may hold of a given key
     * @param outKeys   outputs to place, in order
     * @param outCounts amounts for each output
     * @param same      whether two keys can merge
     */
    public static <T> boolean fitsAll(List<T> keys, int[] counts, java.util.function.ToIntFunction<T> limit,
                                      List<T> outKeys, int[] outCounts, BiPredicate<T, T> same) {
        List<T> slotKeys = new ArrayList<>(keys);
        int[] slotCounts = counts.clone();
        for (int out = 0; out < outKeys.size(); out++) {
            T key = outKeys.get(out);
            int left = outCounts[out];
            if (key == null || left <= 0) {
                continue;
            }
            for (int slot = 0; slot < slotKeys.size() && left > 0; slot++) {
                T held = slotKeys.get(slot);
                if (held != null && slotCounts[slot] > 0 && !same.test(held, key)) {
                    continue;
                }
                int room = limit.applyAsInt(key) - (held == null || slotCounts[slot] <= 0 ? 0 : slotCounts[slot]);
                if (room <= 0) {
                    continue;
                }
                int step = Math.min(room, left);
                if (held == null || slotCounts[slot] <= 0) {
                    slotKeys.set(slot, key);
                    slotCounts[slot] = 0;
                }
                slotCounts[slot] += step;
                left -= step;
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }
}
