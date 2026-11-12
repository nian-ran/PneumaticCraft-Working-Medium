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


    @Test
    void airChangeProducesWhenRecipeAirIsNegative() {
        assertEquals(200, Tuning.airChange(-200, 1, 1.0, false));
        assertEquals(267, Tuning.airChange(-200, 1, 1.0, true));
        assertEquals(480, Tuning.airChange(-240, 2, 1.0, false));
        assertEquals(-50, Tuning.airChange(50, 1, 1.0, false));
        assertEquals(-38, Tuning.airChange(50, 1, 1.0, true));
        assertEquals(0, Tuning.airChange(0, 1, 1.0, false));
        assertEquals(0, Tuning.airChange(0, 1, 1.0, true));
    }

    @Test
    void heatMovesFromHotToColdAndRespectsTheCap() {
        assertEquals(20.0, Tuning.heatMove(400, 300, 0.2, 80), 1e-6);
        assertEquals(10.0, Tuning.heatMove(400, 300, 0.2, 10), 1e-6);
        assertEquals(0.0, Tuning.heatMove(300, 300, 0.2, 80), 1e-6);
        assertEquals(0.0, Tuning.heatMove(300.1, 300.0, 0.2, 80), 1e-6);
        assertEquals(-20.0, Tuning.heatMove(300, 400, 0.2, 80), 1e-6);
        assertEquals(0.0, Tuning.heatMove(400, 300, 0, 80), 1e-6);
    }

    @Test
    void longerGalleriesTransferHeatMoreSlowly() {
        assertTrue(Tuning.galleryFraction(1) > Tuning.galleryFraction(6));
        assertTrue(Tuning.galleryFraction(6) > Tuning.galleryFraction(12));
        assertTrue(Tuning.galleryFraction(1) > 0);
        assertEquals(0.22, Tuning.galleryFraction(1), 1e-6);
    }

    @Test
    void flareHeatScalesWithGasBurned() {
        assertEquals(0.0, Tuning.flareHeat(0), 1e-6);
        assertEquals(2.4, Tuning.flareHeat(20), 1e-6);
        assertEquals(0.0, Tuning.flareHeat(-5), 1e-6);
    }

    @Test
    void hazardousFluidsHurtAndSomePoison() {
        assertEquals(2f, Tuning.fluidHarm("chlorine"));
        assertEquals(2f, Tuning.fluidHarm("flowing_acid_mist"));
        assertEquals(3f, Tuning.fluidHarm("hydrochloric"));
        assertEquals(3f, Tuning.fluidHarm("caustic"));
        assertEquals(2f, Tuning.fluidHarm("acid_feed"));
        assertEquals(1f, Tuning.fluidHarm("flue_gas"));
        assertEquals(0f, Tuning.fluidHarm("brine"));
        assertEquals(0f, Tuning.fluidHarm("lubricant"));
        assertEquals(0f, Tuning.fluidHarm(null));
        assertEquals(80, Tuning.fluidPoisonTicks("chlorine"));
        assertEquals(80, Tuning.fluidPoisonTicks("acid_mist"));
        assertEquals(0, Tuning.fluidPoisonTicks("caustic"));
        assertEquals(0, Tuning.fluidPoisonTicks("water"));
    }

    @Test
    void dedicatedSlotsCoverNewUpgradeParts() {
        assertEquals(8, Tuning.dedicatedSlotForPath("g3_seal"));
        assertEquals(9, Tuning.dedicatedSlotForPath("catalyst"));
        assertEquals(10, Tuning.dedicatedSlotForPath("electrode"));
        assertEquals(10, Tuning.dedicatedSlotForPath("program_sheet"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g2_pump"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g4_slide"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g2_nozzle"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g5_cylinder"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g3_diaphragm"));
        assertEquals(10, Tuning.dedicatedSlotForPath("g4_gripper"));
        assertEquals(-1, Tuning.dedicatedSlotForPath("g2_plate"));
        assertEquals(-1, Tuning.dedicatedSlotForPath("microfluidic_chip"));
        assertEquals(-1, Tuning.dedicatedSlotForPath(null));
    }

    @Test
    void diaphragmRaisesRegulatorEfficiency() {
        assertEquals(0.80f, Tuning.regulatorEfficiency(1), 1e-5);
        assertEquals(0.84f, Tuning.regulatorEfficiency(2), 1e-5);
        assertEquals(0.96f, Tuning.regulatorEfficiency(5), 1e-5);
        assertEquals(0.96f, Tuning.regulatorEfficiency(9), 1e-5);
        assertEquals(0.80f, Tuning.regulatorEfficiency(0), 1e-5);
        assertEquals(64, Tuning.electrodeLife());
    }

    @Test
    void flareBurnsWasteGasesButNotProducts() {
        assertTrue(Tuning.isCombustibleGas("hydrogen"));
        assertTrue(Tuning.isCombustibleGas("flue_gas"));
        assertTrue(Tuning.isCombustibleGas("acid_mist"));
        assertTrue(Tuning.isCombustibleGas("chlorine"));
        assertTrue(Tuning.isCombustibleGas("dusty_steam"));
        assertTrue(Tuning.isCombustibleGas("flowing_flue_gas"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("argon"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("oxygen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("pure_nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("inerts"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCombustibleGas("brine"));
    }

    @Test
    void purgeAndCryogenicFluidsAreDistinctFromAcids() {
        assertTrue(Tuning.isPurgeGas("nitrogen"));
        assertTrue(Tuning.isPurgeGas("pure_nitrogen"));
        assertTrue(Tuning.isPurgeGas("argon"));
        assertTrue(Tuning.isPurgeGas("inerts"));
        assertTrue(Tuning.isPurgeGas("workingmedium:nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isPurgeGas("hydrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isPurgeGas("oxygen"));
        assertTrue(Tuning.isCryogenic("liquid_co2"));
        assertTrue(Tuning.isCryogenic("nitrogen"));
        assertTrue(Tuning.isCryogenic("argon"));
        assertTrue(Tuning.isCryogenic("oxygen"));
        assertTrue(Tuning.isCryogenic("pure_nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.isCryogenic("chlorine"));
        assertEquals(1f, Tuning.fluidFreeze("liquid_co2"));
        assertEquals(0f, Tuning.fluidFreeze("chlorine"));
        assertEquals(80, Tuning.fluidSlowTicks("nitrogen"));
        assertEquals(0, Tuning.fluidSlowTicks("caustic"));
        assertTrue(Tuning.needsDrain("acid_mist"));
        assertTrue(Tuning.needsDrain("workingmedium:flue_gas"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.needsDrain("nitrogen"));
        org.junit.jupiter.api.Assertions.assertFalse(Tuning.needsDrain("liquid_co2"));
    }

    @Test
    void fillSignalAndCertifiedHelpers() {
        assertEquals(0, Tuning.fillSignal(0, 64));
        assertEquals(0, Tuning.fillSignal(10, 0));
        assertEquals(1, Tuning.fillSignal(1, 64));
        assertEquals(8, Tuning.fillSignal(32, 64));
        assertEquals(15, Tuning.fillSignal(64, 64));
        assertEquals(15, Tuning.fillSignal(80, 64));
        assertEquals(0.80f, Tuning.certifiedEfficiency(0.80f, false), 1e-5);
        assertEquals(0.85f, Tuning.certifiedEfficiency(0.80f, true), 1e-5);
        assertEquals(0.97f, Tuning.certifiedEfficiency(0.96f, true), 1e-5);
        assertEquals(50, Tuning.certifiedBoost(50, false));
        assertEquals(67, Tuning.certifiedBoost(50, true));
        assertEquals(0, Tuning.certifiedBoost(0, true));
        assertEquals(1, Tuning.sealWear(2.0, false, true));
        assertEquals(2, Tuning.sealWear(2.0, false, false));
        assertEquals(20, Tuning.PURGE_MB);
    }

}
