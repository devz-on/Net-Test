package com.example.nettest.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ForegroundNetworkService : Service() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Create notification channel and start foreground notification.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Start foreground work related to networking tasks.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
