package dev.pncwm.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class SlotPlanTest {
    private static boolean fits(List<String> slots, int[] counts, int limit, List<String> outs, int[] outCounts) {
        return SlotPlan.fitsAll(slots, counts, key -> limit, outs, outCounts, String::equals);
    }

    @Test
    void emptySlotsTakeEveryOutput() {
        assertTrue(fits(Arrays.asList(null, null, null, null), new int[4], 64,
                List.of("ingot", "slag", "nugget"), new int[] {2, 1, 1}));
    }

    @Test
    void twoOutputsCannotShareTheLastEmptySlot() {
        // Three slots hold other things; one empty slot left. Checked alone, each output fits;
        // together they do not, which is the overflow that used to void the second output.
        List<String> slots = Arrays.asList("a", "b", "c", null);
        int[] counts = {64, 64, 64, 0};
        assertTrue(fits(slots, counts, 64, List.of("ingot"), new int[] {2}));
        assertTrue(fits(slots, counts, 64, List.of("slag"), new int[] {1}));
        assertFalse(fits(slots, counts, 64, List.of("ingot", "slag"), new int[] {2, 1}));
    }

    @Test
    void mergesIntoMatchingSlotsAndSpills() {
        List<String> slots = Arrays.asList("ingot", null, "slag", "slag");
        int[] counts = {63, 0, 64, 64};
        assertTrue(fits(slots, counts, 64, List.of("ingot"), new int[] {2}));
        assertFalse(fits(slots, counts, 64, List.of("ingot", "slag"), new int[] {2, 1}));
    }

    @Test
    void fluidTanksUseCapacity() {
        List<String> tanks = Arrays.asList("brine", null, null, null);
        int[] amounts = {15_900, 0, 0, 0};
        assertTrue(fits(tanks, amounts, 16_000, List.of("brine", "chlorine", "hydrogen"), new int[] {200, 200, 100}));
        List<String> full = Arrays.asList("x", "y", "z", null);
        int[] fullAmounts = {16_000, 16_000, 16_000, 0};
        assertFalse(fits(full, fullAmounts, 16_000, List.of("chlorine", "hydrogen"), new int[] {200, 100}));
    }

    @Test
    void doesNotModifyInputs() {
        List<String> slots = Arrays.asList(null, null, null, null);
        int[] counts = new int[4];
        fits(slots, counts, 64, List.of("ingot"), new int[] {10});
        assertTrue(slots.stream().allMatch(s -> s == null));
        assertTrue(Arrays.stream(counts).allMatch(c -> c == 0));
    }
}
