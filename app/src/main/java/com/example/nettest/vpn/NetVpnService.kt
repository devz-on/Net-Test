package com.example.nettest.vpn

import android.content.Intent
import android.net.VpnService

class NetVpnService : VpnService() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Initialize VPN interface and foreground notification.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Start VPN session once prepared.
        return START_STICKY
    }
}
