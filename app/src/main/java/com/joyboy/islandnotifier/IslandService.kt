package com.joyboy.islandnotifier

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class IslandService : Service() {

    companion object {
        const val ACTION_TEST = "com.joyboy.islandnotifier.TEST"
        private const val CH_SERVICE = "service"
        private const val CH_EVENTS = "events"
        private const val ID_SERVICE = 1
        private const val ID_EVENT = 2
        private const val SHOW_MS = 4000L
        private const val KIND_BT = 0
        private const val KIND_WIFI = 1
    }

    private lateinit var nm: NotificationManager
    private lateinit var session: MediaSession
    private lateinit var cm: ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private val startedAt = SystemClock.elapsedRealtime()
    private val wifiNetworks = mutableSetOf<Network>()
    private var lastTitle = ""
    private var lastAt = 0L
    private var isDestroyed = false

    private val deactivate = Runnable {
        if (!isDestroyed) {
            session.isActive = false
        }
    }

    // ---------- Bluetooth ----------
    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (isDestroyed) return
            val name = try {
                btDevice(intent)?.name
            } catch (e: Exception) {
                null
            } ?: "Device"
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED ->
                    showEvent(KIND_BT, "Bluetooth connected", name)
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    showEvent(KIND_BT, "Bluetooth disconnected", name)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun btDevice(intent: Intent): BluetoothDevice? {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Wi-Fi ----------
    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handler.post {
                if (isDestroyed) return@post
                val wasEmpty = wifiNetworks.isEmpty()
                wifiNetworks.add(network)
                // ignore the "already connected" callback fired right after start
                val settled = SystemClock.elapsedRealtime() - startedAt > 2000
                if (wasEmpty && settled) {
                    showEvent(KIND_WIFI, "Wi-Fi connected", "Network available")
                }
            }
        }

        override fun onLost(network: Network) {
            handler.post {
                if (isDestroyed) return@post
                wifiNetworks.remove(network)
                if (wifiNetworks.isEmpty()) {
                    showEvent(KIND_WIFI, "Wi-Fi disconnected", "Network lost")
                }
            }
        }
    }

    // ---------- Lifecycle ----------
    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        cm = getSystemService(ConnectivityManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Service status", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_EVENTS, "Island events", NotificationManager.IMPORTANCE_HIGH)
        )

        session = MediaSession(this, "IslandNotifier")

        startServiceForeground()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(btReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(btReceiver, filter)
        }

        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(req, wifiCallback)
    }

    private fun startServiceForeground() {
        val n = Notification.Builder(this, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle("Island Notifier is running")
            .setContentText("Listening for Bluetooth and Wi-Fi")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_SERVICE, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(ID_SERVICE, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServiceForeground()
        if (intent?.action == ACTION_TEST) {
            showEvent(KIND_BT, "Island test", "It works")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isDestroyed = true
        try { unregisterReceiver(btReceiver) } catch (e: Exception) { }
        try { cm.unregisterNetworkCallback(wifiCallback) } catch (e: Exception) { }
        handler.removeCallbacksAndMessages(null)
        try { session.release() } catch (e: Exception) { }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- Show event on the island ----------
    private fun showEvent(kind: Int, title: String, text: String) {
        if (isDestroyed) return
        val now = SystemClock.elapsedRealtime()
        if (title == lastTitle && now - lastAt < 3000) return // de-duplicate
        lastTitle = title
        lastAt = now

        val art = makeArt(kind)
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, text)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
                .build()
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(0L)
                .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                .build()
        )
        session.isActive = true

        val icon = if (kind == KIND_BT) R.drawable.ic_bluetooth else R.drawable.ic_wifi
        val n = Notification.Builder(this, CH_EVENTS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(art)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken))
            .setTimeoutAfter(SHOW_MS)
            .build()
        nm.notify(ID_EVENT, n)

        // stop being an "active media app" once the popup is gone
        handler.removeCallbacks(deactivate)
        handler.postDelayed(deactivate, SHOW_MS)
    }

    private fun makeArt(kind: Int): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (kind == KIND_BT) Color.parseColor("#1A73E8") else Color.parseColor("#0F9D58")
        }
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 56f, 56f, paint)
        val res = if (kind == KIND_BT) R.drawable.ic_bluetooth else R.drawable.ic_wifi
        val d = getDrawable(res)?.mutate() ?: return bmp
        d.setTint(Color.WHITE)
        d.setBounds(48, 48, size - 48, size - 48)
        d.draw(canvas)
        return bmp
    }
}

