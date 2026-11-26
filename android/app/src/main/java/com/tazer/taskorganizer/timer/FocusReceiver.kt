package com.tazer.taskorganizer.timer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val event = FocusEvent.fromReceiver(intent)
        if (event == null && action !in SYSTEM_ACTIONS) return
        val pending = goAsync()
        val application = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    val runtime = FocusRuntime.get(application)
                    if (event == null) {
                        // Boot, clock, and zone changes also restore the Daily Briefing alarms.
                        DailyBriefingAlarms.reschedule(application)
                        runtime.onSystemChange(action)
                    } else {
                        runtime.onEvent(event)
                    }
                }
            } catch (cancelled: CancellationException) {
                Log.w(TAG, "Focus broadcast interrupted", cancelled)
                throw cancelled
            } catch (e: RuntimeException) {
                Log.w(TAG, "Could not process focus broadcast", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "FocusReceiver"
        val SYSTEM_ACTIONS = buildSet {
            add(Intent.ACTION_BOOT_COMPLETED)
            add(Intent.ACTION_TIME_CHANGED)
            add(Intent.ACTION_TIMEZONE_CHANGED)
            add(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
            }
        }
    }
}
