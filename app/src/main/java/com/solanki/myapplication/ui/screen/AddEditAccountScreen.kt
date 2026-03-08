package com.solanki.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solanki.myapplication.ui.theme.BankColors
import com.solanki.myapplication.ui.viewmodel.AddEditAccountEvent
import com.solanki.myapplication.ui.viewmodel.AddEditAccountViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel()
) {
    val nameState = viewModel.accountName.value
    val colorState = viewModel.accountColor.value
    val currencyState = viewModel.accountCurrency.value
    val balanceState = viewModel.initialBalance.value
    
    val snackbarHostState = SnackbarHostState()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditAccountViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is AddEditAccountViewModel.UiEvent.SaveAccount -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (accountId == -1L) "Add Account" else "Edit Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AddEditAccountEvent.SaveAccount) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nameState,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredName(it)) },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = balanceState,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredInitialBalance(it)) },
                label = { Text("Initial Balance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text("Select Color", style = MaterialTheme.typography.titleMedium)
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BankColors) { color ->
                    val colorHex = String.format("#%08X", color.toArgb())
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (colorState == colorHex) 3.dp else 0.dp,
                                color = if (colorState == colorHex) Color.Black else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.onEvent(AddEditAccountEvent.ChangedColor(colorHex))
                            }
                    )
                }
            }

            Text("Currency", style = MaterialTheme.typography.titleMedium)
            
            val currencies = listOf("USD", "EUR", "GBP", "₹", "JPY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currencies.forEach { currency ->
                    FilterChip(
                        selected = currencyState == currency,
                        onClick = { viewModel.onEvent(AddEditAccountEvent.ChangedCurrency(currency)) },
                        label = { Text(currency) }
                    )
                }
            }
        }
    }
}