package neunix.dailychunk.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

data class ManagedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val mimeType: String?
)

/** Reads/writes files that live in the public Download/Daily Chunk folder. */
class FilesRepository(private val context: Context) {

    fun listFiles(): List<ManagedFile> {
        val result = mutableListOf<ManagedFile>()
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED,
            MediaStore.Downloads.MIME_TYPE
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("%Daily Chunk%")
        try {
            context.contentResolver.query(collection, projection, selection, args, "${MediaStore.Downloads.DATE_ADDED} DESC")
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        result.add(
                            ManagedFile(
                                uri = Uri.withAppendedPath(collection, id.toString()),
                                name = cursor.getString(nameCol) ?: "file",
                                sizeBytes = cursor.getLong(sizeCol),
                                dateAddedMillis = cursor.getLong(dateCol) * 1000L,
                                mimeType = cursor.getString(mimeCol)
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            // Ignore and fall back below
        }

        if (result.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Daily Chunk")
            dir.listFiles()?.forEach { f ->
                result.add(
                    ManagedFile(
                        uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f),
                        name = f.name,
                        sizeBytes = f.length(),
                        dateAddedMillis = f.lastModified(),
                        mimeType = null
                    )
                )
            }
        }
        return result
    }

    fun delete(file: ManagedFile): Boolean = try {
        context.contentResolver.delete(file.uri, null, null) > 0
    } catch (e: Exception) {
        false
    }

    fun deleteByName(name: String): Boolean {
        val match = listFiles().firstOrNull { it.name == name } ?: return false
        return delete(match)
    }

    fun rename(file: ManagedFile, newName: String): Boolean = try {
        val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, newName) }
        context.contentResolver.update(file.uri, values, null, null) > 0
    } catch (e: Exception) {
        false
    }
}