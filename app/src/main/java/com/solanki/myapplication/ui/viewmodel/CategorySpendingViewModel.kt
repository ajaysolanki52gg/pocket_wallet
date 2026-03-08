package com.solanki.myapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategorySpendingViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: -1L

    private val _timeFilter = MutableStateFlow<TimeFilter>(TimeFilter.LAST_7_DAYS)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(
        if (initialAccountId == -1L) emptySet() else setOf(initialAccountId)
    )
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    val allAccounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val filterParams: Flow<Triple<Long, Long, List<Long>>> = combine(
        _timeFilter, 
        _customDateRange, 
        _selectedAccountIds
    ) { filter: TimeFilter, range: Pair<Long, Long>?, accountIds: Set<Long> ->
        val since = if (filter == TimeFilter.CUSTOM) range?.first ?: 0L else filter.getStartTime()
        val until = if (filter == TimeFilter.CUSTOM) range?.second ?: System.currentTimeMillis() else System.currentTimeMillis()
        val effectiveAccountIds = accountIds.toList()
        Triple(since, until, effectiveAccountIds)
    }

    val spendingByCategory: StateFlow<List<CategorySum>> = filterParams.flatMapLatest { params ->
        val (since, until, accountIds) = params
        if (accountIds.isEmpty()) {
            repository.getSpendingByCategory(since = since, until = until)
        } else {
            repository.getSpendingByCategoryForAccounts(accountIds, since = since, until = until)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryTransactions: StateFlow<List<Transaction>> = combine(_selectedCategory, filterParams) { category: String?, params: Triple<Long, Long, List<Long>> ->
        category to params
    }.flatMapLatest { pair ->
        val (category, params) = pair
        val (since, until, accountIds) = params
        val effectiveAccountIds = if (accountIds.isEmpty()) listOf(-1L) else accountIds
        
        if (category == null) {
            repository.getTransactionsForAnalysisForAccounts(effectiveAccountIds, since, until)
        } else {
            repository.getTopTransactionsByCategoryForAccounts(effectiveAccountIds, category, since, until)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
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
}
