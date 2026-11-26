package com.tazer.taskorganizer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusDndStateTest {
    @Test
    fun grantsAndRecoveryCannotStartDndWithoutAFreshCountingSegment() {
        assertEquals(FocusDndAction.KEEP, focusDndAction(null, "counting", true, false, false))
        assertEquals(FocusDndAction.KEEP, focusDndAction(null, "counting", false, true, false))
        assertEquals(FocusDndAction.KEEP, focusDndAction(null, null, true, true, false))
        assertEquals(FocusDndAction.INSTALL, focusDndAction(null, "counting", true, true, false))
    }

    @Test
    fun stayingInTheSameSegmentDoesNotReassertTheRule() {
        assertEquals(FocusDndAction.KEEP, focusDndAction("counting", "counting", false, true, false))
    }

    @Test
    fun pauseStopCutoffOrNewSegmentWithdrawsTheRecordedLease() {
        assertEquals(FocusDndAction.WITHDRAW, focusDndAction("counting", null, false, true, false))
        assertEquals(FocusDndAction.WITHDRAW, focusDndAction("counting", "resumed", true, true, false))
    }

    @Test
    fun permissionRevocationKeepsTheWithdrawalPendingAfterGrant() {
        assertEquals(FocusDndAction.WITHDRAW, focusDndAction("counting", "counting", false, false, false))
        assertEquals(FocusDndAction.WITHDRAW, focusDndAction("counting", "counting", false, true, true))
    }
}
