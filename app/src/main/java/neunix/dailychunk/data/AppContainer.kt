package neunix.dailychunk.data

import android.content.Context
import androidx.room.Room
import java.util.concurrent.TimeUnit
import neunix.dailychunk.download.DownloadEngine
import okhttp3.OkHttpClient

object AppContainer {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: DownloadRepository
        private set

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            database = Room.databaseBuilder(appContext, AppDatabase::class.java, "dailychunk.db")
                .fallbackToDestructiveMigration()
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val engine = DownloadEngine(client)
            repository = DownloadRepository(database.downloadDao(), engine)
            initialized = true
        }
    }
}