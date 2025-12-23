package com.taxipro.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.taxipro.manager.data.local.TaxiDatabase
import com.taxipro.manager.data.repository.TaxiRepository
import com.taxipro.manager.data.repository.UserPreferencesRepository
import com.taxipro.manager.ui.screens.DashboardScreen
import com.taxipro.manager.ui.screens.ExpensesScreen
import com.taxipro.manager.ui.screens.ForecastScreen
import com.taxipro.manager.ui.screens.HistoryScreen
import com.taxipro.manager.ui.screens.ReceivablesScreen
import com.taxipro.manager.ui.screens.RecurringExpensesScreen
import com.taxipro.manager.ui.screens.ReportsScreen
import com.taxipro.manager.ui.screens.SettingsScreen
import com.taxipro.manager.ui.screens.ShiftDetailsScreen
import com.taxipro.manager.ui.theme.TaxiManagerTheme
import com.taxipro.manager.ui.viewmodel.MainViewModel
import com.taxipro.manager.ui.viewmodel.MainViewModelFactory

sealed class Screen {
    data object Dashboard : Screen()
    data object History : Screen()
    data class ShiftDetails(val shiftId: Long) : Screen()
    data object Settings : Screen()
    data object RecurringExpenses : Screen()
    data object Expenses : Screen()
    data object Reports : Screen()
    data object Receivables : Screen()
    data object Forecast : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = TaxiDatabase.getDatabase(this)
        val repository = TaxiRepository(
            database, 
            database.taxiDao(), 
            database.expenseDao(), 
            database.recurringExpenseDao(),
            database.installmentDao()
        )
        val userPreferencesRepository = UserPreferencesRepository(this)
        val viewModelFactory = MainViewModelFactory(repository, userPreferencesRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            TaxiManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(viewModel)
                }
            }
        }
    }

    @Composable
    private fun MainNavigation(viewModel: MainViewModel) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

        when (val screen = currentScreen) {
            is Screen.Dashboard -> DashboardScreen(
                viewModel = viewModel,
                onHistoryClick = { currentScreen = Screen.History },
                onSettingsClick = { currentScreen = Screen.Settings },
                onExpensesClick = { currentScreen = Screen.Expenses },
                onReportsClick = { currentScreen = Screen.Reports },
                onReceivablesClick = { currentScreen = Screen.Receivables },
                onForecastClick = { currentScreen = Screen.Forecast }
            )
            is Screen.History -> HistoryScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard },
                onShiftClick = { shiftId -> currentScreen = Screen.ShiftDetails(shiftId) }
            )
            is Screen.ShiftDetails -> ShiftDetailsScreen(
                shiftId = screen.shiftId,
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.History }
            )
            is Screen.Settings -> SettingsScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard },
                onRecurringExpensesClick = { currentScreen = Screen.RecurringExpenses }
            )
            is Screen.RecurringExpenses -> RecurringExpensesScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Settings }
            )
            is Screen.Expenses -> ExpensesScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard }
            )
            is Screen.Reports -> ReportsScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard }
            )
            is Screen.Receivables -> ReceivablesScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard }
            )
            is Screen.Forecast -> ForecastScreen(
                viewModel = viewModel,
                onBackClick = { currentScreen = Screen.Dashboard }
            )
        }
    }
}
