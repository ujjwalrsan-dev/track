package com.uzxsan.dev.track.dsa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.dsa.data.DsaProblem
import com.uzxsan.dev.track.dsa.data.DsaRepository
import com.uzxsan.dev.track.dsa.data.Platform
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DsaUiState(
    val problemsByDay: Map<LocalDate, List<DsaProblem>> = emptyMap(),
    val countsByDay: Map<LocalDate, Int> = emptyMap(),
    val stats: DsaStats = DsaStats(),
)

class DsaViewModel(private val repository: DsaRepository) : ViewModel() {

    val uiState: StateFlow<DsaUiState> = repository.observeAll()
        .map { problems ->
            DsaUiState(
                problemsByDay = problems.groupBy { it.solvedDate },
                countsByDay = countsByDay(problems),
                stats = computeStats(problems, LocalDate.now()),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DsaUiState())

    fun log(title: String, platform: Platform, difficulty: Difficulty, topic: String, date: LocalDate) {
        viewModelScope.launch {
            repository.save(
                DsaProblem(
                    title = title.trim(),
                    platform = platform,
                    difficulty = difficulty,
                    topic = topic.trim(),
                    solvedOn = date.toEpochDay(),
                )
            )
        }
    }

    fun delete(problem: DsaProblem) {
        viewModelScope.launch { repository.delete(problem) }
    }

    fun restore(problem: DsaProblem) {
        viewModelScope.launch { repository.save(problem) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TrackApplication
                DsaViewModel(app.container.dsaRepository)
            }
        }
    }
}
