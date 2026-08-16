package neunix.dailychunk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val destinationPath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val cycleLimitBytes: Long,
    val cycleUsedBytes: Long,
    val cycleIntervalMillis: Long,
    val nextCycleAtMillis: Long,
    val status: DownloadStatus,
    val supportsRange: Boolean,
    val mimeType: String?,
    val errorMessage: String?,
    val retryCount: Int = 0,
    val lastSpeedBps: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
)