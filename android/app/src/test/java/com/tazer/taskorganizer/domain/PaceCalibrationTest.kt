package com.tazer.taskorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaceCalibrationTest {
    @Test
    fun prefersNormalizedCourseAndResistsAnOutlier() {
        val history = listOf(
            sample("Basis Data", 10, 12),
            sample("BASIS  DATA", 20, 30),
            sample("basis\tdata", 15, 300),
            sample("Pemrograman", 10, 9),
            sample("Sistem Operasi", 20, 20),
            sample("Logika", 10, 10),
        )

        val factor = PaceCalibration.factor("  basis Data  ", history)
        assertEquals(1.5, requireNotNull(factor), 0.0001)
        assertEquals(90, PaceCalibration.adjustedMinutes(60, factor))
    }

    @Test
    fun fallsBackToGlobalMedianWithFewerThanThreeValidCourseSamples() {
        val history = listOf(
            sample("Basis Data", 10, 10),
            sample("Basis Data", 10, 16),
            sample("Basis Data", 10, 0),
            sample("Logika", 10, 8),
            sample("Logika", 10, 14),
        )

        assertEquals(1.2, requireNotNull(PaceCalibration.factor("Basis Data", history)), 0.0001)
        assertEquals(1.2, requireNotNull(PaceCalibration.factor("", history)), 0.0001)
    }

    @Test
    fun ignoresInvalidSamplesAndKeepsRawEstimateUntilThereIsEnoughHistory() {
        val history = listOf(
            sample("Basis Data", 10, 15),
            sample("Basis Data", 10, 18),
            sample("Basis Data", 0, 20),
            sample("Basis Data", 10_081, 20),
            sample("Basis Data", 10, -1),
        )

        val factor = PaceCalibration.factor("Basis Data", history)
        assertNull(factor)
        assertEquals(25, PaceCalibration.adjustedMinutes(25, factor))
    }

    @Test
    fun clampsTheFactorAndKeepsAdjustedMinutesInRange() {
        val fast = List(3) { sample("Basis Data", 20, 5) }
        val slow = List(3) { sample("Basis Data", 20, 50) }
        val fastFactor = PaceCalibration.factor("Basis Data", fast)
        val slowFactor = PaceCalibration.factor("Basis Data", slow)

        assertEquals(0.75, requireNotNull(fastFactor), 0.0001)
        assertEquals(1.8, requireNotNull(slowFactor), 0.0001)
        assertEquals(1, PaceCalibration.adjustedMinutes(1, fastFactor))
        assertEquals(10_080, PaceCalibration.adjustedMinutes(8_000, slowFactor))
        assertEquals(45, PaceCalibration.adjustedMinutes(25, slowFactor))
    }

    private fun sample(course: String, baseline: Int, actualMinutes: Int) =
        PaceSample(course, baseline, actualMinutes * 60_000L)
}
