package com.taxipro.manager.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackupUtils {

    private const val DB_NAME = "taxi_database"

    suspend fun backupDatabase(context: Context): Uri? = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
        val dbShmFile = context.getDatabasePath("$DB_NAME-shm")
        
        val backupFile = File(context.cacheDir, "taxi_backup.zip")
        
        try {
            FileOutputStream(backupFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    if (dbFile.exists()) addToZip(zos, dbFile)
                    if (dbWalFile.exists()) addToZip(zos, dbWalFile)
                    if (dbShmFile.exists()) addToZip(zos, dbShmFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        return@withContext FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
    }

    suspend fun backupDatabaseToUri(context: Context, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
        val dbShmFile = context.getDatabasePath("$DB_NAME-shm")

        try {
            context.contentResolver.openOutputStream(targetUri)?.use { fos ->
                ZipOutputStream(fos).use { zos ->
                    if (dbFile.exists()) addToZip(zos, dbFile)
                    if (dbWalFile.exists()) addToZip(zos, dbWalFile)
                    if (dbShmFile.exists()) addToZip(zos, dbShmFile)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreDatabase(context: Context, backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
        val dbShmFile = context.getDatabasePath("$DB_NAME-shm")
        
        // Delete current DB files
        if (dbFile.exists()) dbFile.delete()
        if (dbWalFile.exists()) dbWalFile.delete()
        if (dbShmFile.exists()) dbShmFile.delete()

        try {
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        // Security check: prevent zip path traversal
                        if (entry.name.contains("..")) {
                             entry = zis.nextEntry
                             continue
                        }

                        val targetFile = context.getDatabasePath(entry.name)
                        // Ensure parent dir exists
                        targetFile.parentFile?.mkdirs()
                        
                        FileOutputStream(targetFile).use { fos ->
                            val buffer = ByteArray(1024)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(file.name)
            zos.putNextEntry(entry)
            val buffer = ByteArray(1024)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zos.write(buffer, 0, len)
            }
            zos.closeEntry()
        }
    }
}
