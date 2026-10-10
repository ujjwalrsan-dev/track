package com.uzxsan.dev.track.data

import android.content.Context
import androidx.room.Room
import com.uzxsan.dev.track.dsa.data.DsaRepository
import com.uzxsan.dev.track.money.data.MoneyRepository
import com.uzxsan.dev.track.todo.data.TodoRepository

/** Manual dependency container shared across all trackers. */
class AppContainer(context: Context) {
    private val database: TrackDatabase = Room.databaseBuilder(
        context.applicationContext,
        TrackDatabase::class.java,
        "track.db",
    ).addMigrations(*Migrations.ALL).build()

    val dsaRepository: DsaRepository by lazy { DsaRepository(database.dsaDao()) }
    val todoRepository: TodoRepository by lazy { TodoRepository(database.todoDao()) }
    val moneyRepository: MoneyRepository by lazy { MoneyRepository(database.moneyDao()) }
}
