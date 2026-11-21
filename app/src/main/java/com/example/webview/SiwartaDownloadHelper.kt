package com.example.webview

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

data class SavedExportResult(
  val fileName: String,
  val mimeType: String,
  val contentUri: Uri,
  val savedToDownloads: Boolean,
)

object SiwartaDownloadHelper {

  fun saveBase64Export(
    context: Context,
    dataUrlOrBase64: String,
    rawFileName: String,
    rawMimeType: String?,
  ): Result<SavedExportResult> = runCatching {
    val cleanFileName = sanitizeFileName(rawFileName.ifBlank { "SIWARTA_Export" })
    val (parsedMime, base64Payload) = parseDataUrl(dataUrlOrBase64, rawMimeType)
    val resolvedMime = resolveMimeType(cleanFileName, parsedMime)
    val bytes = Base64.decode(base64Payload, Base64.DEFAULT)

    // Always write a shareable copy in cacheDir/exports for instant Open/Share via FileProvider
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val cachedFile = File(exportDir, cleanFileName)
    cachedFile.writeBytes(bytes)
    val shareUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachedFile)

    var savedUri: Uri = shareUri
    var savedToDownloads = false

    // On Android 10+ (API 29+), save directly to public Downloads via MediaStore (zero permissions)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values =
        ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, cleanFileName)
          put(MediaStore.Downloads.MIME_TYPE, resolvedMime)
          put(
            MediaStore.Downloads.RELATIVE_PATH,
            "${Environment.DIRECTORY_DOWNLOADS}/SIWARTA",
          )
          put(MediaStore.Downloads.IS_PENDING, 1)
        }
      val resolver = context.contentResolver
      val mediaUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
      if (mediaUri != null) {
        resolver.openOutputStream(mediaUri)?.use { out ->
          out.write(bytes)
          out.flush()
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(mediaUri, values, null, null)
        savedUri = mediaUri
        savedToDownloads = true
      }
    }

    SavedExportResult(
      fileName = cleanFileName,
      mimeType = resolvedMime,
      contentUri = shareUri, // FileProvider URI works reliably for ACTION_VIEW across all API levels
      savedToDownloads = savedToDownloads || savedUri != shareUri,
    )
  }

  fun openExportedFile(context: Context, export: SavedExportResult): Boolean {
    val viewIntent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(export.contentUri, export.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return runCatching {
        context.startActivity(viewIntent)
        true
      }
      .getOrElse {
        val shareIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = export.mimeType
            putExtra(Intent.EXTRA_STREAM, export.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        runCatching {
            context.startActivity(
              Intent.createChooser(shareIntent, "Bagikan / Buka ${export.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
            )
            true
          }
          .getOrDefault(false)
      }
  }

  private fun parseDataUrl(input: String, fallbackMime: String?): Pair<String, String> {
    val trimmed = input.trim()
    if (trimmed.startsWith("data:", ignoreCase = true)) {
      val commaIdx = trimmed.indexOf(',')
      if (commaIdx > 0) {
        val header = trimmed.substring(5, commaIdx)
        val mime = header.substringBefore(';').ifBlank { fallbackMime ?: "application/octet-stream" }
        val payload = trimmed.substring(commaIdx + 1)
        return mime to payload
      }
    }
    return (fallbackMime ?: "application/octet-stream") to trimmed
  }

  private fun resolveMimeType(fileName: String, parsedMime: String): String {
    if (parsedMime != "application/octet-stream" && parsedMime.isNotBlank()) return parsedMime
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
      "pdf" -> "application/pdf"
      "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      "xls" -> "application/vnd.ms-excel"
      "jpg",
      "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "json" -> "application/json"
      else ->
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
  }

  private fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(120)
  }
}
