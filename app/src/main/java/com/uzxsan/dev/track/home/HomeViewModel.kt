package com.uzxsan.dev.track.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.dsa.computeStats
import com.uzxsan.dev.track.dsa.countsByDay
import com.uzxsan.dev.track.dsa.data.DsaRepository
import com.uzxsan.dev.track.money.MoneyFormat
import com.uzxsan.dev.track.money.data.MoneyRepository
import com.uzxsan.dev.track.money.flowTotals
import com.uzxsan.dev.track.money.grandTotals
import com.uzxsan.dev.track.money.inRange
import com.uzxsan.dev.track.todo.data.TodoRepository
import com.uzxsan.dev.track.todo.summarize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

// [Kotlin] Several small classes can live in one file (Java needs one public class per file).
data class TrackerStat(val value: String, @StringRes val label: Int)

/** At-a-glance summary for a tracker card; [recent] holds daily counts, oldest first. */
data class TrackerSummary(val stats: List<TrackerStat>, val recent: List<Int>)

// [Kotlin] `= emptyMap()` is a DEFAULT ARGUMENT: HomeUiState() with no args is valid.
// In Java you would write a second, overloaded constructor for that.
data class HomeUiState(val summaries: Map<String, TrackerSummary> = emptyMap())

// [Kotlin] The parameter list after the class name IS the constructor (the "primary constructor").
// `: ViewModel()` = Java `extends ViewModel` (the `()` calls the super constructor).
// Java equivalent:
//   public class HomeViewModel extends ViewModel {
//       public HomeViewModel(DsaRepository dsaRepository) { ... }
//   }
class HomeViewModel(
    dsaRepository: DsaRepository,
    todoRepository: TodoRepository,
    moneyRepository: MoneyRepository,
) : ViewModel() {

    // [Kotlin] A StateFlow is like LiveData: it always holds a current value and notifies observers.
    // This is a pipeline (similar to RxJava / Java Streams):
    //   1. Each repository's observeAll() emits its full list from Room every time its table changes.
    //   2. combine(a, b) { x, y -> } merges the two Flows: it re-runs whenever EITHER emits
    //      (like RxJava's combineLatest) and converts both lists into the UI state Home needs.
    //   3. .stateIn(...) turns it into a StateFlow that the screen can observe.
    // [Kotlin] `{ problems, tasks -> ... }` is a lambda with two parameters, like Java's `(a, b) -> { ... }`.
    // The LAST expression in the lambda is its return value (no `return` keyword needed).
    val uiState: StateFlow<HomeUiState> = combine(
        dsaRepository.observeAll(),
        todoRepository.observeAll(),
        moneyRepository.observe(),
    ) { problems, tasks, money ->
            // [Kotlin] `val today = ...` with no type: the compiler infers LocalDate. Like Java 10's `var`,
            // but `val` is also final.
            val today = LocalDate.now()
            val stats = computeStats(problems, today)
            val counts = countsByDay(problems)
            val dsa = TrackerSummary(
                stats = listOf(
                    TrackerStat(stats.total.toString(), R.string.dsa_stat_solved),
                    TrackerStat(stats.currentStreak.toString(), R.string.dsa_stat_streak),
                    TrackerStat(stats.activeDays.toString(), R.string.dsa_stat_active_days),
                ),
                // [Kotlin] `13 downTo 0` is a range 13, 12, ..., 0, mapped to a List of the last 14 days' counts.
                // `it` = the implicit name of a lambda's single parameter (here the number of days ago).
                // `counts[day]` = counts.get(day); it may return null when that day has no entry.
                // `?: 0` is the ELVIS operator: "if the left side is null, use 0". Java equivalent:
                //   Integer c = counts.get(day); int value = (c != null) ? c : 0;
                recent = (13 downTo 0).map { counts[today.minusDays(it.toLong())] ?: 0 },
            )
            val todo = summarize(tasks, today, WeekFields.of(Locale.getDefault()).firstDayOfWeek)
            val todoSummary = TrackerSummary(
                stats = listOf(
                    // [Kotlin] "${a}/${b}" is a STRING TEMPLATE: Java's a + "/" + b or String.format("%d/%d", a, b).
                    TrackerStat("${todo.todayDone}/${todo.todayTotal}", R.string.todo_stat_today),
                    TrackerStat(todo.doneThisWeek.toString(), R.string.todo_stat_week),
                    TrackerStat(todo.open.toString(), R.string.todo_stat_open),
                ),
                recent = (13 downTo 0).map { todo.completionsByDay[today.minusDays(it.toLong())] ?: 0 },
            )
            // [Kotlin] No `new` keyword: you call a constructor like a function.
            // `Trackers.DSA to dsa` creates a key-value Pair; mapOf(...) builds a Map from pairs.
            val format = MoneyFormat()
            val month = YearMonth.from(today)
            val perDay = money.transactions.groupingBy { it.localDate }.eachCount()
            val budgetSummary = TrackerSummary(
                stats = listOf(
                    TrackerStat(format.formatWhole(grandTotals(money).firstOrNull()?.second ?: 0L), R.string.budget_stat_balance),
                    TrackerStat(format.formatWhole(flowTotals(money.transactions.inRange(today..today)).expense), R.string.budget_stat_today),
                    TrackerStat(
                        format.formatWhole(flowTotals(money.transactions.inRange(month.atDay(1)..month.atEndOfMonth())).expense),
                        R.string.budget_stat_month,
                    ),
                ),
                recent = (13 downTo 0).map { perDay[today.minusDays(it.toLong())] ?: 0 },
            )
            HomeUiState(mapOf(Trackers.DSA to dsa, Trackers.TODO to todoSummary, Trackers.BUDGET to budgetSummary))
        }
        // [Kotlin] viewModelScope = coroutines (lightweight background tasks) that are cancelled
        // automatically when the ViewModel is cleared, so there's nothing to unsubscribe in onCleared().
        // WhileSubscribed(5_000): keep working while the screen is visible, plus 5 s (survives rotation).
        // HomeUiState() is the initial value shown before Room answers. `5_000` = 5000 (underscores for readability).
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    // [Kotlin] Kotlin has no `static`. A `companion object` holds class-level members instead:
    // `HomeViewModel.Factory` here works like `public static final ViewModelProvider.Factory FACTORY` in Java.
    // The factory tells Android how to construct this ViewModel, since its constructor needs repositories.
    companion object {
        val Factory = viewModelFactory {
            initializer {
                // [Kotlin] `as` is a cast: Java's `(TrackApplication) ...`.
                val app = this[APPLICATION_KEY] as TrackApplication
                HomeViewModel(app.container.dsaRepository, app.container.todoRepository, app.container.moneyRepository)
            }
        }
    }
}
