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
import com.tazer.taskorganizer.data.CalendarRepository
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.integration.AndroidIntegration
import com.tazer.taskorganizer.timer.FocusRuntime
import com.tazer.taskorganizer.ui.TazerApp
import com.tazer.taskorganizer.ui.TazerViewModel

class MainActivity : ComponentActivity() {
    private lateinit var model: TazerViewModel
    private val focus by lazy { FocusRuntime.get(applicationContext) }
    private var permissionRefresh by mutableIntStateOf(0)
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefresh++
        if (::model.isInitialized) model.refreshFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = TazerDatabase.getInstance(applicationContext)
                return TazerViewModel(TaskRepository(database), CalendarRepository(database),
                    AndroidIntegration(applicationContext), focus, BuildConfig.DEBUG) as T
            }
        }
        model = ViewModelProvider(this, factory)[TazerViewModel::class.java]
        routeIntent(intent)
        setContent { TazerApp(model, ::startActivity, ::requestNotifications, permissionRefresh) }
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
                intent.replaceExtras(Bundle())
                intent.data = null
                model.handleFocusLink(link)
            }
            uri?.scheme == "tazer" -> {
                intent.data = null
                model.handleAuthCallback(uri)
            }
            else -> model.restoreSession()
        }
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
}
