package com.uzxsan.dev.track.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.money.data.GrandTotal
import com.uzxsan.dev.track.money.data.MoneyAccount
import com.uzxsan.dev.track.money.data.MoneyRepository
import com.uzxsan.dev.track.money.data.MoneySnapshot
import com.uzxsan.dev.track.money.data.MoneyTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoneyUiState(
    val snapshot: MoneySnapshot = MoneySnapshot(),
    val balances: Map<Long, Long> = emptyMap(),
    val grandTotals: List<Pair<GrandTotal, Long>> = emptyList(),
) {
    fun account(id: Long?): MoneyAccount? = snapshot.accounts.firstOrNull { it.id == id }
    fun membersOf(totalId: Long): Set<Long> =
        snapshot.memberships.filter { it.totalId == totalId }.mapTo(mutableSetOf()) { it.accountId }
    fun totalsOf(accountId: Long): Set<Long> =
        snapshot.memberships.filter { it.accountId == accountId }.mapTo(mutableSetOf()) { it.totalId }
}

class MoneyViewModel(private val repository: MoneyRepository) : ViewModel() {

    val uiState: StateFlow<MoneyUiState> = repository.observe()
        .map { snapshot ->
            val balances = balances(snapshot)
            MoneyUiState(snapshot, balances, grandTotals(snapshot, balances))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyUiState())

    fun saveTransaction(transaction: MoneyTransaction) = launch { repository.saveTransaction(transaction) }

    fun deleteTransaction(transaction: MoneyTransaction) = launch { repository.deleteTransaction(transaction) }

    fun saveAccount(account: MoneyAccount, totalIds: Set<Long>) = launch { repository.saveAccount(account, totalIds) }

    fun deleteAccount(account: MoneyAccount) = launch { repository.deleteAccount(account) }

    fun saveTotal(total: GrandTotal, accountIds: Set<Long>) = launch { repository.saveTotal(total, accountIds) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TrackApplication
                MoneyViewModel(app.container.moneyRepository)
            }
        }
    }
}
