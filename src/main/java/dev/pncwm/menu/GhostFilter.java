package dev.pncwm.menu;

/**
 * Rules for the phantom filter slot. A ghost mark must never enter the inventory as a real item,
 * leftover real items from older worlds must be returned, and placing a mark must not consume
 * the stack in hand.
 */
public final class GhostFilter {
    private GhostFilter() {
    }

    /** What a number-key swap should do with the filter slot and the hotbar stack. */
    public enum Swap {
        /** Both empty, or nothing to change. */
        NONE,
        /** Hotbar empty, slot holds a ghost: just clear the filter. */
        CLEAR_GHOST,
        /** Hotbar empty, slot holds a leftover real item: move it to the hotbar. */
        GIVE_REAL,
        /** Hotbar has an item, slot is empty or already a ghost: place a ghost, keep the hotbar item. */
        PLACE_KEEP,
        /** Hotbar has an item, slot holds a leftover real item: place a ghost and give the real item back. */
        PLACE_RETURN_REAL
    }

    public static Swap swap(boolean slotEmpty, boolean slotGhost, boolean hotbarEmpty) {
        if (hotbarEmpty) {
            if (slotEmpty) {
                return Swap.NONE;
            }
            return slotGhost ? Swap.CLEAR_GHOST : Swap.GIVE_REAL;
        }
        if (slotEmpty || slotGhost) {
            return Swap.PLACE_KEEP;
        }
        return Swap.PLACE_RETURN_REAL;
    }

    /** Double-click gathering must not take a ghost, or it would clear the filter. */
    public static boolean mayPickAll(boolean slotGhost) {
        return !slotGhost;
    }

    /** Creative middle-click must not copy a ghost into the cursor as a full stack. */
    public static boolean mayClone(boolean slotEmpty, boolean slotGhost) {
        return !slotEmpty && !slotGhost;
    }
}
