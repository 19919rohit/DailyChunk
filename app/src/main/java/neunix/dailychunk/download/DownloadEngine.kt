package neunix.dailychunk.download

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neunix.dailychunk.data.DownloadEntity
import neunix.dailychunk.util.FileUtils
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class AnalyzeResult {
    data class Success(
        val fileName: String,
        val totalBytes: Long,
        val contentType: String?,
        val supportsRange: Boolean
    ) : AnalyzeResult()

    data class Error(val message: String) : AnalyzeResult()
}

class DownloadEngine(private val client: OkHttpClient) {

    sealed class CycleResult {
        object Completed : CycleResult()
        object CycleLimitReached : CycleResult()
        object ManuallyPaused : CycleResult()
        data class Error(val message: String) : CycleResult()
    }

    @Volatile private var pauseRequested = false

    fun requestPause() {
        pauseRequested = true
    }

    suspend fun runCycle(
        download: DownloadEntity,
        onProgress: suspend (downloadedBytes: Long, cycleUsedBytes: Long, speedBps: Long) -> Unit
    ): CycleResult = withContext(Dispatchers.IO) {
        pauseRequested = false
        val partFile = File(download.destinationPath + ".part")
        partFile.parentFile?.mkdirs()
        val startOffset = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(download.url)
        if (download.supportsRange && startOffset > 0) {
            requestBuilder.addHeader("Range", "bytes=$startOffset-")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    return@withContext CycleResult.Error("Server returned HTTP ${response.code}")
                }
                val body = response.body
                    ?: return@withContext CycleResult.Error("Server returned an empty response")

                if (response.code == 200 && startOffset > 0) {
                    // Server ignored our resume request; must restart from scratch.
                    partFile.delete()
                }
                val actualStartOffset = if (response.code == 206) startOffset else 0L
                // If a resume attempt got downgraded to a fresh 200, don't cap this
                // pass at the cycle limit or the download could loop forever.
                val enforceCycleLimit = !(response.code == 200 && startOffset > 0)

                var cycleUsed = 0L
                var totalDownloaded = actualStartOffset
                var lastTickTime = System.currentTimeMillis()
                var bytesSinceLastTick = 0L
                var streamEnded = false

                RandomAccessFile(partFile, "rw").use { raf ->
                    raf.seek(actualStartOffset)
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            if (pauseRequested) return@withContext CycleResult.ManuallyPaused
                            if (enforceCycleLimit && cycleUsed >= download.cycleLimitBytes) {
                                onProgress(totalDownloaded, cycleUsed, 0L)
                                return@withContext CycleResult.CycleLimitReached
                            }

                            val remainingInCycle = download.cycleLimitBytes - cycleUsed
                            val toRead = if (enforceCycleLimit)
                                minOf(buffer.size.toLong(), remainingInCycle).toInt().coerceAtLeast(1)
                            else buffer.size

                            val read = input.read(buffer, 0, toRead)
                            if (read == -1) {
                                streamEnded = true
                                break
                            }

                            raf.write(buffer, 0, read)
                            cycleUsed += read
                            totalDownloaded += read
                            bytesSinceLastTick += read

                            val now = System.currentTimeMillis()
                            if (now - lastTickTime >= 1000) {
                                val speed = (bytesSinceLastTick * 1000L) / (now - lastTickTime)
                                onProgress(totalDownloaded, cycleUsed, speed)
                                bytesSinceLastTick = 0
                                lastTickTime = now
                            }
                        }
                    }
                }

                onProgress(totalDownloaded, cycleUsed, 0L)

                if (streamEnded) {
                    val finalFile = File(download.destinationPath)
                    if (finalFile.exists()) finalFile.delete()
                    if (partFile.renameTo(finalFile)) {
                        CycleResult.Completed
                    } else {
                        CycleResult.Error("Could not finalize the downloaded file")
                    }
                } else {
                    CycleResult.CycleLimitReached
                }
            }
        } catch (e: IOException) {
            CycleResult.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            CycleResult.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun analyze(url: String): AnalyzeResult = withContext(Dispatchers.IO) {
        try {
            val headRequest = Request.Builder().url(url).head().build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    val acceptRanges = response.header("Accept-Ranges")
                    val contentType = response.header("Content-Type")
                    val fileName = extractFileName(response.header("Content-Disposition"), url)
                    AnalyzeResult.Success(
                        fileName = fileName,
                        totalBytes = contentLength,
                        contentType = contentType,
                        supportsRange = acceptRanges?.contains("bytes", ignoreCase = true) == true
                    )
                } else {
                    analyzeWithRangeGet(url)
                }
            }
        } catch (e: Exception) {
            try {
                analyzeWithRangeGet(url)
            } catch (e2: Exception) {
                AnalyzeResult.Error(e2.message ?: "Could not reach the server")
            }
        }
    }

    private fun analyzeWithRangeGet(url: String): AnalyzeResult {
        val request = Request.Builder().url(url).header("Range", "bytes=0-0").build()
        client.newCall(request).execute().use { response ->
            return when {
                response.code == 206 -> {
                    val contentRange = response.header("Content-Range")
                    val total = contentRange?.substringAfterLast("/")?.toLongOrNull() ?: -1L
                    AnalyzeResult.Success(
                        fileName = extractFileName(response.header("Content-Disposition"), url),
                        totalBytes = total,
                        contentType = response.header("Content-Type"),
                        supportsRange = true
                    )
                }
                response.isSuccessful -> {
                    val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    AnalyzeResult.Success(
                        fileName = extractFileName(response.header("Content-Disposition"), url),
                        totalBytes = contentLength,
                        contentType = response.header("Content-Type"),
                        supportsRange = false
                    )
                }
                else -> AnalyzeResult.Error("Server returned HTTP ${response.code}")
            }
        }
    }

    private fun extractFileName(disposition: String?, url: String): String {
        if (disposition != null) {
            val match = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?").find(disposition)
            if (match != null) {
                val raw = match.groupValues[1]
                return try { URLDecoder.decode(raw, "UTF-8") } catch (e: Exception) { raw }
            }
        }
        return FileUtils.extractFileNameFromUrl(url)
    }
}