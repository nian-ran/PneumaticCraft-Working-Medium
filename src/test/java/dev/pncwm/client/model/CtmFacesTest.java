package dev.pncwm.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CtmFacesTest {
    @Test
    void packUsesStableBitsAndSheetUvs() {
        assertEquals(0, CtmIndex.pack(false, false, false, false));
        assertEquals(1, CtmIndex.pack(true, false, false, false));
        assertEquals(15, CtmIndex.pack(true, true, true, true));
        assertEquals(0, CtmIndex.u0(0));
        assertEquals(0, CtmIndex.v0(0));
        assertEquals(12, CtmIndex.u0(15));
        assertEquals(12, CtmIndex.v0(15));
        assertEquals(8, CtmIndex.u0(CtmIndex.RIGHT));
        assertEquals(4, CtmIndex.v0(CtmIndex.DOWN));
    }
}
