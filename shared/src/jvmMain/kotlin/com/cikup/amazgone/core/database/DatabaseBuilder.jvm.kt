package com.cikup.amazgone.core.database

import androidx.room.Room
import androidx.room.RoomDatabase

/** JVM host (tests/tools only). */
fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(name = "${System.getProperty("java.io.tmpdir")}/amazgone/${AppDatabase.FILE_NAME}")
