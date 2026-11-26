package com.tazer.taskorganizer.timer

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.util.Log
import androidx.annotation.RequiresApi
import com.tazer.taskorganizer.MainActivity
import java.util.UUID

internal enum class FocusDndAction { KEEP, INSTALL, WITHDRAW }

internal fun focusDndAction(
    recordedSegment: String?,
    activeSegment: String?,
    newlyStarted: Boolean,
    access: Boolean,
    withdrawalPending: Boolean,
): FocusDndAction = when {
    recordedSegment != null && (withdrawalPending || !access || recordedSegment != activeSegment) ->
        FocusDndAction.WITHDRAW
    recordedSegment == null && activeSegment != null && newlyStarted && access -> FocusDndAction.INSTALL
    else -> FocusDndAction.KEEP
}

internal class FocusDnd(private val context: Context, private val preferences: SharedPreferences) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val configuration = ComponentName(context, MainActivity::class.java)

    fun supported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun hasAccess(): Boolean = supported() && manager.isNotificationPolicyAccessGranted

    fun reconcile(activeSegment: String?, newlyStarted: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val recordedSegment = preferences.getString(SEGMENT, null)
            val access = hasAccess()
            val action = focusDndAction(
                recordedSegment, activeSegment, newlyStarted, access, preferences.getBoolean(WITHDRAWAL_PENDING, false),
            )
            when (action) {
                FocusDndAction.KEEP -> Unit
                FocusDndAction.INSTALL -> install(requireNotNull(activeSegment))
                FocusDndAction.WITHDRAW -> {
                    if (withdraw(access) && activeSegment != null && newlyStarted) install(activeSegment)
                }
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not reconcile the focus DND rule", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun install(segment: String) {
        val condition = Condition.newId(context).appendPath("focus").appendPath(UUID.randomUUID().toString()).build()
        // The condition also locates a rule created just before a process death.
        if (!preferences.edit()
                .putString(SEGMENT, segment)
                .putString(CONDITION, condition.toString())
                .putInt(PRIOR_FILTER, manager.currentInterruptionFilter)
                .commit()
        ) return
        try {
            val rule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                AutomaticZenRule.Builder(NAME, condition)
                    .setConfigurationActivity(configuration)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    .setManualInvocationAllowed(false)
                    .setTriggerDescription("Selama hitung mundur fokus berjalan")
                    .build()
            } else {
                AutomaticZenRule(NAME, null, configuration, condition, null,
                    NotificationManager.INTERRUPTION_FILTER_ALARMS, true)
            }
            val id = manager.addAutomaticZenRule(rule)
            manager.setAutomaticZenRuleState(id, Condition(condition, NAME, Condition.STATE_TRUE))
        } catch (e: RuntimeException) {
            withdraw(hasAccess())
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun withdraw(access: Boolean): Boolean {
        if (!preferences.getBoolean(WITHDRAWAL_PENDING, false) &&
            !preferences.edit().putBoolean(WITHDRAWAL_PENDING, true).commit()
        ) return false
        if (!access) return false
        val condition = preferences.getString(CONDITION, null)
        val ownedRules = manager.automaticZenRules.filterValues {
            it.conditionId.toString() == condition && it.configurationActivity == configuration
        }
        for (id in ownedRules.keys) {
            if (!manager.removeAutomaticZenRule(id) && manager.getAutomaticZenRule(id) != null) return false
        }
        // Android recomputes the live filter from the user's current manual mode and remaining rules.
        return preferences.edit().remove(SEGMENT).remove(CONDITION).remove(PRIOR_FILTER)
            .remove(WITHDRAWAL_PENDING).commit()
    }

    companion object {
        private const val TAG = "FocusDnd"
        private const val NAME = "Tazer Focus"
        private const val SEGMENT = "dnd_segment"
        private const val CONDITION = "dnd_condition"
        private const val PRIOR_FILTER = "dnd_prior_filter"
        private const val WITHDRAWAL_PENDING = "dnd_withdrawal_pending"
    }
}
