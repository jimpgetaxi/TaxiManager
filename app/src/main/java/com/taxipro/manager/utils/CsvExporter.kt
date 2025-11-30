package com.taxipro.manager.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.taxipro.manager.data.local.entity.Expense
import com.taxipro.manager.data.local.entity.ShiftSummary
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportShifts(context: Context, shifts: List<ShiftSummary>, fileName: String): Uri? {
        val file = createReportFile(context, fileName) ?: return null
        
        try {
            FileWriter(file).use { writer ->
                // BOM for Excel compatibility with UTF-8
                writer.write("\uFEFF")
                // Header
                writer.append("Date,Start Time,End Time,Distance (km),Revenue,Receipts (Z),VAT (Included),Notes\n")
                
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                for (summary in shifts) {
                    val shift = summary.shift
                    val date = dateFormat.format(Date(shift.startTime))
                    val start = timeFormat.format(Date(shift.startTime))
                    val endTimeVal = shift.endTime
                    val end = if (endTimeVal != null) timeFormat.format(Date(endTimeVal)) else "-"
                    val dist = if (shift.endOdometer != null) "%.1f".format(shift.endOdometer - shift.startOdometer) else "0"
                    val rev = "%.2f".format(summary.totalRevenue ?: 0.0)
                    val rec = "%.2f".format(summary.totalReceipts ?: 0.0)
                    // VAT 13% included
                    val vat = "%.2f".format((summary.totalReceipts ?: 0.0) / 1.13 * 0.13)
                    
                    writer.append("$date,$start,$end,$dist,$rev,$rec,$vat,\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun exportExpenses(context: Context, expenses: List<Expense>, fileName: String): Uri? {
        val file = createReportFile(context, fileName) ?: return null
        
        try {
            FileWriter(file).use { writer ->
                // BOM for Excel compatibility with UTF-8
                writer.write("\uFEFF")
                // Header
                writer.append("Date,Description,Amount,VAT Amount,Deductible VAT,Invoice?\n")
                
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (expense in expenses) {
                    val date = dateFormat.format(Date(expense.timestamp))
                    val amount = "%.2f".format(expense.amount)
                    val vat = "%.2f".format(expense.vatAmount)
                    val isInvoice = if (expense.vatAmount > 0) "Yes" else "No"
                    val desc = expense.description.replace(",", " ") // Escape commas
                    
                    writer.append("$date,$desc,$amount,$vat,$vat,$isInvoice\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun createReportFile(context: Context, fileName: String): File? {
        val reportDir = File(context.cacheDir, "reports")
        if (!reportDir.exists()) {
            if (!reportDir.mkdirs()) return null
        }
        return File(reportDir, fileName)
    }
}
