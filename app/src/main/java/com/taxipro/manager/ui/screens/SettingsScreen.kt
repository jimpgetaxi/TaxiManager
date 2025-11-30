package com.taxipro.manager.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.ui.viewmodel.MainViewModel
import com.taxipro.manager.utils.DatabaseBackupUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val initialKm by viewModel.initialHistoricalKm.collectAsState(initial = 0.0)
    val initialExpenses by viewModel.initialHistoricalExpenses.collectAsState(initial = 0.0)
    val costPerKm by viewModel.costPerKm.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currencySymbol by remember(uiState.currencySymbol) { mutableStateOf(uiState.currencySymbol) }
    // Only initialize state once from flow, to allow editing without it jumping back
    var kmInput by remember { mutableStateOf("") }
    var expensesInput by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    // Initialize inputs when data is loaded and not yet initialized
    LaunchedEffect(initialKm, initialExpenses) {
        if (!initialized && (initialKm > 0 || initialExpenses > 0)) {
            kmInput = if (initialKm > 0) initialKm.toString() else ""
            expensesInput = if (initialExpenses > 0) initialExpenses.toString() else ""
            initialized = true
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val success = DatabaseBackupUtils.restoreDatabase(context, it)
                if (success) {
                    Toast.makeText(context, "Restore successful. Please restart the app.", Toast.LENGTH_LONG).show()
                    // Ideally, we should trigger a restart or re-initialization here.
                    // For now, closing the app or navigating back might be safest to force DB reload.
                    (context as? Activity)?.finish()
                } else {
                    Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text(stringResource(R.string.currency_symbol_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Vehicle Settings Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vehicle_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Result Display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.cost_per_km_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${uiState.currencySymbol}${"%.2f".format(costPerKm)}",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = stringResource(R.string.calculated_cost_info),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = kmInput,
                        onValueChange = { kmInput = it },
                        label = { Text(stringResource(R.string.initial_km_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = expensesInput,
                        onValueChange = { expensesInput = it },
                        label = { Text(stringResource(R.string.initial_expenses_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Data Management Card (Backup & Restore)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management", // Should be in strings.xml
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.checkpoint() // Ensure WAL is flushed
                                val backupUri = DatabaseBackupUtils.backupDatabase(context)
                                if (backupUri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, backupUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Backup Database"))
                                } else {
                                    Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup Database") // Should be in strings.xml
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Database") // Should be in strings.xml
                    }
                    
                    Text(
                        text = "Warning: Restore will overwrite all current data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateCurrency(currencySymbol)
                    val km = kmInput.toDoubleOrNull() ?: 0.0
                    val expenses = expensesInput.toDoubleOrNull() ?: 0.0
                    viewModel.updateVehicleSettings(km, expenses)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_action))
            }
        }
    }
}
