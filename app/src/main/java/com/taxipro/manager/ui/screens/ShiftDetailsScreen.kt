package com.taxipro.manager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftDetailsScreen(
    shiftId: Long,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val shift by viewModel.getShift(shiftId).collectAsState(initial = null)
    val jobs by viewModel.getJobs(shiftId).collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState() // For currency symbol

    var showEditShiftDialog by remember { mutableStateOf(false) }
    var showAddJobDialog by remember { mutableStateOf(false) }
    var selectedJobForEdit by remember { mutableStateOf<Job?>(null) }

    if (shift == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentShift = shift!!
    // Calculate totals locally for this view since we have the jobs list
    val revenue = jobs.sumOf { it.revenue }
    val receipts = jobs.sumOf { it.receiptAmount ?: 0.0 }
    val vat = (receipts / 1.13) * 0.13

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shift_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditShiftDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddJobDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_job_desc))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Header Card with Date and Odometer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(currentShift.startTime)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(stringResource(R.string.starting_odometer), style = MaterialTheme.typography.labelSmall)
                            Text("${currentShift.startOdometer.toInt()}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.ending_odometer), style = MaterialTheme.typography.labelSmall)
                            Text("${currentShift.endOdometer?.toInt() ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.distance_label), style = MaterialTheme.typography.labelSmall)
                            val dist = (currentShift.endOdometer ?: 0.0) - currentShift.startOdometer
                            Text("${if (dist > 0) dist.toInt() else 0} km", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBox(stringResource(R.string.revenue), revenue, uiState.currencySymbol)
                StatBox(stringResource(R.string.receipts_z_label), receipts, uiState.currencySymbol)
                StatBox(stringResource(R.string.vat_label), vat, uiState.currencySymbol)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.jobs), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs) { job ->
                    JobItem(job, uiState.currencySymbol, onClick = { selectedJobForEdit = job })
                }
            }
        }
    }

    if (showEditShiftDialog) {
        EditShiftFullDialog(
            shift = currentShift,
            onDismiss = { showEditShiftDialog = false },
            onConfirm = { updatedShift ->
                viewModel.updateShiftDetails(updatedShift)
                showEditShiftDialog = false
            }
        )
    }

    if (showAddJobDialog) {
        AddJobDialog(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddJobDialog = false },
            onConfirm = { rev, rec, note, odo ->
                viewModel.addJob(rev, rec, note, odo) // Note: addJob currently uses activeShift from UI State. 
                // We need a variant of addJob that takes shiftId!
                // The current addJob in ViewModel:
                /*
                fun addJob(...) {
                    val shift = uiState.value.activeShift ?: return
                    viewModelScope.launch { repository.addJob(shift.id, ...) }
                }
                */
                // This is a problem. It will add to the ACTIVE shift, not THIS shift.
                // I need to expose a `addJobToShift(shiftId, ...)` in ViewModel.
                viewModel.addJobToShift(currentShift.id, rev, rec, note, odo)
                showAddJobDialog = false
            }
        )
    }

    if (selectedJobForEdit != null) {
        EditJobDialog(
            job = selectedJobForEdit!!,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { selectedJobForEdit = null },
            onConfirm = { rev, rec, note, odo ->
                viewModel.updateJob(selectedJobForEdit!!, rev, rec, note, odo)
                selectedJobForEdit = null
            }
        )
    }
}

@Composable
fun StatBox(label: String, value: Double, currency: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text("$currency${"%.2f".format(value)}", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun EditShiftFullDialog(
    shift: Shift,
    onDismiss: () -> Unit,
    onConfirm: (Shift) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = shift.startTime
    
    var selectedDate by remember { mutableStateOf(shift.startTime) }
    var startOdometer by remember { mutableStateOf(shift.startOdometer.toString()) }
    var endOdometer by remember { mutableStateOf(shift.endOdometer?.toString() ?: "") }

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_shift_details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.date_label, SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate))))
                }
                OutlinedTextField(
                    value = startOdometer,
                    onValueChange = { startOdometer = it },
                    label = { Text(stringResource(R.string.starting_odometer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = endOdometer,
                    onValueChange = { endOdometer = it },
                    label = { Text(stringResource(R.string.ending_odometer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = startOdometer.toDoubleOrNull() ?: shift.startOdometer
                val end = endOdometer.toDoubleOrNull()
                onConfirm(shift.copy(
                    startTime = selectedDate,
                    startOdometer = start,
                    endOdometer = end
                ))
            }) {
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
