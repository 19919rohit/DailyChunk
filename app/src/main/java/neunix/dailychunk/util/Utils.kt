package neunix.dailychunk.util

import android.net.Uri

object Formatters {
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }

    fun formatSpeed(bps: Long): String = if (bps <= 0) "—" else "${formatBytes(bps)}/s"

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "now"
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

object FileUtils {
    fun sanitizeFileName(name: String): String {
        var clean = name.trim()
        if (clean.isEmpty()) clean = "download_${System.currentTimeMillis()}"
        clean = clean.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        clean = clean.replace("..", "_")
        return clean.take(150)
    }

    fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).lastPathSegment
            if (!path.isNullOrBlank()) path else "download_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }
}