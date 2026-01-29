package com.example.nettest.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

class OverlayService : Service() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Build overlay view and add it to WindowManager.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Manage overlay display lifecycle.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
