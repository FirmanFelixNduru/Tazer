package com.tazer.taskorganizer.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tazer.taskorganizer.MainActivity
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.timer.FocusRuntime
import com.tazer.taskorganizer.ui.TazerThemeMode
import com.tazer.taskorganizer.ui.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshFromBroadcast(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, ACTION_REFRESH -> refreshFromBroadcast(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        synchronized(publicationLock) { stopWatching(context) }
    }

    private fun refreshFromBroadcast(context: Context) {
        val result = goAsync()
        val app = context.applicationContext
        scope.launch {
            try {
                withTimeoutOrNull(8_000L) { reload(app, force = true) }
            } finally {
                result?.finish()
            }
        }
    }

    companion object {
        internal const val ACTION_OPEN_SCHEDULE = "com.tazer.taskorganizer.widget.OPEN_SCHEDULE"
        internal const val EXTRA_WIDGET_LINK = "com.tazer.taskorganizer.widget.FOCUS_LINK"
        private const val ACTION_REFRESH = "com.tazer.taskorganizer.widget.REFRESH"
        private const val WIDGET_CLICK = 1
        private const val TAG = "FocusWidget"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val reloadLock = Mutex()
        private val publicationLock = Any()
        private var ownerGeneration = 0L
        private var ownerChanging = false
        private var observer: Job? = null
        private var lastContent: Pair<WidgetContent, TazerThemeMode>? = null
        private var alarm: Pair<Int, Long>? = null

        internal fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch { reload(app, force = true) }
        }

        internal fun beginOwnerChange(context: Context) = synchronized(publicationLock) {
            ownerChanging = true
            ownerGeneration++
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(component(context))
                stopWatching(context)
                if (ids.isNotEmpty()) {
                    publish(context, manager, ids, WidgetContent(), ThemePreference(context).read())
                }
            } catch (failure: Exception) {
                Log.w(TAG, "Could not clear widget for account change", failure)
            }
        }

        internal fun endOwnerChange(context: Context) {
            synchronized(publicationLock) { ownerChanging = false }
            refresh(context)
        }

        internal fun isSelectedOwner(context: Context, uid: String): Boolean = synchronized(publicationLock) {
            !ownerChanging && FocusRuntime.get(context).widgetOwnerUid() == uid
        }

        private suspend fun reload(context: Context, force: Boolean = false) = reloadLock.withLock {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val focus = FocusRuntime.get(context)
                val (uid, generation) = synchronized(publicationLock) {
                    if (manager.getAppWidgetIds(component(context)).isEmpty()) {
                        stopWatching(context)
                        return@withLock
                    }
                    if (ownerChanging) return@withLock
                    focus.widgetOwnerUid() to ownerGeneration
                }
                val snapshot = if (uid == null) WidgetSnapshot() else {
                    val database = TazerDatabase.getInstance(context)
                    synchronized(publicationLock) {
                        if (generation != ownerGeneration) return@withLock
                        watch(context, database)
                    }
                    try {
                        loadWidgetSnapshot(database, uid, focus.now())
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (failure: Exception) {
                        Log.w(TAG, "Widget data unavailable", failure)
                        WidgetSnapshot()
                    }
                }
                synchronized(publicationLock) {
                    if (generation != ownerGeneration || ownerChanging || focus.widgetOwnerUid() != uid) return@withLock
                    val ids = manager.getAppWidgetIds(component(context))
                    if (ids.isEmpty()) {
                        stopWatching(context)
                        return@withLock
                    }
                    val theme = ThemePreference(context).read()
                    if (force || lastContent != (snapshot.content to theme)) {
                        publish(context, manager, ids, snapshot.content, theme)
                    }
                    updateAlarm(context, snapshot, force)
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                Log.w(TAG, "Could not refresh home widget", failure)
            }
        }

        private fun watch(context: Context, database: TazerDatabase) {
            if (observer?.isActive == true) return
            observer = scope.launch {
                try {
                    database.invalidationTracker.createFlow("UserProfile", "Task", "WorkSession").collect {
                        reload(context)
                    }
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    Log.w(TAG, "Could not observe widget data", failure)
                }
            }
        }

        private fun publish(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            content: WidgetContent,
            theme: TazerThemeMode,
        ) {
            val open = content.link?.let { FocusRuntime.get(context).widgetFocusIntent(it) }
                ?.putExtra(EXTRA_WIDGET_LINK, true)
                ?: Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_SCHEDULE)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val views = widgetViews(context, content, theme)
            views.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(
                context, WIDGET_CLICK, open, PendingIntent.FLAG_IMMUTABLE,
            ))
            manager.updateAppWidget(ids, views)
            lastContent = content to theme
        }

        private fun stopWatching(context: Context) {
            observer?.cancel()
            observer = null
            lastContent = null
            updateAlarm(context, WidgetSnapshot(), force = true)
        }

        private fun updateAlarm(context: Context, snapshot: WidgetSnapshot, force: Boolean) {
            val next = snapshot.refreshAtWallMs?.let { AlarmManager.RTC to it }
                ?: snapshot.refreshAtElapsedMs?.let { AlarmManager.ELAPSED_REALTIME to it }
            if (!force && alarm == next) return
            val manager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, FocusWidget::class.java).setAction(ACTION_REFRESH)
            if (next == null) {
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                    ?.let { manager.cancel(it) }
            } else {
                val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                manager.set(next.first, next.second, pending)
            }
            alarm = next
        }

        private fun component(context: Context) = ComponentName(context, FocusWidget::class.java)
    }
}
