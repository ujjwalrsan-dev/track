package com.uzxsan.dev.track.money.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/** A money account (cash, bank, card...). Amounts everywhere are in minor units (paise, cents). */
@Entity(tableName = "money_accounts")
data class MoneyAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val openingBalance: Long = 0,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class TransactionType { Expense, Income, Transfer }

/**
 * Income lands in [accountId]; an expense leaves [accountId]; a transfer moves [amount] from
 * [accountId] to [toAccountId]. [amount] is always positive.
 */
@Entity(
    tableName = "money_transactions",
    foreignKeys = [
        ForeignKey(MoneyAccount::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MoneyAccount::class, ["id"], ["toAccountId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("accountId"), Index("toAccountId"), Index("date")],
)
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val category: String = "",
    val note: String = "",
    val date: Long,
    val timeMinutes: Int,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val localTime: LocalTime get() = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
}

/** One of the user's three named grand totals. */
@Entity(tableName = "money_totals")
data class GrandTotal(
    @PrimaryKey val id: Long,
    val name: String,
)

/** Membership: account [accountId] counts towards grand total [totalId]. */
@Entity(
    tableName = "money_total_accounts",
    primaryKeys = ["totalId", "accountId"],
    foreignKeys = [
        ForeignKey(GrandTotal::class, ["id"], ["totalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MoneyAccount::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("accountId")],
)
data class GrandTotalAccount(val totalId: Long, val accountId: Long)
