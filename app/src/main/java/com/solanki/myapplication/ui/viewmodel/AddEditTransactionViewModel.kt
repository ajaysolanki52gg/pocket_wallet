package com.solanki.myapplication.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
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

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // For Transfer
    private val _targetAccount = mutableStateOf<Account?>(null)
    val targetAccount: State<Account?> = _targetAccount

    init {
        viewModelScope.launch {
            val accounts = repository.getAllAccounts().first()
            _allAccounts.value = accounts
            
            if (transactionId != -1L) {
                repository.getTransactionById(transactionId)?.let { trans ->
                    _amountStr.value = trans.amount.toString()
                    _transactionType.value = trans.type
                    _selectedAccount.value = accounts.find { it.id == trans.accountId }
                    _category.value = trans.category
                    _note.value = trans.notes
                    _date.value = trans.date
                }
            } else {
                _selectedAccount.value = accounts.find { it.id == initialAccountId } ?: accounts.firstOrNull()
            }
        }
    }

    fun onAmountChange(amount: String) {
        // Basic validation: allow only numbers and one decimal point
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _amountStr.value = if (filtered.isEmpty()) "0" else filtered
        }
    }

    fun onEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.Number -> enterNumber(event.number)
            is CalculatorEvent.Decimal -> enterDecimal()
            is CalculatorEvent.Clear -> _amountStr.value = "0"
            is CalculatorEvent.Delete -> delete()
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
    }

    fun onDateChange(timestamp: Long) {
        _date.value = timestamp
    }

    private fun enterNumber(number: Int) {
        if (_amountStr.value == "0") {
            _amountStr.value = number.toString()
        } else {
            _amountStr.value += number.toString()
        }
    }

    private fun enterDecimal() {
        if (!_amountStr.value.contains(".")) {
            _amountStr.value += "."
        }
    }

    private fun delete() {
        if (_amountStr.value.length > 1) {
            _amountStr.value = _amountStr.value.dropLast(1)
        } else {
            _amountStr.value = "0"
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            if (transactionId != -1L) {
                repository.getTransactionById(transactionId)?.let {
                    repository.deleteTransaction(it)
                    _eventFlow.emit(UiEvent.SaveComplete) // Reuse SaveComplete to navigate back
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
                if (targetAcc.id == currentAcc.id) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Source and target accounts cannot be the same"))
                    return@launch
                }
                
                // Expense from source account
                repository.insertTransaction(
                    Transaction(
                        id = if (transactionId != -1L) transactionId else 0,
                        accountId = currentAcc.id,
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        currency = currentAcc.currency,
                        category = "Transfer",
                        title = "Transfer to ${targetAcc.name}",
                        notes = _note.value,
                        date = _date.value,
                        time = System.currentTimeMillis()
                    )
                )
                // Income to target account
                repository.insertTransaction(
                    Transaction(
                        accountId = targetAcc.id,
                        type = TransactionType.INCOME,
                        amount = amount,
                        currency = targetAcc.currency,
                        category = "Transfer",
                        title = "Transfer from ${currentAcc.name}",
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
                        title = if (_note.value.isNotBlank()) _note.value else _category.value,
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
