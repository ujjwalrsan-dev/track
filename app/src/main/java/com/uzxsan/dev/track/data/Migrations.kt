package com.uzxsan.dev.track.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schema migrations; SQL matches what Room generates for the entities, so validation passes. */
object Migrations {
    /** v2 adds the To Do tracker's tables. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `todo_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, `date` INTEGER NOT NULL, `timeMinutes` INTEGER, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_tasks_date` ON `todo_tasks` (`date`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `todo_stages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`taskId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                    "`done` INTEGER NOT NULL, `completedAt` INTEGER, FOREIGN KEY(`taskId`) " +
                    "REFERENCES `todo_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_stages_taskId` ON `todo_stages` (`taskId`)")
        }
    }

    /** v3 adds the Budget tracker's tables (accounts, transactions, grand totals). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `money_accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `openingBalance` INTEGER NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `money_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `toAccountId` INTEGER, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `date` INTEGER NOT NULL, `timeMinutes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`toAccountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_transactions_accountId` ON `money_transactions` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_transactions_toAccountId` ON `money_transactions` (`toAccountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_transactions_date` ON `money_transactions` (`date`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `money_totals` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `money_total_accounts` (`totalId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, PRIMARY KEY(`totalId`, `accountId`), FOREIGN KEY(`totalId`) REFERENCES `money_totals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`accountId`) REFERENCES `money_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_money_total_accounts_accountId` ON `money_total_accounts` (`accountId`)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
