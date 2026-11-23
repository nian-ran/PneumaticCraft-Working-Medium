package dev.pncwm.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StoredPneumaticsTest {
    @Test
    void emptyWhenNoAirAndAmbientHeat() {
        assertTrue(StoredPneumatics.of(0, StoredPneumatics.AMBIENT_KELVIN).isEmpty());
        assertTrue(StoredPneumatics.of(0, 300.4).isEmpty());
        assertTrue(StoredPneumatics.transfer(0, 0, 299.6).isEmpty());
    }

    @Test
    void notEmptyWhenAirOrHeatDiffers() {
        assertFalse(StoredPneumatics.of(1, StoredPneumatics.AMBIENT_KELVIN).isEmpty());
        assertFalse(StoredPneumatics.transfer(0, 8, StoredPneumatics.AMBIENT_KELVIN).isEmpty());
        assertFalse(StoredPneumatics.of(0, 310.0).isEmpty());
        assertFalse(StoredPneumatics.of(0, 250.0).isEmpty(300.0));
        assertTrue(StoredPneumatics.of(0, 290.0).isEmpty(290.0));
        // Vacuum is still a stored amount, even at ambient temperature.
        assertFalse(StoredPneumatics.of(-500, StoredPneumatics.AMBIENT_KELVIN).isEmpty());
        assertFalse(StoredPneumatics.transfer(0, -8, StoredPneumatics.AMBIENT_KELVIN).isEmpty());
        assertFalse(StoredPneumatics.of(-500, 250.0).isEmpty(300.0));
    }

    @Test
    void transferKeepsBothSides() {
        StoredPneumatics stored = StoredPneumatics.transfer(1200, 400, 340.0);
        assertEquals(1200, stored.air());
        assertEquals(400, stored.secondAir());
        assertEquals(340.0, stored.temperature());
    }
}
