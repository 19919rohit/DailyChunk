package neunix.dailychunk.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    fun formatDate(millis: Long): String {
        val fmt = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(millis))
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

object NetworkUtils {
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}