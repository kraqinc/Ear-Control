package com.kraq.earcontrol

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class DynamicIslandService : Service() {

    private var windowManager: WindowManager? = null
    private var islandView: View? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            1001,
            NotificationCompat.Builder(
                this,
                "earcontrol_island",
            )
                .setContentTitle(
                    "EarControl"
                )
                .setContentText(
                    "Find My Earbuds active"
                )
                .setSmallIcon(
                    com.kraq.earcontrol.R.drawable.ic_earcontrol
                )
                .setOngoing(true)
                .setSilent(true)
                .build()
        )

        showIsland()
    }

    private fun showIsland() {
        if (
            Build.VERSION.SDK_INT >= 23 &&
            !Settings.canDrawOverlays(this)
        ) {
            stopSelf()
            return
        }

        windowManager =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        val text =
            TextView(this).apply {
                text =
                    "●  EarControl   •   Finding earbuds"
                setTextColor(Color.WHITE)
                setTextSize(12f)
                setPadding(
                    28,
                    18,
                    28,
                    18,
                )
                setBackgroundColor(
                    Color.BLACK
                )
                alpha = 0.96f
            }

        islandView = text

        val type =
            if (
                Build.VERSION.SDK_INT >= 26
            ) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            )

        params.gravity =
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL

        params.y = 12

        try {
            windowManager?.addView(
                text,
                params,
            )
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        islandView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }

        islandView = null
        windowManager = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as android.app.NotificationManager

            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    "earcontrol_island",
                    "EarControl Dynamic Island",
                    android.app.NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }
}
