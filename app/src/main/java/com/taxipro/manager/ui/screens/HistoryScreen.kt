package com.taxipro.manager.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.data.local.entity.Shift
import com.taxipro.manager.data.local.entity.ShiftSummary
import com.taxipro.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onShiftClick: (Long) -> Unit
) {
    val shiftHistory by viewModel.shiftHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (shiftHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_history))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shiftHistory) { summary ->
                    HistoryItem(
                        summary = summary,
                        currencySymbol = uiState.currencySymbol,
                        onClick = { onShiftClick(summary.shift.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    summary: ShiftSummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val revenue = summary.totalRevenue ?: 0.0
    val receipts = summary.totalReceipts ?: 0.0
    val vat = (receipts / 1.13) * 0.13

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(summary.shift.startTime)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_desc), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.time_label, timeFormat.format(Date(summary.shift.startTime))), style = MaterialTheme.typography.bodyMedium)
                // Just show raw numbers for odometer in history summary
                Text("${summary.shift.startOdometer.toInt()} - ${summary.shift.endOdometer?.toInt() ?: "..."}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.revenue), style = MaterialTheme.typography.bodySmall)
                    Text("$currencySymbol${"%.2f".format(revenue)}", style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text(stringResource(R.string.receipts_z_label), style = MaterialTheme.typography.bodySmall)
                    Text("$currencySymbol${"%.2f".format(receipts)}", style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text(stringResource(R.string.vat_label), style = MaterialTheme.typography.bodySmall)
                    Text("$currencySymbol${"%.2f".format(vat)}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
