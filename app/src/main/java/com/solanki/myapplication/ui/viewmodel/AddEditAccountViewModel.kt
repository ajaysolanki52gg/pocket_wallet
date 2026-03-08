package com.solanki.myapplication.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import com.solanki.myapplication.ui.theme.BankColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _accountName = mutableStateOf("")
    val accountName: State<String> = _accountName

    private val _accountColor = mutableStateOf(String.format("#%08X", BankColors[0].toArgb()))
    val accountColor: State<String> = _accountColor

    private val _accountCurrency = mutableStateOf("₹")
    val accountCurrency: State<String> = _accountCurrency

    private val _initialBalance = mutableStateOf("")
    val initialBalance: State<String> = _initialBalance

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentAccountId: Long? = null

    init {
        savedStateHandle.get<Long>("accountId")?.let { accountId ->
            if (accountId != -1L) {
                viewModelScope.launch {
                    repository.getAccountById(accountId)?.also { account ->
                        currentAccountId = account.id
                        _accountName.value = account.name
                        _accountColor.value = account.color
                        _accountCurrency.value = account.currency
                        _initialBalance.value = String.format(java.util.Locale.US, "%.2f", account.initialBalance)
                    }
                }
            } else {
                viewModelScope.launch {
                    val existingAccounts = repository.getAllAccounts().first()
                    val usedColors = existingAccounts.map { it.color.uppercase() }.toSet()
                    
                    val availableColor = BankColors.firstOrNull { color ->
                        val hex = String.format("#%08X", color.toArgb()).uppercase()
                        !usedColors.contains(hex)
                    } ?: BankColors.random()
                    
                    _accountColor.value = String.format("#%08X", availableColor.toArgb())
                }
            }
        }
    }

    fun onEvent(event: AddEditAccountEvent) {
        when (event) {
            is AddEditAccountEvent.EnteredName -> _accountName.value = event.value
            is AddEditAccountEvent.ChangedColor -> _accountColor.value = event.value
            is AddEditAccountEvent.ChangedCurrency -> _accountCurrency.value = event.value
            is AddEditAccountEvent.EnteredInitialBalance -> _initialBalance.value = event.value
            is AddEditAccountEvent.SaveAccount -> {
                viewModelScope.launch {
                    try {
                        if (_accountName.value.isBlank()) {
                            _eventFlow.emit(UiEvent.ShowSnackbar("Account name can't be empty"))
                            return@launch
                        }
                        val balance = _initialBalance.value.toDoubleOrNull() ?: 0.0
                        val account = Account(
                            id = currentAccountId ?: 0,
                            name = _accountName.value,
                            color = _accountColor.value,
                            currency = _accountCurrency.value,
                            initialBalance = balance
                        )
                        
                        if (currentAccountId != null && currentAccountId != 0L) {
                            repository.updateAccount(account)
                        } else {
                            repository.insertAccount(account)
                        }
                        _eventFlow.emit(UiEvent.SaveAccount)
                    } catch (e: Exception) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Could not save account"))
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveAccount : UiEvent()
    }
}

sealed class AddEditAccountEvent {
    data class EnteredName(val value: String) : AddEditAccountEvent()
    data class ChangedColor(val value: String) : AddEditAccountEvent()
    data class ChangedCurrency(val value: String) : AddEditAccountEvent()
    data class EnteredInitialBalance(val value: String) : AddEditAccountEvent()
    object SaveAccount : AddEditAccountEvent()
}
