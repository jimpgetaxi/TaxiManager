package com.taxipro.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.RecurringExpense
import com.taxipro.manager.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val recurringExpenses by viewModel.allRecurringExpenses.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecurringExpense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Πάγια Έξοδα") }, // Hardcoded Greek for now or add to strings
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Expense")
            }
        }
    ) { paddingValues ->
        if (recurringExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Δεν υπάρχουν πάγια έξοδα.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recurringExpenses) { expense ->
                    RecurringExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onDeleteClick = { itemToDelete = expense }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringExpenseDialog(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { description, amount, vatAmount, affectsCost, frequency ->
                val expense = RecurringExpense(
                    description = description,
                    amount = amount,
                    vatAmount = vatAmount,
                    affectsCostPerKm = affectsCost,
                    frequency = frequency
                )
                viewModel.addRecurringExpense(expense)
                showAddDialog = false
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Διαγραφή Πάγιου;") },
            text = { Text("${itemToDelete?.description} - ${uiState.currencySymbol}${itemToDelete?.amount}") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { viewModel.deleteRecurringExpense(it) }
                    itemToDelete = null
                }) {
                    Text("Διαγραφή")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Ακύρωση")
                }
            }
        )
    }
}

@Composable
fun RecurringExpenseItem(
    expense: RecurringExpense,
    currencySymbol: String,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expense.frequency == "MONTHLY") "Μηνιαίο" else "Ετήσιο",
                    style = MaterialTheme.typography.bodySmall
                )
                if (expense.vatAmount > 0) {
                    Text(
                        text = "ΦΠΑ: $currencySymbol${"%.2f".format(expense.vatAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currencySymbol${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddRecurringExpenseDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Boolean, String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var affectsCost by remember { mutableStateOf(true) }
    var isInvoice by remember { mutableStateOf(false) }
    var vatRate by remember { mutableStateOf("13%") }
    var manualVatAmount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") } // "MONTHLY" or "YEARLY"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Νέο Πάγιο Έξοδο") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Περιγραφή") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Ποσό ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Συχνότητα:", modifier = Modifier.padding(end = 8.dp))
                    FilterChip(
                        selected = frequency == "MONTHLY",
                        onClick = { frequency = "MONTHLY" },
                        label = { Text("Μήνας") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = frequency == "YEARLY",
                        onClick = { frequency = "YEARLY" },
                        label = { Text("Έτος") }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Επηρεάζει Κόστος/χλμ", modifier = Modifier.weight(1f))
                    Switch(checked = affectsCost, onCheckedChange = { affectsCost = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Είναι Τιμολόγιο (έχει ΦΠΑ);", modifier = Modifier.weight(1f))
                    Switch(checked = isInvoice, onCheckedChange = { isInvoice = it })
                }

                if (isInvoice) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = vatRate == "13%",
                            onClick = { vatRate = "13%" },
                            label = { Text("13%") }
                        )
                        FilterChip(
                            selected = vatRate == "24%",
                            onClick = { vatRate = "24%" },
                            label = { Text("24%") }
                        )
                        FilterChip(
                            selected = vatRate == "Manual",
                            onClick = { vatRate = "Manual" },
                            label = { Text("Manual") }
                        )
                    }

                    if (vatRate == "Manual") {
                        OutlinedTextField(
                            value = manualVatAmount,
                            onValueChange = { manualVatAmount = it },
                            label = { Text("Ποσό ΦΠΑ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && description.isNotBlank()) {
                        val calcVat = if (isInvoice) {
                            when (vatRate) {
                                "13%" -> amt - (amt / 1.13)
                                "24%" -> amt - (amt / 1.24)
                                else -> manualVatAmount.toDoubleOrNull() ?: 0.0
                            }
                        } else 0.0
                        
                        onConfirm(description, amt, calcVat, affectsCost, frequency)
                    }
                }
            ) {
                Text("Προσθήκη")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ακύρωση")
            }
        }
    )
}
