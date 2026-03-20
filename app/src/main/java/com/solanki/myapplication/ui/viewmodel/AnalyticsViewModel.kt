package com.solanki.myapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import com.solanki.myapplication.ui.screen.CATEGORY_DATA
import com.solanki.myapplication.util.DataUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TransactionGroup(
    val title: String,
    val totalExpense: Double,
    val balance: Double? = null,
    val transactions: List<Transaction>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: -1L

    private val _timeFilter = MutableStateFlow<TimeFilter>(TimeFilter.LAST_30_DAYS)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(
        if (initialAccountId == -1L) emptySet() else setOf(initialAccountId)
    )
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allAccounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = repository.getAccountsWithBalanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow<String?>(null)
    val selectedSubcategory: StateFlow<String?> = _selectedSubcategory.asStateFlow()

    private val _isBreakdownMode = MutableStateFlow(false)
    val isBreakdownMode: StateFlow<Boolean> = _isBreakdownMode.asStateFlow()

    private val filterParams = combine(_timeFilter, _customDateRange, _selectedAccountIds) { filter, range, accountIds ->
        val since = if (filter == TimeFilter.CUSTOM) range?.first ?: 0L else filter.getStartTime()
        val until = if (filter == TimeFilter.CUSTOM) range?.second ?: Long.MAX_VALUE else Long.MAX_VALUE
        val effectiveAccountIds = accountIds.toList()
        Triple(since, until, effectiveAccountIds)
    }

    val balanceTrend: StateFlow<List<Pair<Long, Double>>> = combine(_selectedAccountIds, _timeFilter, _customDateRange) { ids, filter, range ->
        Triple(ids, filter, range)
    }.flatMapLatest { (ids, filter, range) ->
        val since = if (filter == TimeFilter.CUSTOM) range?.first ?: filter.getStartTime() else filter.getStartTime()
        val until = if (filter == TimeFilter.CUSTOM) range?.second ?: System.currentTimeMillis() else System.currentTimeMillis()
        repository.getBalanceTrendFlow(ids.toList(), since, until)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawSpendingByCategory: StateFlow<List<CategorySum>> = combine(_selectedAccountIds, _timeFilter, _customDateRange) { ids, filter, range ->
        Triple(ids, filter, range)
    }.flatMapLatest { (ids, filter, range) ->
        val since = if (filter == TimeFilter.CUSTOM) range?.first ?: 0L else filter.getStartTime()
        val until = if (filter == TimeFilter.CUSTOM) range?.second ?: Long.MAX_VALUE else Long.MAX_VALUE
        repository.getSpendingAnalysisFlow(ids.toList(), since, until)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // HOME PAGE LOGIC: Always grouped by Main Category
    val spendingByCategory: StateFlow<List<CategorySum>> = rawSpendingByCategory.map { list ->
        DataUtils.groupSubcategoriesToMain(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Get all expense transactions within the current filter/accounts
    private val allExpenseTransactions: StateFlow<List<Transaction>> = filterParams.flatMapLatest { (since, until, accountIds) ->
        if (accountIds.isEmpty()) {
            repository.getTransactionsForAnalysis(since, until)
        } else {
            repository.getTransactionsForAnalysisForAccounts(accountIds, since, until)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BREAKDOWN PAGE LOGIC: Subcategories for the selected main category
    val subcategorySpending: StateFlow<List<CategorySum>> = combine(allExpenseTransactions, _selectedCategory) { transactions, mainCategory ->
        if (mainCategory == null) emptyList()
        else {
            transactions.filter { DataUtils.findMainCategory(it.category) == mainCategory }
                .groupBy { it.category }
                .map { (sub, list) -> CategorySum(sub, list.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactionsFlow: Flow<List<Transaction>> = filterParams.flatMapLatest { (since, until, accountIds) ->
        if (accountIds.isEmpty()) {
            repository.getAllTransactions(since, until)
        } else {
            repository.getAllTransactionsForAccounts(accountIds, since, until)
        }
    }

    val groupedTransactions: StateFlow<List<TransactionGroup>> = combine(
        allTransactionsFlow,
        _searchQuery,
        _timeFilter
    ) { transactions, query, filter ->
        val filtered = if (query.isBlank()) transactions else {
            transactions.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            }
        }
        groupTransactions(filtered, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unified transaction list for the Spending tab (filtered by chart selection)
    val spendingTransactions: StateFlow<List<Transaction>> = combine(
        allExpenseTransactions,
        _selectedCategory, 
        _selectedSubcategory,
        _isBreakdownMode
    ) { transactions, category, subcategory, isBreakdown ->
        if (category == null) {
            transactions.sortedByDescending { it.amount }
        } else {
            transactions.filter { trans ->
                val main = DataUtils.findMainCategory(trans.category)
                if (isBreakdown && subcategory != null) {
                    main == category && trans.category.equals(subcategory, ignoreCase = true)
                } else {
                    main == category
                }
            }.sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun groupTransactions(transactions: List<Transaction>, filter: TimeFilter): List<TransactionGroup> {
        if (transactions.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance()
        val groups = linkedMapOf<String, MutableList<Transaction>>()
        transactions.forEach { trans ->
            val date = Date(trans.date)
            calendar.time = date
            val key = when (filter) {
                TimeFilter.LAST_7_DAYS -> getDayKey(calendar)
                TimeFilter.LAST_14_DAYS -> getWeekRangeKey(calendar)
                TimeFilter.LAST_30_DAYS, TimeFilter.LAST_12_WEEKS, TimeFilter.THREE_MONTHS -> getWeekNumberKey(calendar)
                TimeFilter.SIX_MONTHS, TimeFilter.ONE_YEAR, TimeFilter.TWO_YEARS, TimeFilter.THIS_MONTH -> getMonthKey(calendar)
                else -> getDayKey(calendar)
            }
            groups.getOrPut(key) { mutableListOf() }.add(trans)
        }
        return groups.map { (title, list) ->
            val totalExp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val totalInc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            TransactionGroup(title, totalExp, totalInc - totalExp, list)
        }
    }

    private fun getDayKey(cal: Calendar): String {
        val today = Calendar.getInstance()
        if (isSameDay(cal, today)) return "TODAY"
        today.add(Calendar.DAY_OF_YEAR, -1)
        if (isSameDay(cal, today)) return "YESTERDAY"
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time).uppercase()
    }

    private fun getWeekRangeKey(cal: Calendar): String {
        val now = Calendar.getInstance()
        val startOfThisWeek = now.apply { 
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        return if (cal.timeInMillis >= startOfThisWeek) "THIS WEEK" else "LAST WEEK"
    }

    private fun getWeekNumberKey(cal: Calendar): String {
        return "WEEK ${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun getMonthKey(cal: Calendar): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time).uppercase()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _selectedSubcategory.value = null
        if (category == null) {
            _isBreakdownMode.value = false
        }
    }

    fun selectSubcategory(subcategory: String?) {
        _selectedSubcategory.value = subcategory
    }

    fun toggleBreakdownMode() {
        if (_selectedCategory.value != null) {
            _isBreakdownMode.value = !_isBreakdownMode.value
            if (!_isBreakdownMode.value) {
                _selectedSubcategory.value = null
            }
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = start to end
        _timeFilter.value = TimeFilter.CUSTOM
    }

    fun toggleAccountSelection(accountId: Long) {
        _selectedAccountIds.update { current ->
            if (current.contains(accountId)) current - accountId else current + accountId
        }
    }

    fun selectAllAccounts() {
        _selectedAccountIds.value = emptySet()
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransactionCategory(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            repository.insertTransaction(transaction.copy(category = newCategory, updatedAt = System.currentTimeMillis()))
        }
    }
}
