package com.solanki.myapplication.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.data.model.Template
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class CalculatorEvent {
    data class Number(val number: Int) : CalculatorEvent()
    object Decimal : CalculatorEvent()
    object Clear : CalculatorEvent()
    object Delete : CalculatorEvent()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: -1L
    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: -1L

    private val _amountStr = mutableStateOf("0")
    val amountStr: State<String> = _amountStr

    private val _transactionType = mutableStateOf(TransactionType.EXPENSE)
    val transactionType: State<TransactionType> = _transactionType

    private val _selectedAccount = mutableStateOf<Account?>(null)
    val selectedAccount: State<Account?> = _selectedAccount

    private val _allAccounts = mutableStateOf<List<Account>>(emptyList())
    val allAccounts: State<List<Account>> = _allAccounts

    private val _category = mutableStateOf("Food")
    val category: State<String> = _category

    private val _note = mutableStateOf("")
    val note: State<String> = _note

    private val _date = mutableStateOf(System.currentTimeMillis())
    val date: State<Long> = _date

    private val _templates = mutableStateOf<List<Template>>(emptyList())
    val templates: State<List<Template>> = _templates

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // For Transfer
    private val _targetAccount = mutableStateOf<Account?>(null)
    val targetAccount: State<Account?> = _targetAccount

    // Suggestions logic
    private val _suggestions = mutableStateOf<List<String>>(emptyList())
    val suggestions: State<List<String>> = _suggestions
    
    private var historicalNotes: List<String> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _allAccounts.value = accounts
                
                if (transactionId != -1L && _selectedAccount.value == null) {
                    repository.getTransactionById(transactionId)?.let { trans ->
                        _amountStr.value = trans.amount.toString()
                        _transactionType.value = trans.type
                        _selectedAccount.value = accounts.find { it.id == trans.accountId }
                        _category.value = trans.category
                        _note.value = trans.notes
                        _date.value = trans.date
                    }
                } else if (_selectedAccount.value == null) {
                    _selectedAccount.value = accounts.find { it.id == initialAccountId } ?: accounts.firstOrNull()
                }
            }
        }

        // Collect historical notes reactively
        viewModelScope.launch {
            repository.getAllTransactionNotes().collect { notes ->
                historicalNotes = notes.filter { it.isNotBlank() }.distinct()
                updateSuggestions(_note.value)
            }
        }

        // Collect templates
        viewModelScope.launch {
            repository.getAllTemplates().collect { templates ->
                _templates.value = templates
            }
        }
    }

    fun applyTemplate(template: Template) {
        _transactionType.value = template.type
        template.amount?.let { _amountStr.value = it.toString() }
        template.category?.let { _category.value = it }
        template.note?.let { _note.value = it }
        template.accountId?.let { id ->
            _allAccounts.value.find { it.id == id }?.let { _selectedAccount.value = it }
        }
    }

    fun saveAsTemplate(name: String) {
        viewModelScope.launch {
            val template = Template(
                name = name,
                type = _transactionType.value,
                amount = _amountStr.value.toDoubleOrNull(),
                category = _category.value,
                note = _note.value,
                accountId = _selectedAccount.value?.id
            )
            repository.insertTemplate(template)
            _eventFlow.emit(UiEvent.ShowSnackbar("Template '$name' saved"))
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun onAmountChange(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _amountStr.value = if (filtered.isEmpty()) "0" else filtered
        }
    }

    fun onTransactionTypeChange(type: TransactionType) {
        _transactionType.value = type
        if (type == TransactionType.TRANSFER && _targetAccount.value == null) {
            _targetAccount.value = _allAccounts.value.firstOrNull { it.id != _selectedAccount.value?.id }
        }
    }

    fun onAccountChange(account: Account) {
        _selectedAccount.value = account
        if (_transactionType.value == TransactionType.TRANSFER && _targetAccount.value?.id == account.id) {
            _targetAccount.value = _allAccounts.value.firstOrNull { it.id != account.id }
        }
    }
    
    fun onTargetAccountChange(account: Account) {
        if (account.id == _selectedAccount.value?.id) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Source and target accounts cannot be the same"))
            }
            return
        }
        _targetAccount.value = account
    }

    fun onCategoryChange(category: String) {
        _category.value = category
    }

    fun onNoteChange(note: String) {
        _note.value = note
        updateSuggestions(note)
    }

    private fun updateSuggestions(input: String) {
        val trimmedInput = input.trim()
        if (trimmedInput.length >= 3) {
            val lowerInput = trimmedInput.lowercase()
            _suggestions.value = historicalNotes.filter { note ->
                val lowerNote = note.lowercase()
                lowerNote.contains(lowerInput) && lowerNote != lowerInput
            }.sortedBy { 
                !it.lowercase().startsWith(lowerInput)
            }.take(5)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun onSuggestionClick(suggestion: String) {
        _note.value = suggestion
        _suggestions.value = emptyList()
    }

    fun onDateChange(timestamp: Long) {
        _date.value = timestamp
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            if (transactionId != -1L) {
                repository.getTransactionById(transactionId)?.let {
                    repository.deleteTransaction(it)
                    _eventFlow.emit(UiEvent.SaveComplete)
                }
            }
        }
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val amount = _amountStr.value.toDoubleOrNull() ?: return@launch
            if (amount == 0.0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Amount cannot be zero"))
                return@launch
            }
            
            val currentAcc = _selectedAccount.value ?: return@launch

            if (_transactionType.value == TransactionType.TRANSFER) {
                val targetAcc = _targetAccount.value
                if (targetAcc == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select a target account"))
                    return@launch
                }
                
                repository.insertTransaction(
                    Transaction(
                        id = if (transactionId != -1L) transactionId else 0,
                        accountId = currentAcc.id,
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        currency = currentAcc.currency,
                        category = "Transfer",
                        title = "Transfer",
                        notes = _note.value,
                        date = _date.value,
                        time = System.currentTimeMillis()
                    )
                )
                repository.insertTransaction(
                    Transaction(
                        accountId = targetAcc.id,
                        type = TransactionType.INCOME,
                        amount = amount,
                        currency = targetAcc.currency,
                        category = "Transfer",
                        title = "Transfer",
                        notes = _note.value,
                        date = _date.value,
                        time = System.currentTimeMillis()
                    )
                )
            } else {
                repository.insertTransaction(
                    Transaction(
                        id = if (transactionId != -1L) transactionId else 0,
                        accountId = currentAcc.id,
                        type = _transactionType.value,
                        amount = amount,
                        currency = currentAcc.currency,
                        category = _category.value,
                        title = _category.value,
                        notes = _note.value,
                        date = _date.value,
                        time = System.currentTimeMillis()
                    )
                )
            }
            _eventFlow.emit(UiEvent.SaveComplete)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveComplete : UiEvent()
    }
}
