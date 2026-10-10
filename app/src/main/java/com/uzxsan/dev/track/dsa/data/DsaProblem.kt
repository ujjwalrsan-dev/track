package com.uzxsan.dev.track.dsa.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Platform(val label: String) {
    LeetCode("LeetCode"),
    Codeforces("Codeforces"),
    CodeChef("CodeChef"),
    GeeksForGeeks("GFG"),
    HackerRank("HackerRank"),
    AtCoder("AtCoder"),
    Other("Other"),
}

enum class Difficulty(val label: String) {
    Easy("Easy"),
    Medium("Medium"),
    Hard("Hard"),
}

/** One logged submission. [solvedOn] is stored as an epoch day. */
@Entity(tableName = "dsa_problems", indices = [Index("solvedOn")])
data class DsaProblem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val platform: Platform,
    val difficulty: Difficulty,
    val topic: String,
    val solvedOn: Long,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val solvedDate: LocalDate get() = LocalDate.ofEpochDay(solvedOn)
}
