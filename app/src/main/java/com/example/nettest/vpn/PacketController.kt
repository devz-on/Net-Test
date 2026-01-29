package com.example.nettest.vpn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe packet buffer with configurable hold time and burst release modes.
 *
 * This controller only stores raw packet bytes and never inspects payloads.
 * Burst sending is delegated to [onBurstRelease], which can later forward packets
 * to a network writer or telemetry sink.
 */
class PacketController(
    private val holdDurationMs: Long,
    private val maxBufferedPackets: Int,
    private val fastSequentialDelayMs: Long,
    private val onBurstRelease: (List<ByteArray>) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val buffer = ArrayDeque<BufferedPacket>()
    @Volatile
    private var isActive = false

    fun start() {
        isActive = true
    }

    fun stop() {
        isActive = false
        scope.coroutineContext.cancelChildren()
        scope.launch {
            clearBuffer()
        }
    }

    fun enqueuePacket(packet: ByteArray) {
        if (!isActive) return
        scope.launch {
            mutex.withLock {
                if (buffer.size >= maxBufferedPackets) {
                    buffer.removeFirst()
                }
                buffer.addLast(BufferedPacket(packet, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Release eligible packets immediately as a single burst.
     *
     * "Instant burst" drains all packets that have been held for at least
     * [holdDurationMs], clears them from the queue, and emits them in one list.
     */
    fun releaseInstantBurst(): Job = scope.launch {
        val ready = drainEligiblePackets()
        if (ready.isNotEmpty()) {
            onBurstRelease(ready)
        }
    }

    /**
     * Release eligible packets sequentially in a fast burst.
     *
     * "Fast sequential burst" drains all packets that have been held for at least
     * [holdDurationMs], clears them from the queue, then emits them in small chunks
     * (one packet per chunk) with a short delay between each emission.
     */
    fun releaseSequentialBurst(): Job = scope.launch {
        val ready = drainEligiblePackets()
        for (packet in ready) {
            onBurstRelease(listOf(packet))
            delay(fastSequentialDelayMs)
        }
    }

    private suspend fun drainEligiblePackets(): List<ByteArray> = mutex.withLock {
        val now = System.currentTimeMillis()
        val readyPackets = ArrayList<ByteArray>()
        val iterator = buffer.iterator()
        while (iterator.hasNext()) {
            val buffered = iterator.next()
            if (now - buffered.enqueuedAtMs >= holdDurationMs) {
                readyPackets.add(buffered.data)
                iterator.remove()
            }
        }
        readyPackets
    }

    private suspend fun clearBuffer() {
        mutex.withLock {
            buffer.clear()
        }
    }

    private data class BufferedPacket(
        val data: ByteArray,
        val enqueuedAtMs: Long
    )
}
