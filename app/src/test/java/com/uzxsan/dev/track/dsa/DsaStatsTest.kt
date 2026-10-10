package com.uzxsan.dev.track.dsa

import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.dsa.data.DsaProblem
import com.uzxsan.dev.track.dsa.data.Platform
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DsaStatsTest {
    private val today = LocalDate.of(2026, 10, 4)

    private fun solved(daysAgo: Long, difficulty: Difficulty = Difficulty.Medium) = DsaProblem(
        title = "p",
        platform = Platform.LeetCode,
        difficulty = difficulty,
        topic = "",
        solvedOn = today.minusDays(daysAgo).toEpochDay(),
    )

    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(DsaStats(), computeStats(emptyList(), today))
    }

    @Test
    fun streakIncludesToday() {
        val stats = computeStats(listOf(solved(0), solved(0), solved(1), solved(2), solved(4)), today)
        assertEquals(5, stats.total)
        assertEquals(4, stats.activeDays)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        val stats = computeStats(listOf(solved(1), solved(2)), today)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun streakBreaksAfterMissedDay() {
        val stats = computeStats(listOf(solved(2), solved(3), solved(4), solved(5)), today)
        assertEquals(0, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun countsByDifficulty() {
        val stats = computeStats(listOf(solved(0, Difficulty.Easy), solved(0, Difficulty.Hard), solved(1, Difficulty.Hard)), today)
        assertEquals(mapOf(Difficulty.Easy to 1, Difficulty.Hard to 2), stats.byDifficulty)
    }
}
