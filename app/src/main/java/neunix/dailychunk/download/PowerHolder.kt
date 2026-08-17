package neunix.dailychunk.download

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reference-counted partial wake lock + high-perf Wi-Fi lock, held only while
 * at least one download is actively transferring bytes. This is the standard
 * fix for background downloads that silently stall: a foreground service
 * alone does not guarantee the CPU/radio stay awake on every OEM, especially
 * during long transfers with the screen off.
 *
 * A safety timeout is set on the wake lock itself so a bug can never hold it
 * forever and drain the battery.
 */
object PowerHolder {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val refCount = AtomicInteger(0)

    private const val SAFETY_TIMEOUT_MS = 6 * 60 * 60 * 1000L // 6 hours

    @Synchronized
    fun acquire(context: Context) {
        if (refCount.getAndIncrement() == 0) {
            val appContext = context.applicationContext
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DailyChunk:downloadWakeLock").apply {
                setReferenceCounted(false)
                acquire(SAFETY_TIMEOUT_MS)
            }
            val wm = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DailyChunk:wifiLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    @Synchronized
    fun release() {
        val remaining = (refCount.get() - 1).coerceAtLeast(0)
        refCount.set(remaining)
        if (remaining == 0) {
            wakeLock?.let { if (it.isHeld) it.release() }
            wifiLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            wifiLock = null
        }
    }
}