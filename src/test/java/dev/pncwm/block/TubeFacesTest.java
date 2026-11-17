package dev.pncwm.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TubeFacesTest {
    @Test
    void reliefVentFaceDoesNotConnect() {
        assertFalse(TubeFaceRules.isReliefAirFace("north", "north"));
        assertTrue(TubeFaceRules.isReliefAirFace("north", "south"));
        assertTrue(TubeFaceRules.isReliefAirFace("north", "up"));
        assertTrue(TubeFaceRules.isReliefAirFace("west", "east"));
        assertFalse(TubeFaceRules.isReliefAirFace("west", "west"));
    }

    @Test
    void transferOnlyConnectsOnInletAndOutlet() {
        assertTrue(TubeFaceRules.isTransferPort("north", "south", "north"));
        assertTrue(TubeFaceRules.isTransferPort("north", "south", "south"));
        assertFalse(TubeFaceRules.isTransferPort("north", "south", "east"));
        assertFalse(TubeFaceRules.isTransferPort("north", "south", "west"));
        assertFalse(TubeFaceRules.isTransferPort("north", "south", "up"));
        assertFalse(TubeFaceRules.isTransferPort("east", "west", "north"));
        assertTrue(TubeFaceRules.isTransferPort("east", "west", "west"));
    }

    @Test
    void isolatedBackOfColumnAndRegulatorDoesNotConnect() {
        assertFalse(TubeFaceRules.isApparatusAirFace(true, "south", "south"));
        assertTrue(TubeFaceRules.isApparatusAirFace(true, "south", "north"));
        assertTrue(TubeFaceRules.isApparatusAirFace(true, "south", "up"));
        assertFalse(TubeFaceRules.isApparatusAirFace(true, "east", "east"));
        assertFalse(TubeFaceRules.isApparatusAirFace(true, "north", "north"));
        assertTrue(TubeFaceRules.isApparatusAirFace(false, "south", "south"));
        assertTrue(TubeFaceRules.isApparatusAirFace(false, "west", "west"));
    }
}
