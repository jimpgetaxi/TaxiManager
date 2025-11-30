package com.taxipro.manager.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taxipro.manager.R
import com.taxipro.manager.ui.viewmodel.MainViewModel
import com.taxipro.manager.ui.viewmodel.ReportPeriod
import com.taxipro.manager.utils.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.reportsUiState.collectAsState()
    val reportPeriod = state.reportPeriod
    val selectedDate = state.selectedDate
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("el-GR"))
    val yearFormat = SimpleDateFormat("yyyy", Locale.forLanguageTag("el-GR"))
    val fileDateFormat = SimpleDateFormat("yyyy_MM", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val (shifts, expenses) = viewModel.getReportData().first()
                            val dateStr = fileDateFormat.format(Date(selectedDate))
                            
                            val shiftsUri = CsvExporter.exportShifts(
                                context, 
                                shifts, 
                                "shifts_report_$dateStr.csv"
                            )
                            val expensesUri = CsvExporter.exportExpenses(
                                context, 
                                expenses, 
                                "expenses_report_$dateStr.csv"
                            )
                            
                            if (shiftsUri != null && expensesUri != null) {
                                val uris = ArrayList<Uri>()
                                uris.add(shiftsUri)
                                uris.add(expensesUri)
                                
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "text/csv"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Report"))
                            } else {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Report")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TabButton(
                    text = stringResource(R.string.monthly_tab),
                    selected = reportPeriod == ReportPeriod.MONTHLY,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.MONTHLY) },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = stringResource(R.string.yearly_tab),
                    selected = reportPeriod == ReportPeriod.YEARLY,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.YEARLY) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    changeDate(viewModel, reportPeriod, selectedDate, -1)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }

                Text(
                    text = if (reportPeriod == ReportPeriod.MONTHLY) 
                        monthFormat.format(Date(selectedDate)).uppercase()
                    else 
                        yearFormat.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    changeDate(viewModel, reportPeriod, selectedDate, 1)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Report Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReportCard(
                    title = stringResource(R.string.total_income),
                    amount = state.totalIncome,
                    currency = state.currencySymbol,
                    color = MaterialTheme.colorScheme.primary
                )
                ReportCard(
                    title = stringResource(R.string.total_expenses_label),
                    amount = state.totalExpenses,
                    currency = state.currencySymbol,
                    color = MaterialTheme.colorScheme.error
                )
                
                HorizontalDivider()
                
                ReportCard(
                    title = stringResource(R.string.net_income),
                    amount = state.netIncome,
                    currency = state.currencySymbol,
                    color = if (state.netIncome >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.vat_analysis), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VatColumn(
                        title = stringResource(R.string.vat_collected),
                        amount = state.vatCollected,
                        currency = state.currencySymbol
                    )
                    VatColumn(
                        title = stringResource(R.string.vat_deductible),
                        amount = state.vatDeductible,
                        currency = state.currencySymbol
                    )
                    VatColumn(
                        title = stringResource(R.string.vat_payable),
                        amount = state.vatPayable,
                        currency = state.currencySymbol,
                        isBold = true
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ReportCard(
    title: String,
    amount: Double,
    currency: String,
    color: Color
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
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$currency${"%.2f".format(amount)}",
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VatColumn(
    title: String,
    amount: Double,
    currency: String,
    isBold: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "$currency${"%.2f".format(amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun changeDate(
    viewModel: MainViewModel,
    period: ReportPeriod,
    currentDate: Long,
    amount: Int
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentDate
    
    if (period == ReportPeriod.MONTHLY) {
        calendar.add(Calendar.MONTH, amount)
    } else {
        calendar.add(Calendar.YEAR, amount)
    }
    
    viewModel.setReportDate(calendar.timeInMillis)
}
