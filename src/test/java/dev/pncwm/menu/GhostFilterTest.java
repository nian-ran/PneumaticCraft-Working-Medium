package dev.pncwm.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GhostFilterTest {
    @Test
    void numberKeySwapDoesNotFabricateOrConsume() {
        assertEquals(GhostFilter.Swap.NONE, GhostFilter.swap(true, false, true));
        assertEquals(GhostFilter.Swap.CLEAR_GHOST, GhostFilter.swap(false, true, true));
        assertEquals(GhostFilter.Swap.GIVE_REAL, GhostFilter.swap(false, false, true));
        assertEquals(GhostFilter.Swap.PLACE_KEEP, GhostFilter.swap(true, false, false));
        assertEquals(GhostFilter.Swap.PLACE_KEEP, GhostFilter.swap(false, true, false));
        assertEquals(GhostFilter.Swap.PLACE_RETURN_REAL, GhostFilter.swap(false, false, false));
    }

    @Test
    void pickAllAndCloneSkipGhosts() {
        assertFalse(GhostFilter.mayPickAll(true));
        assertTrue(GhostFilter.mayPickAll(false));
        assertFalse(GhostFilter.mayClone(true, false));
        assertFalse(GhostFilter.mayClone(false, true));
        assertTrue(GhostFilter.mayClone(false, false));
    }
}
