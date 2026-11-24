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
    void vacuumTooltipsUseDedicatedKeysAndUnsignedCounts() {
        assertEquals("tooltip.workingmedium.stored_controller_vacuum", StoredPneumatics.controllerAirTooltipKey(-20_000));
        assertEquals("tooltip.workingmedium.stored_controller_air", StoredPneumatics.controllerAirTooltipKey(20_000));
        org.junit.jupiter.api.Assertions.assertNull(StoredPneumatics.controllerAirTooltipKey(0));
        assertEquals("tooltip.workingmedium.stored_vacuum", StoredPneumatics.storedAirTooltipKey(-500, true));
        assertEquals("tooltip.workingmedium.stored_vacuum_amount", StoredPneumatics.storedAirTooltipKey(-500, false));
        assertEquals("tooltip.workingmedium.stored_air", StoredPneumatics.storedAirTooltipKey(500, true));
        assertEquals("tooltip.workingmedium.pending_vacuum", StoredPneumatics.pendingAirTooltipKey(-8_000));
        assertEquals("tooltip.workingmedium.pending_air", StoredPneumatics.pendingAirTooltipKey(8_000));
        assertEquals("tooltip.workingmedium.stored_transfer_vacuum", StoredPneumatics.transferTooltipKey(-100, 0));
        assertEquals("tooltip.workingmedium.stored_transfer_vacuum", StoredPneumatics.transferTooltipKey(-100, -40));
        assertEquals("tooltip.workingmedium.stored_transfer_signed", StoredPneumatics.transferTooltipKey(-100, 40));
        assertEquals("tooltip.workingmedium.stored_transfer", StoredPneumatics.transferTooltipKey(100, 40));
        assertEquals(20_000, StoredPneumatics.displayAmount(-20_000));
        assertEquals(20_000, StoredPneumatics.displayAmount(20_000));
        assertEquals("5.00", StoredPneumatics.displayBar(-2000, 400));
        assertEquals("-5.00", StoredPneumatics.signedBar(-2000, 400));
    }

    @Test
    void transferKeepsBothSides() {
        StoredPneumatics stored = StoredPneumatics.transfer(1200, 400, 340.0);
        assertEquals(1200, stored.air());
        assertEquals(400, stored.secondAir());
        assertEquals(340.0, stored.temperature());
    }

    @Test
    void rebuiltHandlerKeepsVacuumAndUnformedGuiShowsHeldAir() {
        assertTrue(StoredPneumatics.hasCarriedAir(-500));
        assertTrue(StoredPneumatics.hasCarriedAir(500));
        assertFalse(StoredPneumatics.hasCarriedAir(0));
        assertEquals("tooltip.workingmedium.pending_vacuum_short",
                StoredPneumatics.pendingGuiTooltipKey(false, -20_000));
        assertEquals("tooltip.workingmedium.pending_air_short",
                StoredPneumatics.pendingGuiTooltipKey(false, 8_000));
        org.junit.jupiter.api.Assertions.assertNull(StoredPneumatics.pendingGuiTooltipKey(true, -20_000));
        org.junit.jupiter.api.Assertions.assertNull(StoredPneumatics.pendingGuiTooltipKey(false, 0));
    }
}
