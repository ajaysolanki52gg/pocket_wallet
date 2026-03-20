package com.solanki.myapplication.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Template
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTemplateViewModel @Inject constructor(
    private val repository: PocketLedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: Long = savedStateHandle.get<Long>("templateId") ?: -1L

    private val _name = mutableStateOf("")
    val name: State<String> = _name

    private val _amount = mutableStateOf("")
    val amount: State<String> = _amount

    private val _type = mutableStateOf(TransactionType.EXPENSE)
    val type: State<TransactionType> = _type

    private val _category = mutableStateOf("Food")
    val category: State<String> = _category

    private val _note = mutableStateOf("")
    val note: State<String> = _note

    private val _selectedAccount = mutableStateOf<Account?>(null)
    val selectedAccount: State<Account?> = _selectedAccount

    private val _allAccounts = mutableStateOf<List<Account>>(emptyList())
    val allAccounts: State<List<Account>> = _allAccounts

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _allAccounts.value = accounts
                
                if (templateId != -1L && _name.value.isEmpty()) {
                    // Logic for editing template if needed in future
                    // For now we'll just handle creation
                }
            }
        }
    }

    fun onNameChange(name: String) { _name.value = name }
    fun onAmountChange(amount: String) { _amount.value = amount }
    fun onTypeChange(type: TransactionType) { _type.value = type }
    fun onCategoryChange(category: String) { _category.value = category }
    fun onNoteChange(note: String) { _note.value = note }
    fun onAccountChange(account: Account) { _selectedAccount.value = account }

    fun saveTemplate() {
        if (_name.value.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Template name is required")) }
            return
        }
        
        viewModelScope.launch {
            val template = Template(
                name = _name.value,
                type = _type.value,
                amount = _amount.value.toDoubleOrNull(),
                category = _category.value,
                note = _note.value,
                accountId = _selectedAccount.value?.id
            )
            repository.insertTemplate(template)
            _eventFlow.emit(UiEvent.SaveComplete)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveComplete : UiEvent()
    }
}
