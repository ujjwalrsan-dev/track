package com.uzxsan.dev.track.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Opens a hand-built v3 database with Room so the v3→v4 migration runs and the schema is validated. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "migration-test.db"

    @Before
    fun clean() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun migrate3To4KeepsTasksAndAddsEmptyDetails() = runBlocking {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            V3_SCHEMA.forEach(db::execSQL)
            db.execSQL("INSERT INTO todo_tasks (id, title, date, timeMinutes, createdAt) VALUES (1, 'Old task', 20000, NULL, 0)")
            db.execSQL("INSERT INTO todo_stages (id, taskId, position, name, done, completedAt) VALUES (1, 1, 0, 'Done', 0, NULL)")
            db.version = 3
        }

        val room = Room.databaseBuilder(context, TrackDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        try {
            // Room validates the migrated schema against the entities when it opens the database.
            val item = room.todoDao().get(1)!!
            assertEquals("Old task", item.task.title)
            assertEquals(20000L, item.task.date)
            assertEquals("", item.task.notes)
            assertEquals("", item.task.links)
            assertNull(item.task.originalDate)
            assertEquals(listOf("Done"), item.stages.map { it.name })
        } finally {
            room.close()
        }
    }

    private companion object {
        /** The schema as of v3, before notes, links and originalDate were added to todo_tasks. */
        val V3_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `dsa_problems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `platform` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `topic` TEXT NOT NULL, `solvedOn` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_dsa_problems_solvedOn` ON `dsa_problems` (`solvedOn`)",
            "CREATE TABLE IF NOT EXISTS `todo_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `date` INTEGER NOT NULL, `timeMinutes` INTEGER, `createdAt` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_todo_tasks_date` ON `todo_tasks` (`date`)",
            "CREATE TABLE IF NOT EXISTS `todo_stages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `done` INTEGER NOT NULL, `completedAt` INTEGER, FOREIGN KEY(`taskId`) REFERENCES `todo_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_todo_stages_taskId` ON `todo_stages` (`taskId`)",
            "CREATE TABLE IF NOT EXISTS `money_accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `openingBalance` INTEGER NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `money_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `toAccountId` INTEGER, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `date` INTEGER NOT NULL, `timeMinutes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`toAccountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_money_transactions_accountId` ON `money_transactions` (`accountId`)",
            "CREATE INDEX IF NOT EXISTS `index_money_transactions_toAccountId` ON `money_transactions` (`toAccountId`)",
            "CREATE INDEX IF NOT EXISTS `index_money_transactions_date` ON `money_transactions` (`date`)",
            "CREATE TABLE IF NOT EXISTS `money_totals` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `money_total_accounts` (`totalId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, PRIMARY KEY(`totalId`, `accountId`), FOREIGN KEY(`totalId`) REFERENCES `money_totals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`accountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_money_total_accounts_accountId` ON `money_total_accounts` (`accountId`)",
        )
    }
}
