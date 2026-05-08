package io.github.yesterdaylz.kxqiandao

import android.app.Application

class KexieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: KexieApplication
            private set
    }
}