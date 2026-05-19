package io.github.yesterdaylz.kxqiandao

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.yesterdaylz.kxqiandao.data.local.PreferencesManager
import io.github.yesterdaylz.kxqiandao.util.Constants

class KexieApplication : Application() {
    lateinit var preferencesManager: PreferencesManager
        private set
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            Constants.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于保持定时签退功能在后台正常运行"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    companion object {
        lateinit var instance: KexieApplication
            private set
    }
}
