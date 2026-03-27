package com.solanki.myapplication.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.data.model.Template
import com.solanki.myapplication.ui.viewmodel.AddEditTransactionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    accountId: Long,
    transactionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAddTemplate: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val amountState by viewModel.amountStr
    val typeState by viewModel.transactionType
    val selectedAccount by viewModel.selectedAccount
    val targetAccount by viewModel.targetAccount
    val allAccounts by viewModel.allAccounts
    val categoryState by viewModel.category
    val noteState by viewModel.note
    val dateState by viewModel.date
    val suggestions by viewModel.suggestions
    val templates by viewModel.templates
    
    val snackbarHostState = remember { SnackbarHostState() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateState)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = dateState }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = dateState }.get(Calendar.MINUTE)
    )
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showTargetAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    var showTemplateCard by remember { mutableStateOf(false) }
    val drawerOffset by animateDpAsState(targetValue = if (showTemplateCard) 0.dp else 300.dp)

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditTransactionViewModel.UiEvent.SaveComplete -> onNavigateBack()
                is AddEditTransactionViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        val calendar = Calendar.getInstance().apply {
                            val currentTime = Calendar.getInstance().apply { timeInMillis = dateState }
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
                        }
                        viewModel.onDateChange(calendar.timeInMillis) 
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = dateState
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    viewModel.onDateChange(calendar.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showAccountPicker) {
        AccountPickerDialog(allAccounts, onAccountSelected = { viewModel.onAccountChange(it); showAccountPicker = false }, onDismiss = { showAccountPicker = false })
    }
    
    if (showTargetAccountPicker) {
        val filteredAccounts = allAccounts.filter { it.id != selectedAccount?.id }
        AccountPickerDialog(filteredAccounts, onAccountSelected = { viewModel.onTargetAccountChange(it); showTargetAccountPicker = false }, onDismiss = { showTargetAccountPicker = false })
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(onCategorySelected = { viewModel.onCategoryChange(it); showCategoryPicker = false }, onDismiss = { showCategoryPicker = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteTransaction()
                    showDeleteConfirm = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(if (transactionId == -1L) "Add Transaction" else "Edit Transaction") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (transactionId != -1L) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        IconButton(onClick = { if (!showTemplateCard) viewModel.saveTransaction() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E88E5), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF1E88E5))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TabButton("INCOME", typeState == TransactionType.INCOME) { viewModel.onTransactionTypeChange(TransactionType.INCOME) }
                        TabButton("EXPENSE", typeState == TransactionType.EXPENSE) { viewModel.onTransactionTypeChange(TransactionType.EXPENSE) }
                        TabButton("TRANSFER", typeState == TransactionType.TRANSFER) { viewModel.onTransactionTypeChange(TransactionType.TRANSFER) }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.3f))

                    // Amount Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        TextField(
                            value = if (amountState == "0") "" else amountState,
                            onValueChange = { viewModel.onAmountChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.End
                            ),
                            placeholder = { 
                                Text(
                                    "0", 
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 48.sp
                                ) 
                            },
                            suffix = {
                                Text(
                                    "₹", 
                                    color = Color.White.copy(alpha = 0.7f), 
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = IndianAmountTransformation(),
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
                            label = "From Account", 
                            value = selectedAccount?.name ?: "Select", 
                            onClick = { showAccountPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(Modifier.width(16.dp))

                        if (typeState == TransactionType.TRANSFER) {
                            SelectorItem(
                                label = "To Account", 
                                value = targetAccount?.name ?: "Select", 
                                onClick = { showTargetAccountPicker = true },
                                modifier = Modifier.weight(1f),
                                alignEnd = true
                            )
                        } else {
                            SelectorItem(
                                label = "Category", 
                                value = categoryState, 
                                onClick = { showCategoryPicker = true },
                                modifier = Modifier.weight(1f),
                                alignEnd = true
                            )
                        }
                    }

                    // Note/Description section
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text("Note", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Container for suggestions that doesn't take space in layout but overlaps upwards
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.dp)
                                    .wrapContentHeight(Alignment.Bottom, unbounded = true)
                                    .zIndex(10f)
                            ) {
                                if (suggestions.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .heightIn(max = 200.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1565C0),
                                        shadowElevation = 8.dp,
                                        tonalElevation = 8.dp
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(suggestions) { suggestion ->
                                                Text(
                                                    text = suggestion,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.onSuggestionClick(suggestion) }
                                                        .padding(12.dp),
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                                if (suggestion != suggestions.last()) {
                                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            TextField(
                                value = noteState,
                                onValueChange = { viewModel.onNoteChange(it) },
                                placeholder = { Text("Enter description here...", color = Color.White.copy(alpha = 0.5f)) },
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
                    
                    // Date & Time section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Date", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateState)),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker = true },
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Time", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    Text(
                                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateState)),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Modern UI Pull-out Handle
        if (!showTemplateCard) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .zIndex(1f)
            ) {
                Surface(
                    onClick = { showTemplateCard = true },
                    modifier = Modifier
                        .offset(x = 10.dp) // Peeking a bit
                        .size(width = 40.dp, height = 80.dp)
                        .align(Alignment.CenterEnd),
                    shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Show Templates",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }

        // Template Drawer Card
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .offset(x = drawerOffset)
                .align(Alignment.CenterEnd)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                .clickable(enabled = false) {} // Consume clicks
                .padding(24.dp)
                .zIndex(2f)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Templates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { showTemplateCard = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // New Template Button - More Modern
                Button(
                    onClick = onNavigateToAddTemplate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Template", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Templates List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(templates) { template ->
                        TemplateItem(
                            template = template,
                            onClick = {
                                viewModel.applyTemplate(template)
                                showTemplateCard = false
                            },
                            onDelete = { viewModel.deleteTemplate(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateItem(template: Template, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, color) = getCategoryIconAndColor(template.category ?: "Other")
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp)
                template.amount?.let { 
                    Text("₹ ${formatCurrency(it)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SelectorItem(
    label: String, 
    value: String, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignEnd) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(
                value, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp,
                maxLines = 1
            )
            if (!alignEnd) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun AccountPickerDialog(accounts: List<Account>, onAccountSelected: (Account) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Account") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    items(accounts) { account ->
                        Text(
                            text = account.name,
                            modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) }.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Select Time",
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}
