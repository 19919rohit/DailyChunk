package neunix.dailychunk.download

import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * One shared client for the whole app. Reusing it (instead of building a new
 * OkHttpClient per download cycle, as before) keeps TCP/TLS connections warm
 * across cycles and avoids the handshake cost on every resume — a real
 * contributor to slow starts on flaky mobile networks.
 */
object NetworkClientProvider {
    val client: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 16
            maxRequestsPerHost = 8
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}