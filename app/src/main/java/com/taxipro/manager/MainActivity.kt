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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.taxipro.manager.data.local.TaxiDatabase
import com.taxipro.manager.data.repository.TaxiRepository
import com.taxipro.manager.data.repository.UserPreferencesRepository
import com.taxipro.manager.ui.screens.DashboardScreen
import com.taxipro.manager.ui.screens.ExpensesScreen
import com.taxipro.manager.ui.screens.HistoryScreen
import com.taxipro.manager.ui.screens.RecurringExpensesScreen
import com.taxipro.manager.ui.screens.ReportsScreen
import com.taxipro.manager.ui.screens.SettingsScreen
import com.taxipro.manager.ui.screens.ShiftDetailsScreen
import com.taxipro.manager.ui.theme.TaxiManagerTheme
import com.taxipro.manager.ui.viewmodel.MainViewModel
import com.taxipro.manager.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = TaxiDatabase.getDatabase(this)
        val repository = TaxiRepository(database, database.taxiDao(), database.expenseDao(), database.recurringExpenseDao())
        val userPreferencesRepository = UserPreferencesRepository(this)
        val viewModelFactory = MainViewModelFactory(repository, userPreferencesRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            TaxiManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("dashboard") }
                    var selectedShiftId by remember { mutableStateOf<Long?>(null) }

                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onHistoryClick = { currentScreen = "history" },
                            onSettingsClick = { currentScreen = "settings" },
                            onExpensesClick = { currentScreen = "expenses" },
                            onReportsClick = { currentScreen = "reports" }
                        )
                        "history" -> HistoryScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" },
                            onShiftClick = { shiftId ->
                                selectedShiftId = shiftId
                                currentScreen = "shift_details"
                            }
                        )
                        "shift_details" -> {
                            selectedShiftId?.let { shiftId ->
                                ShiftDetailsScreen(
                                    shiftId = shiftId,
                                    viewModel = viewModel,
                                    onBackClick = { currentScreen = "history" }
                                )
                            } ?: run {
                                currentScreen = "history"
                            }
                        }
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" },
                            onRecurringExpensesClick = { currentScreen = "recurring_expenses" }
                        )
                        "recurring_expenses" -> RecurringExpensesScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "settings" }
                        )
                        "expenses" -> ExpensesScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" }
                        )
                        "reports" -> ReportsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }
    }
}