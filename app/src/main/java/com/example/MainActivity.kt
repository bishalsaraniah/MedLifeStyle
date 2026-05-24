package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.MedicalRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.screens.MedicalMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MedicalViewModel
import com.example.ui.viewmodel.MedicalViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database holding Patient medical data offline
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = MedicalRepository(database.medicalDao())

    // Instantiate Viewmodel with the thread safe factory
    val viewModelFactory = MedicalViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[MedicalViewModel::class.java]

    setContent {
      val forceDark by viewModel.forceDarkTheme.collectAsState()
      val systemDark = isSystemInDarkTheme()
      val darkTheme = forceDark ?: systemDark

      MyApplicationTheme(darkTheme = darkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MedicalMainScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

