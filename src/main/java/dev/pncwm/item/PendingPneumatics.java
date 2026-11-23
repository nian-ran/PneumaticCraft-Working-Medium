package dev.pncwm.item;

/**
 * Air held on an unformed controller so a large charged shell is not dumped into the small
 * unformed volume (which would spike pressure). Negative air (vacuum) is held the same way,
 * so shrinking the volume cannot deepen the vacuum, and the amount is restored after the
 * shell forms again. Temperature is intensive and is not held here.
 */
public final class PendingPneumatics {
    private int air;

    public int air() {
        return air;
    }

    public boolean isEmpty() {
        return air == 0;
    }

    /** Remember air taken from a placed or reloaded item, including a stored vacuum. */
    public void holdFromItem(int amount) {
        air = amount;
    }

    /** After the shell unforms, move the handler's air here before the volume shrinks. */
    public void holdFromHandler(int handlerAir) {
        if (handlerAir != 0) {
            air += handlerAir;
        }
    }

    /** Once the shell has formed and the volume is the formed size, give this air to the handler. */
    public int releaseOnForm() {
        int released = air;
        air = 0;
        return released;
    }

    /** Total air to write onto a dropped or middle-clicked controller, including a stored vacuum. */
    public int itemAir(int handlerAir) {
        return handlerAir + air;
    }

    public void load(int amount) {
        air = amount;
    }
}
