package neunix.dailychunk.data

import android.content.Context
import androidx.room.Room
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import neunix.dailychunk.download.DownloadEngine
import okhttp3.OkHttpClient

object AppContainer {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: DownloadRepository
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var prefsState: StateFlow<AppPreferences>
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            preferencesRepository = UserPreferencesRepository(appContext)
            prefsState = preferencesRepository.preferences
                .stateIn(appScope, SharingStarted.Eagerly, AppPreferences())
            initialized = true
        }
    }
}