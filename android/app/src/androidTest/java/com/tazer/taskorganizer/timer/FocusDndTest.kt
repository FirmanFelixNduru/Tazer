package com.tazer.taskorganizer.timer

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.Condition
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusDndTest {
    @Test
    fun persistsPriorFilterAndWithdrawsOnlyItsRuleAfterRecovery() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        assumeTrue("Enable Tazer notification policy access on the test device", manager.isNotificationPolicyAccessGranted)
        val name = "test_focus_dnd_${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val before = manager.automaticZenRules.keys
        val priorFilter = manager.currentInterruptionFilter
        val dnd = FocusDnd(context, preferences)

        try {
            dnd.reconcile("counting", newlyStarted = true)
            val installed = (manager.automaticZenRules.keys - before).single()
            assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, manager.getAutomaticZenRule(installed)?.interruptionFilter)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                assertEquals(Condition.STATE_TRUE, manager.getAutomaticZenRuleState(installed))
            }
            assertEquals(priorFilter, preferences.getInt("dnd_prior_filter", -1))

            val recovered = FocusDnd(context, preferences)
            recovered.reconcile("counting")
            assertEquals(before + installed, manager.automaticZenRules.keys)
            recovered.reconcile(null)
            assertEquals(before, manager.automaticZenRules.keys)
            assertTrue(preferences.all.isEmpty())
        } finally {
            dnd.reconcile(null)
            context.deleteSharedPreferences(name)
        }
    }
}
