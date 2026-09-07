package neunix.dailychunk.data

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED_MANUAL,
    WAITING_NEXT_CYCLE,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED
}