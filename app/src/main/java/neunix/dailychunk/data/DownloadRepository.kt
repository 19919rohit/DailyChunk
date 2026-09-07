package neunix.dailychunk.data

import kotlinx.coroutines.flow.Flow
import neunix.dailychunk.download.AnalyzeResult
import neunix.dailychunk.download.DownloadEngine

class DownloadRepository(
    private val dao: DownloadDao,
    private val engine: DownloadEngine
) {
    fun observeAll(): Flow<List<DownloadEntity>> = dao.getAll()
    fun observe(id: Long): Flow<DownloadEntity?> = dao.getByIdFlow(id)
    suspend fun getById(id: Long): DownloadEntity? = dao.getById(id)
    suspend fun insert(download: DownloadEntity): Long = dao.insert(download)
    suspend fun update(download: DownloadEntity) = dao.update(download)
    suspend fun delete(download: DownloadEntity) = dao.delete(download)
    suspend fun analyze(url: String): AnalyzeResult = engine.analyze(url)
}