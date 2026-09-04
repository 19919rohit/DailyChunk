package neunix.dailychunk.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLinks {
    const val REPO_URL = "https://github.com/19919rohit/DailyChunk"

    fun openRepo(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
        } catch (e: Exception) {
            // No browser available — fail silently rather than crash.
        }
    }
}