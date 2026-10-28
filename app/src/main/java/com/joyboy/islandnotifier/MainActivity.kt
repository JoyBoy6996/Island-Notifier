package com.joyboy.islandnotifier

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        status = TextView(this).apply { textSize = 18f }

        val btnStart = Button(this).apply {
            text = "Start"
            setOnClickListener { onStartClicked() }
        }
        val btnStop = Button(this).apply {
            text = "Stop"
            setOnClickListener {
                setEnabled(false)
                stopService(Intent(this@MainActivity, IslandService::class.java))
                refresh()
            }
        }
        val btnTest = Button(this).apply {
            text = "Send test to island"
            setOnClickListener {
                try {
                    val i = Intent(this@MainActivity, IslandService::class.java)
                        .setAction(IslandService.ACTION_TEST)
                    startForegroundService(i)
                } catch (e: Exception) {
                    // blocked or failed
                }
            }
        }
        val btnBattery = Button(this).apply {
            text = "Battery settings"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        root.addView(status)
        root.addView(btnStart)
        root.addView(btnStop)
        root.addView(btnTest)
        root.addView(btnBattery)
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun prefs() = getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private fun setEnabled(on: Boolean) {
        prefs().edit().putBoolean("enabled", on).apply()
    }

    private fun refresh() {
        status.text = if (prefs().getBoolean("enabled", false))
            "Status: ON (auto-starts after reboot)"
        else
            "Status: OFF"
    }

    private fun missingPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) list.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) list.add(Manifest.permission.BLUETOOTH_CONNECT)
        return list.toTypedArray()
    }

    private fun onStartClicked() {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            requestPermissions(missing, 1)
        } else {
            startIsland()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startIsland() // start anyway; missing permissions only reduce features
    }

    private fun startIsland() {
        setEnabled(true)
        try {
            startForegroundService(Intent(this, IslandService::class.java))
        } catch (e: Exception) {
            // blocked by system
        }
        refresh()
    }
}
