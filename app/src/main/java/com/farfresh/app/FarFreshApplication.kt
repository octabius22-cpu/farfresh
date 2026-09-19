package com.farfresh.app

import android.app.Application
import com.farfresh.app.data.local.AppDatabase

class FarFreshApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FarFreshApplication
            private set
    }
}
