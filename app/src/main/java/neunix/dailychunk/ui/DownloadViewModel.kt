package neunix.dailychunk.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadEntity
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.download.AnalyzeResult
import neunix.dailychunk.download.DownloadService
import neunix.dailychunk.util.FileUtils
import neunix.dailychunk.work.Scheduler

sealed class AnalyzeUiState {
    object Idle : AnalyzeUiState()
    object Loading : AnalyzeUiState()
    data class Success(val result: AnalyzeResult.Success) : AnalyzeUiState()
    data class Error(val message: String) : AnalyzeUiState()
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repo get() = AppContainer.repository

    val downloads: StateFlow<List<DownloadEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observe(id: Long): Flow<DownloadEntity?> = repo.observe(id)

    private val _analyzeState = MutableStateFlow<AnalyzeUiState>(AnalyzeUiState.Idle)
    val analyzeState: StateFlow<AnalyzeUiState> = _analyzeState

    fun analyze(url: String) {
        _analyzeState.value = AnalyzeUiState.Loading
        viewModelScope.launch {
            when (val result = repo.analyze(url)) {
                is AnalyzeResult.Success -> _analyzeState.value = AnalyzeUiState.Success(result)
                is AnalyzeResult.Error -> _analyzeState.value = AnalyzeUiState.Error(result.message)
            }
        }
    }

    fun resetAnalyze() {
        _analyzeState.value = AnalyzeUiState.Idle
    }

    fun addDownload(
        url: String,
        fileName: String,
        totalBytes: Long,
        supportsRange: Boolean,
        mimeType: String?,
        cycleLimitBytes: Long,
        cycleIntervalMillis: Long
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            dir?.mkdirs()
            val safeFileName = FileUtils.sanitizeFileName(fileName)
            val destination = File(dir, safeFileName).absolutePath
            val now = System.currentTimeMillis()
            val entity = DownloadEntity(
                url = url,
                fileName = safeFileName,
                destinationPath = destination,
                totalBytes = totalBytes,
                downloadedBytes = 0L,
                cycleLimitBytes = cycleLimitBytes,
                cycleUsedBytes = 0L,
                cycleIntervalMillis = cycleIntervalMillis,
                nextCycleAtMillis = 0L,
                status = DownloadStatus.QUEUED,
                supportsRange = supportsRange,
                mimeType = mimeType,
                errorMessage = null,
                createdAt = now,
                updatedAt = now
            )
            val id = repo.insert(entity)
            DownloadService.start(context, id)
        }
    }

    fun pause(d: DownloadEntity) = viewModelScope.launch {
        val context = getApplication<Application>()
        when (d.status) {
            DownloadStatus.DOWNLOADING -> DownloadService.requestPause(d.id)
            DownloadStatus.WAITING_NEXT_CYCLE, DownloadStatus.QUEUED, DownloadStatus.RETRYING -> {
                Scheduler.cancel(context, d.id)
                repo.update(d.copy(status = DownloadStatus.PAUSED_MANUAL, updatedAt = System.currentTimeMillis()))
            }
            else -> {}
        }
    }

    fun resume(d: DownloadEntity) = viewModelScope.launch {
        val context = getApplication<Application>()
        when (d.status) {
            DownloadStatus.PAUSED_MANUAL, DownloadStatus.FAILED -> {
                repo.update(
                    d.copy(status = DownloadStatus.QUEUED, errorMessage = null, retryCount = 0, updatedAt = System.currentTimeMillis())
                )
                DownloadService.start(context, d.id)
            }
            DownloadStatus.WAITING_NEXT_CYCLE -> {
                Scheduler.cancel(context, d.id)
                DownloadService.start(context, d.id)
            }
            else -> {}
        }
    }

    fun cancel(d: DownloadEntity) = viewModelScope.launch {
        val context = getApplication<Application>()
        DownloadService.requestPause(d.id)
        Scheduler.cancel(context, d.id)
        File(d.destinationPath + ".part").delete()
        repo.update(d.copy(status = DownloadStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
    }

    fun delete(d: DownloadEntity) = viewModelScope.launch {
        val context = getApplication<Application>()
        DownloadService.requestPause(d.id)
        Scheduler.cancel(context, d.id)
        File(d.destinationPath + ".part").delete()
        File(d.destinationPath).delete()
        repo.delete(d)
    }
}