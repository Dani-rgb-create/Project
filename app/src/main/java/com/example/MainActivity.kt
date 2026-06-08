package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.WebDatabase
import com.example.ui.WebCopilotApp
import com.example.ui.WebCopilotViewModel
import com.example.ui.WebCopilotViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure WebDatabase Room Instance
        val database = WebDatabase.getDatabase(this)
        val webDao = database.webDao()

        // Initialize ViewModel using Factory
        val viewModel = ViewModelProvider(
            this,
            WebCopilotViewModelFactory(application, webDao)
        )[WebCopilotViewModel::class.java]

        setContent {
            MyApplicationTheme {
                WebCopilotApp(viewModel = viewModel)
            }
        }
    }
}
