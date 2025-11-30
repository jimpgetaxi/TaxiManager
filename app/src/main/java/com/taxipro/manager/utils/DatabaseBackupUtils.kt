package com.taxipro.manager.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackupUtils {

    private const val DB_NAME = "taxi_database"

    fun backupDatabase(context: Context): Uri? {
        val dbFile = context.getDatabasePath(DB_NAME)
        val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
        val dbShmFile = context.getDatabasePath("$DB_NAME-shm")
        
        val backupFile = File(context.cacheDir, "taxi_backup.zip")
        
        try {
            FileOutputStream(backupFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    addToZip(zos, dbFile)
                    if (dbWalFile.exists()) addToZip(zos, dbWalFile)
                    if (dbShmFile.exists()) addToZip(zos, dbShmFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
    }

    fun restoreDatabase(context: Context, backupUri: Uri): Boolean {
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
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
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
