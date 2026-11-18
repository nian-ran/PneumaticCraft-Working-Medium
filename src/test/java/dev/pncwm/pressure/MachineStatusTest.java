package dev.pncwm.pressure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MachineStatusTest {
    @Test
    void operatingWindowIncludesRunningAndCertified() {
        assertTrue(MachineStatus.WINDOW.inOperatingWindow());
        assertTrue(MachineStatus.RUNNING.inOperatingWindow());
        assertTrue(MachineStatus.CERTIFIED.inOperatingWindow());
        assertFalse(MachineStatus.IDLE.inOperatingWindow());
        assertFalse(MachineStatus.PRESSURE.inOperatingWindow());
        assertFalse(MachineStatus.TEMPERATURE.inOperatingWindow());
        assertFalse(MachineStatus.PAUSED.inOperatingWindow());
        assertFalse(MachineStatus.UNFORMED.inOperatingWindow());
        assertFalse(MachineStatus.CLOGGED.inOperatingWindow());
        assertFalse(MachineStatus.NO_AIR.inOperatingWindow());
    }
}
