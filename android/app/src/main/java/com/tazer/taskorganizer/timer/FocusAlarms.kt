package com.tazer.taskorganizer.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.net.toUri

internal data class FocusAlarm(val event: FocusEvent, val clock: Int, val triggerAt: Long)

internal class FocusAlarms(private val context: Context, private val preferences: SharedPreferences) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private val scheduled = mutableMapOf<FocusEvent, FocusAlarm>()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    fun consume(event: FocusEvent): Boolean {
        if (key(event) !in tracked()) return false
        scheduled.remove(event)
        return true
    }

    fun reconcile(wanted: List<FocusAlarm>, force: Boolean = false) {
        val newKeys = wanted.mapTo(mutableSetOf()) { key(it.event) }
        val oldKeys = tracked()
        // Record identities before registering alarms so a restarted process can cancel them.
        if (!oldKeys.containsAll(newKeys)) save(oldKeys + newKeys)
        for (staleKey in oldKeys - newKeys) {
            val event = FocusEvent.fromUri(staleKey.toUri(), "event") ?: continue
            scheduled.remove(event)
            pending(event, PendingIntent.FLAG_NO_CREATE)?.let {
                manager.cancel(it)
                it.cancel()
            }
        }
        if (oldKeys != newKeys) save(newKeys)

        // Revocation stops the process and produces no broadcast. A new process
        // reinstalls alarms using the currently allowed scheduling method.
        for (alarm in wanted) {
            if (!force && scheduled[alarm.event] == alarm) continue
            val operation = requireNotNull(pending(alarm.event, 0))
            if (canScheduleExact()) {
                try {
                    manager.setExactAndAllowWhileIdle(alarm.clock, alarm.triggerAt, operation)
                    scheduled[alarm.event] = alarm
                    continue
                } catch (denied: SecurityException) {
                    Log.i(TAG, "Exact alarm access changed during scheduling", denied)
                }
            }
            manager.setAndAllowWhileIdle(alarm.clock, alarm.triggerAt, operation)
            scheduled[alarm.event] = alarm
        }
    }

    private fun pending(event: FocusEvent, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(context, 0, event.receiverIntent(context), flags or PendingIntent.FLAG_IMMUTABLE)

    private fun key(event: FocusEvent): String = event.uri("event").toString()

    private fun tracked(): Set<String> = preferences.getStringSet(TRACKED, emptySet()).orEmpty()

    private fun save(keys: Set<String>) {
        val editor = preferences.edit()
        editor.putStringSet(TRACKED, keys)
        if (!editor.commit()) {
            Log.w(TAG, "Could not persist focus alarm identities")
        }
    }

    private companion object {
        const val TAG = "FocusAlarms"
        const val TRACKED = "alarm_keys"
    }
}
