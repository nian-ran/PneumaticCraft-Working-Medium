package dev.pncwm.pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TuningTest {
    @Test
    void pressureSignalSpansZeroToFifteen() {
        assertEquals(0, Tuning.pressureSignal(0f, 20f));
        assertEquals(1, Tuning.pressureSignal(0.1f, 20f));
        assertEquals(8, Tuning.pressureSignal(10f, 20f));
        assertEquals(15, Tuning.pressureSignal(20f, 20f));
        assertEquals(15, Tuning.pressureSignal(30f, 20f));
        assertEquals(0, Tuning.pressureSignal(5f, 0f));
    }

    @Test
    void progressSignalIsZeroWhenIdle() {
        assertEquals(0, Tuning.progressSignal(50, 100, false));
        assertEquals(0, Tuning.progressSignal(50, 0, true));
        assertEquals(1, Tuning.progressSignal(0, 100, true));
        assertEquals(8, Tuning.progressSignal(50, 100, true));
        assertEquals(15, Tuning.progressSignal(100, 100, true));
    }

    @Test
    void higherGradeSealsLastLonger() {
        assertEquals(1000, Tuning.sealLife(2));
        assertEquals(1500, Tuning.sealLife(3));
        assertEquals(2000, Tuning.sealLife(4));
        assertEquals(2500, Tuning.sealLife(5));
        assertEquals(1000, Tuning.sealLife(0));
        assertTrue(Tuning.sealLife(2) > Tuning.SEAL_SWAP_AT);
    }

    @Test
    void regulatorBleedsOnlyAboveSetpoint() {
        assertEquals(0, Tuning.regulatorBleed(16f, 16f, 5000));
        assertEquals(0, Tuning.regulatorBleed(16.4f, 16f, 5000));
        assertEquals(600, Tuning.regulatorBleed(20f, 16f, 5000));
        assertEquals(100, Tuning.regulatorBleed(17f, 16f, 500));
        assertEquals(0, Tuning.regulatorBleed(20f, 16f, 0));
    }

    @Test
    void strongerCurtainsKeepLessDamage() {
        assertTrue(Tuning.curtainKept(5) < Tuning.curtainKept(4));
        assertTrue(Tuning.curtainKept(4) < Tuning.curtainKept(3));
        assertTrue(Tuning.curtainKept(3) < Tuning.curtainKept(2));
    }

    @Test
    void certifiedCostCutsAirAndSealWear() {
        assertEquals(100, Tuning.certifiedCost(100, false));
        assertEquals(75, Tuning.certifiedCost(100, true));
        assertEquals(0, Tuning.certifiedCost(0, true));
        assertEquals(1, Tuning.certifiedCost(1, true));
        assertEquals(1, Tuning.sealWear(1.0, false));
        assertEquals(1, Tuning.sealWear(1.0, true));
        assertEquals(2, Tuning.sealWear(2.0, false));
        assertEquals(2, Tuning.sealWear(2.0, true)); // 2 * 0.75 = 1.5, rounded to 2
        assertEquals(3, Tuning.sealWear(4.0, true));
    }

    @Test
    void upgradePartsMultiplyThroughput() {
        assertEquals(1, Tuning.gradeMultiplier(0));
        assertEquals(1, Tuning.gradeMultiplier(1));
        assertEquals(2, Tuning.gradeMultiplier(2));
        assertEquals(5, Tuning.gradeMultiplier(5));
        assertEquals(5, Tuning.gradeMultiplier(9));
        assertEquals(1, Tuning.partMultiplier("g4_seal", "_pump", 4));
        assertEquals(4, Tuning.partMultiplier("g4_pump", "_pump", 4));
        assertEquals(3, Tuning.partMultiplier("g3_slide", "_slide", 3));
        assertEquals(1, Tuning.partMultiplier(null, "_pump", 5));
    }

    @Test
    void gasPathsCoverWasteGasesAndIgnoreLiquids() {
        assertTrue(Tuning.isGasPath("hydrogen"));
        assertTrue(Tuning.isGasPath("chlorine"));
        assertTrue(Tuning.isGasPath("acid_mist"));
        assertTrue(Tuning.isGasPath("dusty_steam"));
        assertTrue(Tuning.isGasPath("flue_gas"));
        assertTrue(Tuning.isGasPath("flowing_nitrogen"));
        assertTrue(Tuning.isGasPath("inerts"));
        assertTrue(Tuning.isGasPath("pure_nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isGasPath("lubricant"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isGasPath("brine"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isGasPath("hydrochloric"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isGasPath(""));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isGasPath(null));
    }

    @Test
    void breakingATankDropsOnlyFullBuckets() {
        assertEquals(0, Tuning.bucketsFromMilli(0));
        assertEquals(0, Tuning.bucketsFromMilli(999));
        assertEquals(1, Tuning.bucketsFromMilli(1000));
        assertEquals(2, Tuning.bucketsFromMilli(2500));
        assertEquals(16, Tuning.bucketsFromMilli(16_000));
    }

    @Test
    void repeatedMachinesCountOnce() {
        assertEquals(1, Tuning.distinct(List.of(7L, 7L, 7L)));
        assertEquals(3, Tuning.distinct(List.of(1L, 2L, 3L)));
        assertEquals(0, Tuning.distinct(List.of()));
    }
}
