package dev.pncwm.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PendingPneumaticsTest {
    @Test
    void airFromAnItemStaysPendingUntilTheShellForms() {
        PendingPneumatics pending = new PendingPneumatics();
        pending.holdFromItem(100_000);
        assertEquals(100_000, pending.air());
        assertEquals(100_000, pending.itemAir(0));
        assertEquals(100_000, pending.releaseOnForm());
        assertTrue(pending.isEmpty());
        assertEquals(0, pending.releaseOnForm());
        assertEquals(44_000, pending.itemAir(44_000));
    }

    @Test
    void unformingMovesHandlerAirIntoPendingAndSumsOnPickup() {
        PendingPneumatics pending = new PendingPneumatics();
        pending.holdFromHandler(80_000);
        assertEquals(80_000, pending.air());
        assertEquals(80_000, pending.itemAir(0));
        pending.holdFromHandler(500);
        assertEquals(80_500, pending.itemAir(0));
        // A later fill of the unformed handler is added on pickup, not lost.
        assertEquals(80_700, pending.itemAir(200));
    }

    @Test
    void negativeOrZeroAirIsIgnoredAndReloadRestoresTheHeldAmount() {
        PendingPneumatics pending = new PendingPneumatics();
        pending.holdFromItem(-20);
        assertTrue(pending.isEmpty());
        pending.holdFromHandler(0);
        pending.holdFromHandler(-8);
        assertTrue(pending.isEmpty());
        pending.load(12_000);
        assertEquals(12_000, pending.air());
        pending.load(-1);
        assertTrue(pending.isEmpty());
    }

    @Test
    void releasingAfterAReloadDoesNotCreateExtraAir() {
        PendingPneumatics pending = new PendingPneumatics();
        pending.load(4000 + 5 * 8000);
        int released = pending.releaseOnForm();
        assertEquals(44_000, released);
        assertTrue(pending.isEmpty());
        assertEquals(44_000, pending.itemAir(released));
    }
}
