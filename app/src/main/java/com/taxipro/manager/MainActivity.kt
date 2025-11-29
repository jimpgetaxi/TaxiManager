package com.taxipro.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.taxipro.manager.data.local.TaxiDatabase
import com.taxipro.manager.data.repository.TaxiRepository
import com.taxipro.manager.ui.screens.DashboardScreen
import com.taxipro.manager.ui.theme.TaxiManagerTheme
import com.taxipro.manager.ui.viewmodel.MainViewModel
import com.taxipro.manager.ui.viewmodel.MainViewModelFactory

import com.taxipro.manager.data.repository.UserPreferencesRepository
import com.taxipro.manager.data.repository.dataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = TaxiDatabase.getDatabase(this)
        val repository = TaxiRepository(database.taxiDao())
        val userPreferencesRepository = UserPreferencesRepository(this)
        val viewModelFactory = MainViewModelFactory(repository, userPreferencesRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            TaxiManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}