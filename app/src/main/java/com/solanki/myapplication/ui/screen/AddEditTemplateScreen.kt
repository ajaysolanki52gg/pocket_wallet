package com.solanki.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.ui.viewmodel.AddEditTemplateViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTemplateScreen(
    templateId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTemplateViewModel = hiltViewModel()
) {
    val nameState by viewModel.name
    val amountState by viewModel.amount
    val typeState by viewModel.type
    val categoryState by viewModel.category
    val noteState by viewModel.note
    val selectedAccount by viewModel.selectedAccount
    val allAccounts by viewModel.allAccounts
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditTemplateViewModel.UiEvent.SaveComplete -> onNavigateBack()
                is AddEditTemplateViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showAccountPicker) {
        AccountPickerDialog(allAccounts, onAccountSelected = { viewModel.onAccountChange(it); showAccountPicker = false }, onDismiss = { showAccountPicker = false })
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(onCategorySelected = { viewModel.onCategoryChange(it); showCategoryPicker = false }, onDismiss = { showCategoryPicker = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveTemplate() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5), 
                    titleContentColor = Color.White, 
                    navigationIconContentColor = Color.White, 
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E88E5))
                .verticalScroll(rememberScrollState())
        ) {
            // Template Name
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Template Name", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = nameState,
                    onValueChange = { viewModel.onNameChange(it) },
                    placeholder = { Text("e.g. Daily Milk, Rent", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }

            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton("INCOME", typeState == TransactionType.INCOME) { viewModel.onTypeChange(TransactionType.INCOME) }
                TabButton("EXPENSE", typeState == TransactionType.EXPENSE) { viewModel.onTypeChange(TransactionType.EXPENSE) }
            }

            // Amount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                TextField(
                    value = amountState,
                    onValueChange = { viewModel.onAmountChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End
                    ),
                    placeholder = { 
                        Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = Color.White.copy(alpha = 0.5f), fontSize = 32.sp) 
                    },
                    suffix = { Text("₹", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Selection Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SelectorItem(
                    label = "Account", 
                    value = selectedAccount?.name ?: "Optional", 
                    onClick = { showAccountPicker = true },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(Modifier.width(16.dp))

                SelectorItem(
                    label = "Category", 
                    value = categoryState, 
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }

            // Note
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Note (Optional)", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextField(
                    value = noteState,
                    onValueChange = { viewModel.onNoteChange(it) },
                    placeholder = { Text("Description...", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
