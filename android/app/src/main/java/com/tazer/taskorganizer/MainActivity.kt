package com.tazer.taskorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tazer.taskorganizer.data.TaskRepository
import com.tazer.taskorganizer.data.TazerDatabase
import com.tazer.taskorganizer.ui.TazerApp
import com.tazer.taskorganizer.ui.TazerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = TazerDatabase.getInstance(applicationContext)
                return TazerViewModel(TaskRepository(database), BuildConfig.DEBUG) as T
            }
        }
        val model = ViewModelProvider(this, factory)[TazerViewModel::class.java]
        setContent { TazerApp(model) }
    }
}
