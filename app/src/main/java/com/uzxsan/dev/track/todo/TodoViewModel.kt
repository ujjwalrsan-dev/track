package com.uzxsan.dev.track.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoRepository
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.todo.data.TodoTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TodoUiState(val tasksByDate: Map<LocalDate, List<TaskWithStages>> = emptyMap())

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    val uiState: StateFlow<TodoUiState> = repository.observeAll()
        .map { tasks -> TodoUiState(tasks.groupBy { it.task.localDate }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    fun add(title: String, date: LocalDate, time: LocalTime?, stageNames: List<String>) {
        viewModelScope.launch {
            repository.add(
                TodoTask(
                    title = title.trim(),
                    date = date.toEpochDay(),
                    timeMinutes = time?.let { it.hour * 60 + it.minute },
                ),
                stageNames.map { it.trim() }.filter { it.isNotEmpty() },
            )
        }
    }

    fun advance(item: TaskWithStages) = update(item.advance(System.currentTimeMillis()))

    fun toggle(stage: TodoStage) = update(listOf(stage.toggled(System.currentTimeMillis())))

    private fun update(stages: List<TodoStage>) {
        if (stages.isEmpty()) return
        viewModelScope.launch { repository.updateStages(stages) }
    }

    fun delete(item: TaskWithStages) {
        viewModelScope.launch { repository.delete(item.task) }
    }

    fun restore(item: TaskWithStages) {
        viewModelScope.launch { repository.restore(item) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TrackApplication
                TodoViewModel(app.container.todoRepository)
            }
        }
    }
}
