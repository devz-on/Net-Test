package com.example.nettest.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileDescriptor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MyVpnService : VpnService() {
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private var tunInterface: ParcelFileDescriptor? = null
    private val packetController = PacketController(
        holdDurationMs = PACKET_HOLD_MS,
        maxBufferedPackets = MAX_BUFFERED_PACKETS,
        fastSequentialDelayMs = FAST_BURST_DELAY_MS
    ) { _ ->
        // Placeholder sink: packets are buffered and released without forwarding.
    }

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createForegroundNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun startVpn() {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }

        packetController.start()

        tunInterface = Builder()
            .setSession("NetTest VPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .establish()

        val tunDescriptor = tunInterface
        if (tunDescriptor == null) {
            isRunning.set(false)
            stopSelf()
            return
        }

        setNonBlocking(tunDescriptor)

        executor.execute {
            val inputStream = FileInputStream(tunDescriptor.fileDescriptor)
            try {
                val buffer = ByteArray(MTU)
                while (isRunning.get()) {
                    val length = tryRead(inputStream, buffer)
                    if (length > 0) {
                        // Buffer packets without inspecting or forwarding payloads.
                        packetController.enqueuePacket(buffer.copyOf(length))
                    } else {
                        sleepBriefly()
                    }
                }
            } finally {
                inputStream.close()
            }
        }
    }

    private fun stopVpn() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }
        tunInterface?.close()
        tunInterface = null
        packetController.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createForegroundNotification(): Notification {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NetTest VPN")
            .setContentText("VPN is running locally")
            .setSmallIcon(android.R.drawable.stat_sys_vpn_ic)
            .setOngoing(true)
            .build()
    }

    private fun setNonBlocking(descriptor: ParcelFileDescriptor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val osClass = Class.forName("android.system.Os")
                val setBlocking = osClass.getMethod(
                    "setBlocking",
                    FileDescriptor::class.java,
                    Boolean::class.javaPrimitiveType
                )
                setBlocking.invoke(null, descriptor.fileDescriptor, false)
            } catch (exception: Exception) {
                // Best-effort: non-blocking configuration is not available on all API levels.
            }
        }
    }

    private fun tryRead(inputStream: FileInputStream, buffer: ByteArray): Int {
        return try {
            inputStream.read(buffer)
        } catch (exception: Exception) {
            0
        }
    }

    private fun sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(IDLE_SLEEP_MS)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        const val ACTION_START = "com.example.nettest.vpn.START"
        const val ACTION_STOP = "com.example.nettest.vpn.STOP"
        private const val NOTIFICATION_CHANNEL_ID = "vpn_service"
        private const val NOTIFICATION_ID = 1001
        private const val MTU = 1500
        private const val MAX_BUFFERED_PACKETS = 256
        private const val PACKET_HOLD_MS = 250L
        private const val FAST_BURST_DELAY_MS = 10L
        private const val IDLE_SLEEP_MS = 50L
    }
}
