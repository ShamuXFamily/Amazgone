package com.cikup.amazgone

import android.app.Application
import com.cikup.amazgone.core.di.initKoinAndroid

class AmazgoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
