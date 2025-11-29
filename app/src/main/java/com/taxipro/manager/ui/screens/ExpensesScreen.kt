package com.taxipro.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Expense
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExpenseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_job_desc))
            }
        }
    ) { paddingValues ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_expenses))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onDeleteClick = { expenseToDelete = expense }
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { description, amount, isInvoice, vatRate, manualVatAmount, affectsCost ->
                // Calculate VAT amount based on selection
                val calculatedVat = if (isInvoice) {
                    when (vatRate) {
                        "13%" -> amount - (amount / 1.13)
                        "24%" -> amount - (amount / 1.24)
                        else -> manualVatAmount ?: 0.0
                    }
                } else {
                    0.0
                }

                val expense = Expense(
                    timestamp = System.currentTimeMillis(),
                    description = description,
                    amount = amount,
                    vatAmount = calculatedVat,
                    affectsCostPerKm = affectsCost
                )
                viewModel.addExpense(expense)
                showAddExpenseDialog = false
            }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(stringResource(R.string.delete_expense_confirm)) },
            text = { Text("${expenseToDelete?.description} - ${uiState.currencySymbol}${expenseToDelete?.amount}") },
            confirmButton = {
                TextButton(onClick = {
                    expenseToDelete?.let { viewModel.deleteExpense(it) }
                    expenseToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    currencySymbol: String,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
                    text = dateFormat.format(Date(expense.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                if (expense.vatAmount > 0) {
                    Text(
                        text = "VAT: $currencySymbol${"%.2f".format(expense.vatAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!expense.affectsCostPerKm) {
                     Text(
                        text = "(Not in Cost/Km)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
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
fun AddExpenseDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Boolean, String, Double?, Boolean) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var affectsCost by remember { mutableStateOf(true) }
    var isInvoice by remember { mutableStateOf(false) }
    var vatRate by remember { mutableStateOf("13%") } // "13%", "24%", "Manual"
    var manualVatAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_expense_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_label)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount_label, currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.affects_cost_label), modifier = Modifier.weight(1f))
                    Switch(checked = affectsCost, onCheckedChange = { affectsCost = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.is_invoice_label), modifier = Modifier.weight(1f))
                    Switch(checked = isInvoice, onCheckedChange = { isInvoice = it })
                }

                if (isInvoice) {
                    Text(stringResource(R.string.vat_rate_label), style = MaterialTheme.typography.labelMedium)
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
                            label = { Text(stringResource(R.string.manual_vat)) }
                        )
                    }

                    if (vatRate == "Manual") {
                        OutlinedTextField(
                            value = manualVatAmount,
                            onValueChange = { manualVatAmount = it },
                            label = { Text(stringResource(R.string.vat_amount_label)) },
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
                        onConfirm(
                            description,
                            amt,
                            isInvoice,
                            vatRate,
                            manualVatAmount.toDoubleOrNull(),
                            affectsCost
                        )
                    }
                }
            ) {
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
