package com.tazer.taskorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.integration.AndroidIntegration
import com.tazer.taskorganizer.ui.TazerApp
import com.tazer.taskorganizer.ui.TazerViewModel

class MainActivity : ComponentActivity() {
    private lateinit var model: TazerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = TazerDatabase.getInstance(applicationContext)
                return TazerViewModel(TaskRepository(database), AndroidIntegration(applicationContext), BuildConfig.DEBUG) as T
            }
        }
        model = ViewModelProvider(this, factory)[TazerViewModel::class.java]
        if (intent.data == null) model.restoreSession() else handleAuthIntent(intent)
        setContent { TazerApp(model, ::startActivity) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        val uri = intent.data ?: return
        intent.data = null
        model.handleAuthCallback(uri)
    }
}
