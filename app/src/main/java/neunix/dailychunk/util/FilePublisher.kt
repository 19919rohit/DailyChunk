package neunix.dailychunk.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

/** Publishes a finished download into the public Download/Daily Chunk folder. */
object FilePublisher {

    private const val SUBFOLDER = "Daily Chunk"

    fun publish(context: Context, source: File, displayName: String, mimeType: String?): Boolean {
        if (!source.exists()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishViaMediaStore(context, source, displayName, mimeType)
        } else {
            publishLegacy(context, source, displayName)
        }
    }

    private fun publishViaMediaStore(context: Context, source: File, displayName: String, mimeType: String?): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$SUBFOLDER")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            val opened = resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(source).use { input -> input.copyTo(out) }
                true
            } ?: false
            if (!opened) {
                resolver.delete(uri, null, null)
                return false
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    private fun publishLegacy(context: Context, source: File, displayName: String): Boolean {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SUBFOLDER)
            dir.mkdirs()
            val dest = File(dir, displayName)
            source.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun publicDisplayPath(fileName: String): String = "Download/$SUBFOLDER/$fileName"
}