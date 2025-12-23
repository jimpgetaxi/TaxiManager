package com.taxipro.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.PaymentType
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivablesScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val unpaidJobs by viewModel.unpaidJobs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var jobToMarkPaid by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receivables / Contracts") }, // TODO: Add to strings
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (unpaidJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No unpaid contract jobs.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalReceivables = unpaidJobs.sumOf { it.revenue }
                item {
                    Card(
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                         Row(
                             modifier = Modifier.padding(16.dp).fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("Total Receivables:", style = MaterialTheme.typography.titleMedium)
                             Text(
                                 "{uiState.currencySymbol}${"%.2f".format(totalReceivables)}", 
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Bold
                             )
                         }
                    }
                }
                
                items(unpaidJobs) { job ->
                    UnpaidJobItem(
                        job = job,
                        currencySymbol = uiState.currencySymbol,
                        onMarkPaidClick = { jobToMarkPaid = job }
                    )
                }
            }
        }
    }

    if (jobToMarkPaid != null) {
        AlertDialog(
            onDismissRequest = { jobToMarkPaid = null },
            title = { Text("Mark as Paid?") },
            text = { Text("Has this invoice been paid?\nAmount: {uiState.currencySymbol}{jobToMarkPaid?.revenue}") },
            confirmButton = {
                TextButton(onClick = {
                    jobToMarkPaid?.let { viewModel.markJobAsPaid(it) }
                    jobToMarkPaid = null
                }) {
                    Text("Yes, Paid")
                }
            },
            dismissButton = {
                TextButton(onClick = { jobToMarkPaid = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UnpaidJobItem(
    job: Job,
    currencySymbol: String,
    onMarkPaidClick: () -> Unit
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
                    text = dateFormat.format(Date(job.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = job.notes ?: "Contract Job",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Invoice Pending", // Or "Payment Pending"
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                 Text(
                    text = "$currencySymbol${"%.2f".format(job.revenue)}",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = onMarkPaidClick) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paid")
                }
            }
        }
    }
}
