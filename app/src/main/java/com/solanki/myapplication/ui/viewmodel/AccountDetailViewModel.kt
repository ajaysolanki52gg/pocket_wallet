package com.solanki.myapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>("accountId") ?: -1L

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(
        _allTransactions,
        _searchQuery,
        _selectedFilter
    ) { transactions, query, filter ->
        transactions.filter { transaction ->
            val matchesQuery = transaction.title.contains(query, ignoreCase = true) ||
                    transaction.category.contains(query, ignoreCase = true) ||
                    transaction.notes.contains(query, ignoreCase = true)
            
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.LAST_7_DAYS -> isWithinDays(transaction.date, 7)
                TransactionFilter.LAST_30_DAYS -> isWithinDays(transaction.date, 30)
                TransactionFilter.THIS_MONTH -> isThisMonth(transaction.date)
            }
            
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _incomeTotal = MutableStateFlow(0.0)
    val incomeTotal: StateFlow<Double> = _incomeTotal.asStateFlow()

    private val _expenseTotal = MutableStateFlow(0.0)
    val expenseTotal: StateFlow<Double> = _expenseTotal.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    init {
        viewModelScope.launch {
            if (accountId != -1L) {
                val accountData = repository.getAccountById(accountId)
                _account.value = accountData
                
                if (accountData != null) {
                    combine(
                        repository.getTotalIncomeForAccount(accountId),
                        repository.getTotalExpenseForAccount(accountId)
                    ) { income, expense ->
                        val inc = income ?: 0.0
                        val exp = expense ?: 0.0
                        _incomeTotal.value = inc
                        _expenseTotal.value = exp
                        _balance.value = accountData.initialBalance + inc - exp
                    }.collect()
                }
            } else {
                // All Accounts View: Aggregate initial balances + all income - all expense
                repository.getAllAccounts().collect { accounts ->
                    val totalInitial = accounts.sumOf { it.initialBalance }
                    // Fetch global aggregates if needed, or iterate
                    // For now loading transactions is the priority
                }
            }
        }

        viewModelScope.launch {
            // Updated to use the corrected accountId (-1 handles 'All' in the updated DAO)
            repository.getTransactionsForAccount(accountId).collect {
                _allTransactions.value = it
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    private fun isWithinDays(timestamp: Long, days: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return timestamp >= calendar.timeInMillis
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}

enum class TransactionFilter(val label: String) {
    ALL("All"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    THIS_MONTH("This Month")
}
