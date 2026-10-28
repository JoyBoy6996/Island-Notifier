package com.joyboy.islandnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val enabled = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        if (enabled) {
            try {
                context.startForegroundService(Intent(context, IslandService::class.java))
            } catch (e: Exception) {
                // blocked by the system; open the app once to restart
            }
        }
    }
}
