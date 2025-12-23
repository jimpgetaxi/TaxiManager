package com.taxipro.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.PaymentType
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ShoppingCart

import androidx.compose.material.icons.filled.Timeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onReportsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onForecastClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartShiftDialog by remember { mutableStateOf(false) }
    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showAddJobDialog by remember { mutableStateOf(false) }
    var selectedJobForEdit by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onForecastClick) {
                        Icon(Icons.Default.Timeline, contentDescription = "Forecast")
                    }
                    IconButton(onClick = onReceivablesClick) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Receivables")
                    }
                    IconButton(onClick = onReportsClick) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.reports_title))
                    }
                    IconButton(onClick = onExpensesClick) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.expenses_title))
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.history_desc))
                    }
                    IconButton(onClick = onSettingsClick) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_active_shift),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
            onConfirm = { revenue, receiptAmount, notes, odometer, paymentType, isPaid ->
                viewModel.addJob(revenue, receiptAmount, notes, odometer, paymentType, isPaid)
                showAddJobDialog = false
            }
        )
    }

    if (selectedJobForEdit != null) {
        EditJobDialog(
            job = selectedJobForEdit!!,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { selectedJobForEdit = null },
            onConfirm = { revenue, receiptAmount, notes, odometer, paymentType, isPaid ->
                viewModel.updateJob(selectedJobForEdit!!, revenue, receiptAmount, notes, odometer, paymentType, isPaid)
                selectedJobForEdit = null
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.current_shift).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Main Stats: Revenue & Mileage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.revenue), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.currentRevenue)}",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.mileage), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${"%.1f".format(uiState.currentMileage)} km",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Stats: Receipts (Z) & Revenue VAT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.receipts_z_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.totalReceipts)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.vat_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.totalVat)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tertiary Stats: Deductible VAT & Payable VAT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.deductible_vat_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.totalDeductibleVat)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.payable_vat_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.payableVat)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vehicle Cost & Credit Card Debt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Υπόλοιπο Πιστωτικής", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.creditCardDebt)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.vehicle_cost_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${uiState.currencySymbol}${"%.2f".format(uiState.vehicleCostForCurrentShift)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onEndShift,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
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
    val containerColor = if (!job.isPaid) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.time_label, dateFormat.format(Date(job.timestamp))), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(job.paymentType) {
                            PaymentType.CASH -> "Cash"
                            PaymentType.POS -> "POS"
                            PaymentType.CONTRACT -> "Contract"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
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
    onConfirm: (Double, Double?, String?, Double?, PaymentType, Boolean) -> Unit
) {
    var revenue by remember { mutableStateOf(job.revenue.toString()) }
    var receiptAmount by remember { mutableStateOf(job.receiptAmount?.toString() ?: "") }
    var notes by remember { mutableStateOf(job.notes ?: "") }
    var odometer by remember { mutableStateOf(job.currentOdometer?.toString() ?: "") }
    var paymentType by remember { mutableStateOf(job.paymentType) }
    var isPaid by remember { mutableStateOf(job.isPaid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_job_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaymentTypeChip(
                        type = PaymentType.CASH, 
                        selected = paymentType == PaymentType.CASH, 
                        onClick = { paymentType = PaymentType.CASH }
                    )
                    PaymentTypeChip(
                        type = PaymentType.POS, 
                        selected = paymentType == PaymentType.POS, 
                        onClick = { paymentType = PaymentType.POS }
                    )
                    PaymentTypeChip(
                        type = PaymentType.CONTRACT, 
                        selected = paymentType == PaymentType.CONTRACT, 
                        onClick = { paymentType = PaymentType.CONTRACT }
                    )
                }

                OutlinedTextField(
                    value = revenue,
                    onValueChange = { revenue = it },
                    label = { Text(stringResource(R.string.revenue_label, currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = receiptAmount,
                    onValueChange = { receiptAmount = it },
                    label = { Text(stringResource(R.string.receipt_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text(stringResource(R.string.current_odometer_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paid")
                    Switch(checked = isPaid, onCheckedChange = { isPaid = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                revenue.toDoubleOrNull()?.let { rev ->
                    onConfirm(rev, receiptAmount.toDoubleOrNull(), notes.takeIf { it.isNotBlank() }, odometer.toDoubleOrNull(), paymentType, isPaid)
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
fun AddJobDialog(
    currencySymbol: String, 
    onDismiss: () -> Unit, 
    onConfirm: (Double, Double?, String?, Double?, PaymentType, Boolean) -> Unit
) {
    var revenue by remember { mutableStateOf("") }
    var receiptAmount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentType.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_job_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Payment Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaymentTypeChip(
                        type = PaymentType.CASH, 
                        selected = paymentType == PaymentType.CASH, 
                        onClick = { paymentType = PaymentType.CASH }
                    )
                    PaymentTypeChip(
                        type = PaymentType.POS, 
                        selected = paymentType == PaymentType.POS, 
                        onClick = { paymentType = PaymentType.POS }
                    )
                    PaymentTypeChip(
                        type = PaymentType.CONTRACT, 
                        selected = paymentType == PaymentType.CONTRACT, 
                        onClick = { paymentType = PaymentType.CONTRACT }
                    )
                }

                OutlinedTextField(
                    value = revenue,
                    onValueChange = { revenue = it },
                    label = { Text(stringResource(R.string.revenue_label, currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (paymentType != PaymentType.CONTRACT) {
                    OutlinedTextField(
                        value = receiptAmount,
                        onValueChange = { receiptAmount = it },
                        label = { Text(stringResource(R.string.receipt_amount_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Contract jobs usually don't have immediate receipts (Z). Receipt amount will be 0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text(stringResource(R.string.current_odometer_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                revenue.toDoubleOrNull()?.let { rev ->
                    val receipt = if (paymentType == PaymentType.CONTRACT) 0.0 else receiptAmount.toDoubleOrNull()
                    val isPaid = paymentType != PaymentType.CONTRACT
                    onConfirm(rev, receipt, notes.takeIf { it.isNotBlank() }, odometer.toDoubleOrNull(), paymentType, isPaid)
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
fun PaymentTypeChip(
    type: PaymentType,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Text(
                when(type) {
                    PaymentType.CASH -> "Cash"
                    PaymentType.POS -> "POS"
                    PaymentType.CONTRACT -> "Contract"
                }
            ) 
        }
    )
}


