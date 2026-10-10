package com.uzxsan.dev.track.dsa

import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.dsa.data.DsaProblem
import java.time.LocalDate

data class DsaStats(
    val total: Int = 0,
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val byDifficulty: Map<Difficulty, Int> = emptyMap(),
)

/**
 * The current streak stays alive through [today] even if nothing has been logged yet today,
 * as long as yesterday was active.
 */
fun computeStats(problems: List<DsaProblem>, today: LocalDate): DsaStats {
    val days = problems.mapTo(sortedSetOf()) { it.solvedOn }

    var longest = 0
    var run = 0
    var previous: Long? = null
    for (day in days) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }

    val todayEpoch = today.toEpochDay()
    var cursor = if (todayEpoch in days) todayEpoch else todayEpoch - 1
    var current = 0
    while (cursor in days) {
        current++
        cursor--
    }

    return DsaStats(
        total = problems.size,
        activeDays = days.size,
        currentStreak = current,
        longestStreak = longest,
        byDifficulty = problems.groupingBy { it.difficulty }.eachCount(),
    )
}

fun countsByDay(problems: List<DsaProblem>): Map<LocalDate, Int> =
    problems.groupingBy { it.solvedDate }.eachCount()
