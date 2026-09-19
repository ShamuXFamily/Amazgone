package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.analytics.AnalyticsSink
import org.koin.dsl.module

/**
 * Swift entry point (`KoinInit_iosKt.startAmazgone(analytics:)`). ObjC export drops Kotlin default
 * args, so Swift passes nil when Firebase isn't configured; events are then dropped.
 */
fun startAmazgone(analytics: AnalyticsSink?) = initKoin {
    if (analytics != null) modules(module { single<AnalyticsSink> { analytics } })
}
