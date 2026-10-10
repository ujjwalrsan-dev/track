package com.uzxsan.dev.track.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.uzxsan.dev.track.dsa.data.DsaDao
import com.uzxsan.dev.track.dsa.data.DsaProblem
import com.uzxsan.dev.track.money.data.GrandTotal
import com.uzxsan.dev.track.money.data.GrandTotalAccount
import com.uzxsan.dev.track.money.data.MoneyAccount
import com.uzxsan.dev.track.money.data.MoneyDao
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.todo.data.TodoDao
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.todo.data.TodoTask

/**
 * Single database for every tracker; each tracker owns its own tables and DAO.
 * Adding a tracker's tables bumps [version] and needs a migration in [Migrations].
 */
@Database(
    entities = [
        DsaProblem::class,
        TodoTask::class,
        TodoStage::class,
        MoneyAccount::class,
        MoneyTransaction::class,
        GrandTotal::class,
        GrandTotalAccount::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class TrackDatabase : RoomDatabase() {
    abstract fun dsaDao(): DsaDao
    abstract fun todoDao(): TodoDao
    abstract fun moneyDao(): MoneyDao
}
