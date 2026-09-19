package com.cikup.amazgone.notifications.di

import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.notifications.data.NotificationCoordinator
import com.cikup.amazgone.notifications.data.NotificationRepositoryImpl
import com.cikup.amazgone.notifications.domain.repository.NotificationRenderer
import com.cikup.amazgone.notifications.domain.repository.NotificationRepository
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import com.cikup.amazgone.notifications.domain.usecase.MarkAllNotificationsReadUseCase
import com.cikup.amazgone.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cikup.amazgone.notifications.domain.usecase.ObserveInboxUseCase
import com.cikup.amazgone.notifications.domain.usecase.RemoveNotificationUseCase
import com.cikup.amazgone.notifications.presentation.ResourceNotificationRenderer
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

/** Scheduling is skipped where no platform implementation is bound (JVM tests). */
private object NoSystemNotifications : SystemNotifications {
    override fun schedule(id: String, title: String, body: String, atMillis: Long, link: String) = Unit
    override fun cancel(id: String) = Unit
}

val notificationsModule = module {
    single { get<AppDatabase>().notificationDao() }
    single<NotificationRenderer> { ResourceNotificationRenderer() }
    single {
        NotificationRepositoryImpl(get(), get(), get(), getOrNull<SystemNotifications>() ?: NoSystemNotifications, get(), get())
    } binds arrayOf(NotificationRepository::class)
    singleOf(::NotificationCoordinator) bind StartupTask::class
    factoryOf(::ObserveInboxUseCase)
    factoryOf(::MarkNotificationReadUseCase)
    factoryOf(::MarkAllNotificationsReadUseCase)
    factoryOf(::RemoveNotificationUseCase)
}
