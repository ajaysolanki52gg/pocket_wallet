package com.solanki.myapplication.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import com.solanki.myapplication.ui.screen.CATEGORY_DATA
import com.solanki.myapplication.ui.theme.BankColors
import com.solanki.myapplication.util.DataUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    private val app: Application
) : ViewModel() {

    private val prefs = app.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE)

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(
        prefs.getStringSet("selected_account_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    )
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == "is_dark_mode") {
            _isDarkMode.value = sharedPrefs.getBoolean(key, false)
        }
    }

    private val _timeFilter = MutableStateFlow(TimeFilter.LAST_7_DAYS)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = repository.getAccountsWithBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = _selectedAccountIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                repository.getAllTransactions().map { it.take(5) }
            } else {
                repository.getAllTransactionsForAccounts(ids.toList()).map { it.take(5) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceTrend: StateFlow<List<Pair<Long, Double>>> = _selectedAccountIds
        .flatMapLatest { ids ->
            repository.getBalanceTrendFlow(
                accountIds = ids.toList(),
                since = TimeFilter.LAST_30_DAYS.getStartTime(),
                until = System.currentTimeMillis()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendingByCategory: StateFlow<List<CategorySum>> = combine(_selectedAccountIds, _timeFilter, _customDateRange) { ids, filter, range ->
        Triple(ids, filter, range)
    }.flatMapLatest { (ids, filter, range) ->
        val since = if (filter == TimeFilter.CUSTOM) range?.first ?: 0L else filter.getStartTime()
        val until = if (filter == TimeFilter.CUSTOM) range?.second ?: Long.MAX_VALUE else Long.MAX_VALUE
        repository.getSpendingAnalysisFlow(ids.toList(), since, until)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        viewModelScope.launch {
            repository.getAllAccounts().first().forEach { account ->
                if (account.color.isBlank() || !account.color.startsWith("#")) {
                    val newColor = String.format("#%08X", BankColors.random().toArgb())
                    repository.updateAccount(account.copy(color = newColor))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    fun toggleAccountSelection(accountId: Long) {
        _selectedAccountIds.update { current ->
            val next = if (current.contains(accountId)) current - accountId else current + accountId
            saveSelectedAccounts(next)
            next
        }
    }

    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("is_dark_mode", newValue).apply()
    }

    fun selectAllAccounts() {
        _selectedAccountIds.value = emptySet()
        saveSelectedAccounts(emptySet())
    }

    private fun saveSelectedAccounts(ids: Set<Long>) {
        prefs.edit().putStringSet("selected_account_ids", ids.map { it.toString() }.toSet()).apply()
    }

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = start to end
        _timeFilter.value = TimeFilter.CUSTOM
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            if (_selectedAccountIds.value.contains(account.id)) {
                toggleAccountSelection(account.id)
            }
        }
    }

    fun updateTransactionCategory(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            repository.insertTransaction(transaction.copy(category = newCategory, updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateAccountOrder(reorderedList: List<AccountWithBalance>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, accountWithBalance ->
                if (accountWithBalance.account.orderIndex != index) {
                    repository.updateAccount(accountWithBalance.account.copy(orderIndex = index))
                }
            }
        }
    }

    suspend fun checkpointDatabase() {
        repository.checkpointDatabase()
    }

    suspend fun closeDatabase() {
        repository.closeDatabase()
    }
}
