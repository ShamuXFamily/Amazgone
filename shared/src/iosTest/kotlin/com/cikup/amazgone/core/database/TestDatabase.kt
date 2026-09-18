package com.cikup.amazgone.core.database

import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun inMemoryDatabase(): AppDatabase =
    AppDatabase.build(Room.inMemoryDatabaseBuilder<AppDatabase>(), Dispatchers.IO)
