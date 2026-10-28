package com.joyboy.islandnotifier

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val channelId = "island_test"
    private var session: MediaSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        createChannel()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val info = TextView(this).apply {
            text = "Island test.\nTap a button and watch the Origin Island."
            textSize = 16f
        }
        val btnNormal = Button(this).apply {
            text = "Test 1: normal notification"
            setOnClickListener { postNormal() }
        }
        val btnMedia = Button(this).apply {
            text = "Test 2: media-style notification"
            setOnClickListener { postMedia() }
        }
        root.addView(info)
        root.addView(btnNormal)
        root.addView(btnMedia)
        setContentView(root)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            channelId, "Island test", NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(ch)
    }

    private fun postNormal() {
        val n = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bluetooth connected")
            .setContentText("Test device")
            .setTimeoutAfter(4000)
            .build()
        getSystemService(NotificationManager::class.java).notify(1, n)
    }

    private fun postMedia() {
        val s = session ?: MediaSession(this, "IslandTest").also { session = it }
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Bluetooth connected")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Test device")
                .build()
        )
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build()
        )
        s.isActive = true

        val n = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bluetooth connected")
            .setContentText("Test device")
            .setStyle(Notification.MediaStyle().setMediaSession(s.sessionToken))
            .setTimeoutAfter(4000)
            .build()
        getSystemService(NotificationManager::class.java).notify(2, n)
    }
}
