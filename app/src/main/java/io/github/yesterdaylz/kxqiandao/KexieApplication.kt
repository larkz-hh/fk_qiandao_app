package io.github.yesterdaylz.kxqiandao

import android.app.Application
import io.github.yesterdaylz.kxqiandao.data.local.PreferencesManager

class KexieApplication : Application() {
    lateinit var preferencesManager: PreferencesManager
        private set
    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }
    companion object {
        lateinit var instance: KexieApplication
            private set
    }
}