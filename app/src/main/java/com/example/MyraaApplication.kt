package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.security.SecureKeyManager

class MyraaApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var secureKeyManager: SecureKeyManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        secureKeyManager = SecureKeyManager(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "MYRAA Background Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while MYRAA assistant is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_BACKGROUND_SERVICE = "myraa_background_channel"
        lateinit var instance: MyraaApplication
            private set
    }
}
