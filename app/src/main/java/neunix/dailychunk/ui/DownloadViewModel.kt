package neunix.dailychunk.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.AppPreferences
import neunix.dailychunk.data.DownloadEntity
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.FilesRepository
import neunix.dailychunk.data.IntervalUnit
import neunix.dailychunk.data.ManagedFile
import neunix.dailychunk.data.QueueManager
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
    private val prefsRepo get() = AppContainer.preferencesRepository
    private val filesRepo by lazy { FilesRepository(application) }

    val downloads: StateFlow<List<DownloadEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadEntity>> = downloads
        .combine(downloads) { list, _ -> list }
        .let { flow ->
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }

    val historyDownloads: StateFlow<List<DownloadEntity>> = downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<AppPreferences> = AppContainer.prefsState

    private val _files = MutableStateFlow<List<ManagedFile>>(emptyList())
    val files: StateFlow<List<ManagedFile>> = _files

    fun observe(id: Long): Flow<DownloadEntity?> = repo.observe(id)

    init {
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            _files.value = filesRepo.listFiles()
        }
    }

    fun deleteFile(file: ManagedFile) {
        viewModelScope.launch {
            filesRepo.delete(file)
            refreshFiles()
        }
    }

    fun renameFile(file: ManagedFile, newName: String) {
        viewModelScope.launch {
            filesRepo.rename(file, newName)
            refreshFiles()
        }
    }

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

    /**
     * cycleAmountMb supports fractional values (e.g. 3.5 MB per cycle).
     * intervalValue + intervalUnit lets the user pick minutes or hours.
     */
    fun addDownload(
        url: String,
        fileName: String,
        totalBytes: Long,
        supportsRange: Boolean,
        mimeType: String?,
        cycleAmountMb: Float,
        intervalValue: Long,
        intervalUnit: IntervalUnit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val workDir = File(context.filesDir, "in_progress")
            workDir.mkdirs()
            val safeFileName = FileUtils.sanitizeFileName(fileName)
            val destination = File(workDir, safeFileName).absolutePath
            val now = System.currentTimeMillis()

            val cycleLimitBytes = (cycleAmountMb * 1024f * 1024f).toLong().coerceAtLeast(1L)
            val intervalMillis = when (intervalUnit) {
                IntervalUnit.MINUTES -> intervalValue * 60_000L
                IntervalUnit.HOURS -> intervalValue * 3_600_000L
            }.coerceAtLeast(60_000L)

            val entity = DownloadEntity(
                url = url,
                fileName = safeFileName,
                destinationPath = destination,
                totalBytes = totalBytes,
                downloadedBytes = 0L,
                cycleLimitBytes = cycleLimitBytes,
                cycleUsedBytes = 0L,
                cycleIntervalMillis = intervalMillis,
                nextCycleAtMillis = 0L,
                status = DownloadStatus.QUEUED,
                supportsRange = supportsRange,
                mimeType = mimeType,
                errorMessage = null,
                createdAt = now,
                updatedAt = now
            )
            repo.insert(entity)
            QueueManager.tryStartNext(context)
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
                QueueManager.tryStartNext(context)
            }
            DownloadStatus.WAITING_NEXT_CYCLE -> {
                Scheduler.cancel(context, d.id)
                repo.update(d.copy(status = DownloadStatus.QUEUED, updatedAt = System.currentTimeMillis()))
                QueueManager.tryStartNext(context)
            }
            else -> {}
        }
    }

    fun cancel(d: DownloadEntity) = viewModelScope.launch {
        val context = getApplicat