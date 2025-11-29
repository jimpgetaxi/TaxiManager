package com.taxipro.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartShiftDialog by remember { mutableStateOf(false) }
    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showAddJobDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedJobForEdit by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.activeShift != null) {
                FloatingActionButton(onClick = { showAddJobDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_job_desc))
                }
            } else {
                FloatingActionButton(onClick = { showStartShiftDialog = true }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.start_shift_desc))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (uiState.activeShift != null) {
                ActiveShiftDashboard(
                    uiState = uiState,
                    onEndShift = { showEndShiftDialog = true },
                    onJobClick = { job -> selectedJobForEdit = job }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_active_shift))
                }
            }
        }
    }

    if (showStartShiftDialog) {
        StartShiftDialog(
            onDismiss = { showStartShiftDialog = false },
            onConfirm = { odometer ->
                viewModel.startShift(odometer)
                showStartShiftDialog = false
            }
        )
    }

    if (showEndShiftDialog) {
        EndShiftDialog(
            onDismiss = { showEndShiftDialog = false },
            onConfirm = { odometer ->
                viewModel.endShift(odometer)
                showEndShiftDialog = false
            }
        )
    }

    if (showAddJobDialog) {
        AddJobDialog(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddJobDialog = false },
            onConfirm = { revenue, notes, odometer ->
                viewModel.addJob(revenue, notes, odometer)
                showAddJobDialog = false
            }
        )
    }

    if (selectedJobForEdit != null) {
        EditJobDialog(
            job = selectedJobForEdit!!,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { selectedJobForEdit = null },
            onConfirm = { revenue, notes, odometer ->
                viewModel.updateJob(selectedJobForEdit!!, revenue, notes, odometer)
                selectedJobForEdit = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSymbol = uiState.currencySymbol,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSymbol ->
                viewModel.updateCurrency(newSymbol)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun ActiveShiftDashboard(
    uiState: com.taxipro.manager.ui.viewmodel.DashboardUiState,
    onEndShift: () -> Unit,
    onJobClick: (Job) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.current_shift), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.revenue), style = MaterialTheme.typography.labelMedium)
                        Text("${uiState.currencySymbol}${"%.2f".format(uiState.currentRevenue)}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Column {
                        Text(stringResource(R.string.mileage), style = MaterialTheme.typography.labelMedium)
                        Text("${"%.1f".format(uiState.currentMileage)} km", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEndShift,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.end_shift))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.jobs), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.currentJobs) { job ->
                JobItem(job, uiState.currencySymbol, onClick = { onJobClick(job) })
            }
        }
    }
}

@Composable
fun JobItem(job: Job, currencySymbol: String, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.time_label, dateFormat.format(Date(job.timestamp))), style = MaterialTheme.typography.bodySmall)
                if (!job.notes.isNullOrEmpty()) {
                    Text(job.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("$currencySymbol${"%.2f".format(job.revenue)}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ... StartShiftDialog, EndShiftDialog, AddJobDialog remain the same ...

@Composable
fun EditJobDialog(
    job: Job,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?, Double?) -> Unit
) {
    var revenue by remember { mutableStateOf(job.revenue.toString()) }
    var notes by remember { mutableStateOf(job.notes ?: "") }
    var odometer by remember { mutableStateOf(job.currentOdometer?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_job_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = revenue,
                    onValueChange = { revenue = it },
                    label = { Text(stringResource(R.string.revenue_label, currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) }
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text(stringResource(R.string.current_odometer_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                revenue.toDoubleOrNull()?.let { rev ->
                    onConfirm(rev, notes.takeIf { it.isNotBlank() }, odometer.toDoubleOrNull())
                }
            }) {
                Text(stringResource(R.string.update_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}


@Composable
fun StartShiftDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var odometer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_shift_title)) },
        text = {
            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it },
                label = { Text(stringResource(R.string.starting_odometer)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                odometer.toDoubleOrNull()?.let { onConfirm(it) }
            }) {
                Text(stringResource(R.string.start_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

@Composable
fun EndShiftDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var odometer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.end_shift_title)) },
        text = {
            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it },
                label = { Text(stringResource(R.string.ending_odometer)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                odometer.toDoubleOrNull()?.let { onConfirm(it) }
            }) {
                Text(stringResource(R.string.end_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

@Composable
fun AddJobDialog(currencySymbol: String, onDismiss: () -> Unit, onConfirm: (Double, String?, Double?) -> Unit) {
    var revenue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_job_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = revenue,
                    onValueChange = { revenue = it },
                    label = { Text(stringResource(R.string.revenue_label, currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) }
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text(stringResource(R.string.current_odometer_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                revenue.toDoubleOrNull()?.let { rev ->
                    onConfirm(rev, notes.takeIf { it.isNotBlank() }, odometer.toDoubleOrNull())
                }
            }) {
                Text(stringResource(R.string.add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

@Composable
fun SettingsDialog(currentSymbol: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var symbol by remember { mutableStateOf(currentSymbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it },
                label = { Text(stringResource(R.string.currency_symbol_label)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(symbol) }) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}

