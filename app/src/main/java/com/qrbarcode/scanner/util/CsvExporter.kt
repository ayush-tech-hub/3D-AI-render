package com.qrbarcode.scanner.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.qrbarcode.scanner.domain.model.ScanItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun buildShareIntent(context: Context, scans: List<ScanItem>): Intent {
        val csv = buildString {
            appendLine("ID,Type,Format,Content,Raw Value,Date,Pinned")
            scans.forEach { scan ->
                val date = dateFormat.format(Date(scan.timestamp))
                appendLine(
                    "${scan.id}," +
                    "\"${scan.type.name}\"," +
                    "\"${scan.format}\"," +
                    "\"${scan.content.replace("\"", "\"\"")}\"," +
                    "\"${scan.rawValue.replace("\"", "\"\"")}\"," +
                    "\"$date\"," +
                    "${scan.isPinned}"
                )
            }
        }

        val file = File(context.cacheDir, "scan_history_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { it.write(csv) }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "QR Scanner History Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
