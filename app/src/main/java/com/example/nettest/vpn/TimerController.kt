package com.example.nettest.vpn

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reliable countdown timer that invokes [onExpired] when time elapses.
 *
 * Uses elapsed realtime for accurate timing and supports safe cancellation to
 * avoid memory leaks when the VPN service stops.
 */
class TimerController(
    private val onExpired: () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var activeJob: Job? = null

    fun start(durationMs: Long) {
        stop()
        val startAt = SystemClock.elapsedRealtime()
        activeJob = scope.launch {
            var remainingMs = durationMs
            while (remainingMs > 0) {
                delay(remainingMs.coerceAtMost(TICK_GRANULARITY_MS))
                remainingMs = durationMs - (SystemClock.elapsedRealtime() - startAt)
            }
            onExpired()
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
    }

    companion object {
        private const val TICK_GRANULARITY_MS = 250L
    }
}
