package com.tazer.taskorganizer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.CloudSync
import com.tazer.taskorganizer.data.SubtaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.integration.AndroidIntegration
import com.tazer.taskorganizer.integration.networkAvailability
import com.tazer.taskorganizer.timer.DailyBriefingAlarms
import com.tazer.taskorganizer.timer.FocusRuntime
import com.tazer.taskorganizer.ui.TazerApp
import com.tazer.taskorganizer.ui.TazerViewModel
import com.tazer.taskorganizer.widget.FocusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var model: TazerViewModel
    private val focus by lazy { FocusRuntime.get(applicationContext) }
    private var permissionRefresh by mutableIntStateOf(0)
    private var scheduleRequest by mutableIntStateOf(0)
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefresh++
        if (::model.isInitialized) model.refreshFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleRequest = savedInstanceState?.getInt(SCHEDULE_REQUEST) ?: 0

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = TazerDatabase.getInstance(applicationContext)
                val integration = AndroidIntegration(applicationContext)
                return TazerViewModel(focus.taskRepository, CalendarRepository(database),
                    integration, focus, BuildConfig.DEBUG, SubtaskRepository(database),
                    CloudSync(database, integration::syncRpc), networkAvailability(applicationContext)) as T
            }
        }
        model = ViewModelProvider(this, factory)[TazerViewModel::class.java]
        routeIntent(intent)
        // Restores briefing alarms after an in-place upgrade or a force stop.
        lifecycleScope.launch(Dispatchers.IO) { DailyBriefingAlarms.reschedule(applicationContext) }
        setContent {
            TazerApp(model, ::startActivity, ::requestNotifications, permissionRefresh,
                scheduleRequest = scheduleRequest, onScheduleOpened = { scheduleRequest = 0 })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SCHEDULE_REQUEST, scheduleRequest)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh++
        model.refreshFocus()
    }

    private fun routeIntent(intent: Intent) {
        val link = focus.readLink(intent)
        val uri = intent.data
        when {
            link != null -> {
                val fromWidget = intent.getBooleanExtra(FocusWidget.EXTRA_WIDGET_LINK, false)
                intent.replaceExtras(Bundle())
                intent.data = null
                if (fromWidget) openSchedule()
                if (!fromWidget || FocusWidget.isSelectedOwner(applicationContext, link.uid)) {
                    model.handleFocusLink(link)
                } else {
                    model.restoreSession()
                }
            }
            intent.action == FocusWidget.ACTION_OPEN_SCHEDULE -> {
                intent.action = null
                openSchedule()
                model.restoreSession()
            }
            intent.action == DailyBriefingAlarms.ACTION_OPEN_STATISTICS -> {
                intent.action = null
                model.closeFocus()
                scheduleRequest = STATISTICS_REQUEST
                model.restoreSession()
            }
            uri?.scheme == "tazer" -> {
                intent.data = null
                model.handleAuthCallback(uri)
            }
            else -> model.restoreSession()
        }
    }

    private fun openSchedule() {
        model.closeFocus()
        scheduleRequest = 1
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    internal companion object {
        private const val SCHEDULE_REQUEST = "widget_schedule_request"
        const val STATISTICS_REQUEST = 2
    }
}
