package com.tazer.taskorganizer.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runtime by lazy { FocusRuntime.get(this) }
    private var checkpoints: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = runtime.foregroundPlaceholder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(FocusNotifications.ACTIVE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(FocusNotifications.ACTIVE_ID, notification)
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not promote focus service", e)
            stopSelf(startId)
            return START_NOT_STICKY
        } catch (e: SecurityException) {
            Log.w(TAG, "Focus service promotion denied", e)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        runtime.attach(this)
        checkpoints?.cancel()
        checkpoints = scope.launch {
            var refresh = true
            while (true) {
                val untilNext = checkpoint(refresh) ?: break
                refresh = false
                delay(untilNext)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scope.launch { checkpoint(false) }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runtime.detach(this)
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun checkpoint(refresh: Boolean): Long? =
        try {
            runtime.serviceTick(refresh)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: RuntimeException) {
            Log.w(TAG, "Focus checkpoint failed", e)
            30_000L
        }

    private companion object {
        const val TAG = "FocusService"
    }
}
