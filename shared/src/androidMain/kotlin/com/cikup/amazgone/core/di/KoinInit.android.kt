package com.cikup.amazgone.core.di

import android.content.Context
import com.cikup.amazgone.core.sync.SyncWorker
import org.koin.android.ext.koin.androidContext

fun initKoinAndroid(context: Context) {
    initKoin { androidContext(context) }
    SyncWorker.schedule(context)
}
