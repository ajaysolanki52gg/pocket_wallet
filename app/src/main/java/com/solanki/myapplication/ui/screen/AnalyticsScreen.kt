package com.solanki.myapplication.ui.screen

import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.ui.viewmodel.AnalyticsViewModel
import com.solanki.myapplication.ui.viewmodel.TimeFilter
import com.solanki.myapplication.ui.viewmodel.TransactionGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    accountId: Long,
    initialPage: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long, Long) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val subcategorySpending by viewModel.subcategorySpending.collectAsState()
    val isBreakdownMode by viewModel.isBreakdownMode.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsState()
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val spendingTransactions by viewModel.spendingTransactions.collectAsState()
    val currentTimeFilter by viewModel.timeFilter.collectAsState()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsState()
    val customRange by viewModel.customDateRange.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val balanceTrend by viewModel.balanceTrend.collectAsState()

    val tabs = listOf("Transactions", "Spending", "Balance")
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, tabs.size - 1), pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCategoryPickerForTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Auto-adjust default filter based on tab: 30D for Balance, 7D for others
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) { // Balance tab
            if (currentTimeFilter != TimeFilter.LAST_30_DAYS && currentTimeFilter != TimeFilter.CUSTOM) {
                viewModel.setTimeFilter(TimeFilter.LAST_30_DAYS)
            }
        } else { // Transactions or Spending
            if (currentTimeFilter == TimeFilter.LAST_30_DAYS) {
                viewModel.setTimeFilter(TimeFilter.LAST_7_DAYS)
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { 
            val transactionsToExport = groupedTransactions.flatMap { it.transactions }
            scope.launch { exportTransactionsToCsv(context, it, transactionsToExport, allAccounts) } 
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customRange?.first ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = datePickerState.selectedDateMillis
                    val end = customRange?.second ?: System.currentTimeMillis()
                    if (start != null) {
                        viewModel.setCustomDateRange(start, end)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customRange?.second ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val end = datePickerState.selectedDateMillis
                    val start = customRange?.first ?: System.currentTimeMillis()
                    if (end != null) {
                        viewModel.setCustomDateRange(start, end)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAccountSelector) {
        AccountSelectorDialog(
            allAccounts = allAccounts,
            selectedAccountIds = selectedAccountIds,
            onDismiss = { showAccountSelector = false },
            onToggleAccount = { viewModel.toggleAccountSelection(it) },
            onSelectAll = { viewModel.selectAllAccounts() }
        )
    }

    if (showDeleteConfirm && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedTransaction?.let { viewModel.deleteTransaction(it) }
                    selectedTransaction = null
                    showDeleteConfirm = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCategoryPickerForTransaction != null) {
        CategoryPickerDialog(
            onCategorySelected = { newCategory ->
                viewModel.updateTransactionCategory(showCategoryPickerForTransaction!!, newCategory)
                showCategoryPickerForTransaction = null
            },
            onDismiss = { showCategoryPickerForTransaction = null }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                Column {
                    if (selectedTransaction == null) {
                        if (isSearching) {
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChange(it) },
                                onClose = { 
                                    isSearching = false
                                    viewModel.onSearchQueryChange("")
                                }
                            )
                        } else {
                            TopAppBar(
                                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { isSearching = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                    Box {
                                        IconButton(onClick = { showMenu = !showMenu }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Export CSV") },
                                                onClick = { 
                                                    showMenu = false
                                                    csvExportLauncher.launch("transactions_export_" + System.currentTimeMillis() + ".csv")
                                                }
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = onPrimary,
                                    navigationIconContentColor = onPrimary,
                                    actionIconContentColor = onPrimary
                                )
                            )
                        }
                    } else {
                        TopAppBar(
                            title = { Text("1 Selected") },
                            navigationIcon = {
                                IconButton(onClick = { selectedTransaction = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                                }
                            },
                            actions = {
                                IconButton(onClick = { 
                                    onNavigateToEditTransaction(selectedTransaction!!.accountId, selectedTransaction!!.id)
                                    selectedTransaction = null
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = onPrimary,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = onPrimary,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                unselectedContentColor = onPrimary.copy(alpha = 0.7f),
                                selectedContentColor = onPrimary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            PullUpFilterCard(
                currentTimeFilter = currentTimeFilter,
                customRange = customRange,
                selectedAccountIds = selectedAccountIds,
                allAccounts = allAccounts,
                isExpanded = isFilterExpanded,
                onExpandToggle = { isFilterExpanded = !isFilterExpanded },
                onTimeFilterSelected = {
                    viewModel.selectCategory(null)
                    viewModel.setTimeFilter(it)
                },
                onStartDateClick = {
                    showStartDatePicker = true
                },
                onEndDateClick = {
                    showEndDatePicker = true
                },
                onAccountSelectorClick = { showAccountSelector = true }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> TransactionHistoryContent(
                    groupedTransactions = groupedTransactions,
                    allAccounts = allAccounts,
                    selectedTransactionId = selectedTransaction?.id,
                    onTransactionClick = { transaction ->
                        if (selectedTransaction == null) {
                            onNavigateToEditTransaction(transaction.accountId, transaction.id)
                        } else {
                            selectedTransaction = if (selectedTransaction?.id == transaction.id) null else transaction
                        }
                    },
                    onTransactionLongClick = { transaction ->
                        selectedTransaction = transaction
                    },
                    onCategoryClick = { transaction ->
                        showCategoryPickerForTransaction = transaction
                    }
                )
                1 -> CategorySpendingContent(
                    spendingByCategory = spendingByCategory,
                    subcategorySpending = subcategorySpending,
                    isBreakdownMode = isBreakdownMode,
                    selectedCategory = selectedCategory,
                    selectedSubcategory = selectedSubcategory,
                    transactions = spendingTransactions,
                    allAccounts = allAccounts,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    onSubcategorySelected = { viewModel.selectSubcategory(it) },
                    onToggleBreakdown = { viewModel.toggleBreakdownMode() },
                    onTransactionClick = { onNavigateToEditTransaction(it.accountId, it.id) },
                    onCategoryItemClick = { transaction ->
                        showCategoryPickerForTransaction = transaction
                    },
                    onSeeMoreClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
                2 -> BalanceTabContent(
                    balanceTrend = balanceTrend,
                    accountsWithBalance = accountsWithBalance,
                    selectedAccountIds = selectedAccountIds
                )
            }
        }
    }
}

@Composable
fun BalanceTabContent(
    balanceTrend: List<Pair<Long, Double>>,
    accountsWithBalance: List<AccountWithBalance>,
    selectedAccountIds: Set<Long>
) {
    val effectiveAccounts = if (selectedAccountIds.isEmpty()) accountsWithBalance 
                             else accountsWithBalance.filter { it.account.id in selectedAccountIds }
    val totalBalance = effectiveAccounts.sumOf { it.balance }
    val sortedAccounts = effectiveAccounts.sortedByDescending { it.balance }
    val maxBalance = sortedAccounts.firstOrNull()?.balance ?: 1.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp) // Added bottom padding for filter card
    ) {
        item {
            BalanceTrendCard(
                title = "Balance Trend",
                todayBalance = totalBalance,
                trendData = balanceTrend
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Balance by accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    sortedAccounts.forEach { account ->
                        AccountBalanceBar(
                            account = account,
                            maxBalance = maxBalance
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AccountBalanceBar(account: AccountWithBalance, maxBalance: Double) {
    val ratio = if (maxBalance > 0) (account.balance / maxBalance).toFloat().coerceIn(0f, 1f) else 0f
    val color = try {
        Color(android.graphics.Color.parseColor(account.account.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(account.account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "₹ ${formatCurrency(account.balance)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search transactions...", color = onPrimary.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = onPrimary,
                    unfocusedTextColor = onPrimary
                ),
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onPrimary)
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = onPrimary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun TransactionHistoryContent(
    groupedTransactions: List<TransactionGroup>,
    allAccounts: List<com.solanki.myapplication.data.model.Account>,
    selectedTransactionId: Long?,
    onTransactionClick: (Transaction) -> Unit,
    onTransactionLongClick: (Transaction) -> Unit,
    onCategoryClick: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp), // Space for pull-up card
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groupedTransactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found.", color = Color.Gray)
                }
            }
        } else {
            groupedTransactions.forEach { group ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            TransactionGroupHeader(group)
                            group.transactions.forEachIndexed { index, transaction ->
                                val account = allAccounts.find { it.id == transaction.accountId }
                                TransactionItem(
                                    transaction = transaction,
                                    isSelected = transaction.id == selectedTransactionId,
                                    accountName = account?.name,
                                    onClick = { onTransactionClick(transaction) },
                                    onLongClick = { onTransactionLongClick(transaction) },
                                    onCategoryClick = { onCategoryClick(transaction) }
                                )
                                if (index < group.transactions.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionGroupHeader(group: TransactionGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = group.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (group.balance != null) {
                Text(
                    text = "Balance ₹ " + formatCurrency(group.balance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = "Σ -₹ " + formatCurrency(group.totalExpense),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySpendingContent(
    spendingByCategory: List<com.solanki.myapplication.data.model.CategorySum>,
    subcategorySpending: List<com.solanki.myapplication.data.model.CategorySum>,
    isBreakdownMode: Boolean,
    selectedCategory: String?,
    selectedSubcategory: String?,
    transactions: List<Transaction>,
    allAccounts: List<com.solanki.myapplication.data.model.Account>,
    onCategorySelected: (String?) -> Unit,
    onSubcategorySelected: (String?) -> Unit,
    onToggleBreakdown: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onCategoryItemClick: (Transaction) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    var showAllTransactions by remember { mutableStateOf(false) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val currentSpending = if (isBreakdownMode) subcategorySpending else spendingByCategory
    val totalSum = currentSpending.sumOf { it.total }
    
    val centerText = remember(totalSum, selectedCategory, selectedSubcategory, isBreakdownMode, currentSpending) {
        if (isBreakdownMode) {
            if (selectedSubcategory != null) {
                val amount = currentSpending.find { it.category == selectedSubcategory }?.total ?: 0.0
                "$selectedSubcategory\n₹ ${formatCurrency(amount)}"
            } else {
                "Breakdown\n₹ ${formatCurrency(totalSum)}"
            }
        } else if (selectedCategory != null) {
            val amount = spendingByCategory.find { it.category == selectedCategory }?.total ?: 0.0
            "$selectedCategory\n₹ ${formatCurrency(amount)}"
        } else {
            "Total\n₹ ${formatCurrency(totalSum)}"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(bottom = 100.dp) // Added bottom padding for filter card
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBreakdownMode) "$selectedCategory Breakdown" else "Spending by Categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isBreakdownMode) {
                                TextButton(onClick = onToggleBreakdown) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Collapse")
                                }
                            } else if (selectedCategory != null) {
                                Button(
                                    onClick = onToggleBreakdown,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("View Breakdown", fontSize = 12.sp)
                                }
                            }
                        }

                        AndroidView(
                            factory = { context ->
                                PieChart(context).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        600
                                    )
                                    description.isEnabled = false
                                    isDrawHoleEnabled = true
                                    holeRadius = 58f
                                    setHoleColor(android.graphics.Color.TRANSPARENT)
                                    legend.isEnabled = false
                                    setDrawEntryLabels(false)
                                    setCenterTextSize(14f)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            update = { pieChart ->

                                // 🔥 IMPORTANT: Listener moved here
                                pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                                        val entry = e as? PieEntry
                                        entry?.let {
                                            if (isBreakdownMode) {
                                                onSubcategorySelected(it.label)
                                            } else {
                                                onCategorySelected(it.label)
                                            }
                                        }
                                    }

                                    override fun onNothingSelected() {
                                        if (isBreakdownMode) {
                                            onSubcategorySelected(null)
                                        } else {
                                            onCategorySelected(null)
                                        }
                                    }
                                })

                                val entries = currentSpending.map {
                                    PieEntry(it.total.toFloat(), it.category)
                                }

                                val dataSet = PieDataSet(entries, "").apply {
                                    if (isBreakdownMode) {
                                        val baseColor =
                                            CATEGORY_DATA.find { it.name == selectedCategory }?.color ?: Color.Gray

                                        colors = currentSpending.indices.map { i ->
                                            getShadedColor(baseColor, i, currentSpending.size).toArgb()
                                        }
                                    } else {
                                        colors = currentSpending.map { item ->
                                            val color =
                                                CATEGORY_DATA.find { it.name == item.category }?.color ?: Color.Gray
                                            color.toArgb()
                                        }
                                    }

                                    valueTextColor = android.graphics.Color.WHITE
                                    valueTextSize = 12f
                                    setDrawValues(false)
                                }

                                pieChart.data = PieData(dataSet)
                                pieChart.centerText = centerText
                                pieChart.setCenterTextColor(onSurfaceColor.toArgb())

                                val targetSelected =
                                    if (isBreakdownMode) selectedSubcategory else selectedCategory

                                if (targetSelected != null) {
                                    val index =
                                        currentSpending.indexOfFirst { it.category == targetSelected }

                                    if (index != -1) {
                                        pieChart.highlightValue(index.toFloat(), 0)
                                    } else {
                                        pieChart.highlightValue(null)
                                    }
                                } else {
                                    pieChart.highlightValue(null)
                                }

                                pieChart.invalidate()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            currentSpending.forEachIndexed { index, item ->
                                val catColor = if (isBreakdownMode) {
                                    val baseColor = CATEGORY_DATA.find { it.name == selectedCategory }?.color ?: Color.Gray
                                    getShadedColor(baseColor, index, currentSpending.size)
                                } else {
                                    getCategoryIconAndColor(item.category).second
                                }
                                
                                val isSelected = if (isBreakdownMode) selectedSubcategory == item.category else selectedCategory == item.category
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        if (isBreakdownMode) {
                                            if (selectedSubcategory == item.category) onSubcategorySelected(null)
                                            else onSubcategorySelected(item.category)
                                        } else {
                                            if (selectedCategory == item.category) onCategorySelected(null)
                                            else onCategorySelected(item.category)
                                        }
                                    }
                                ) {
                                    val targetSelected = if (isBreakdownMode) selectedSubcategory else selectedCategory
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (targetSelected == null || isSelected) catColor else catColor.copy(alpha = 0.3f))
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = item.category,
                                        fontSize = 12.sp,
                                        color = if (targetSelected == null || isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        val displayTransactions = if (showAllTransactions) transactions else transactions.take(5)
        
        if (displayTransactions.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    val titleText = if (isBreakdownMode && selectedSubcategory != null) {
                        "Transactions in $selectedSubcategory"
                    } else if (selectedCategory != null) {
                        "Top Transactions in $selectedCategory"
                    } else {
                        "Top Transactions"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            
            items(displayTransactions) { transaction ->
                val account = allAccounts.find { it.id == transaction.accountId }
                Surface(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        TransactionItem(
                            transaction,
                            accountName = account?.name,
                            onClick = { onTransactionClick(transaction) },
                            onCategoryClick = { onCategoryItemClick(transaction) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            
            if (!showAllTransactions && transactions.size > 5) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    ) {
                        TextButton(
                            onClick = { showAllTransactions = true },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text("See More")
                        }
                    }
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Text("No transactions found.", color = Color.Gray)
                }
            }
        }
    }
}

fun getShadedColor(baseColor: Color, index: Int, total: Int): Color {
    if (total <= 1) return baseColor
    val factor = 1.0f - (index.toFloat() / total.toFloat() * 0.6f)
    return Color(
        red = baseColor.red * factor,
        green = baseColor.green * factor,
        blue = baseColor.blue * factor,
        alpha = baseColor.alpha
    )
}

@Composable
fun EmptyTabContent(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title + " analysis coming soon", color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullUpFilterCard(
    currentTimeFilter: TimeFilter,
    customRange: Pair<Long, Long>?,
    selectedAccountIds: Set<Long>,
    allAccounts: List<com.solanki.myapplication.data.model.Account>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onTimeFilterSelected: (TimeFilter) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onAccountSelectorClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pull-up handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }

            // Always visible Time Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { page ->
                val filters = when(page) {
                    0 -> listOf(TimeFilter.LAST_7_DAYS, TimeFilter.LAST_14_DAYS, TimeFilter.LAST_30_DAYS, TimeFilter.LAST_12_WEEKS)
                    1 -> listOf(TimeFilter.THREE_MONTHS, TimeFilter.SIX_MONTHS, TimeFilter.ONE_YEAR, TimeFilter.TWO_YEARS)
                    else -> emptyList()
                }

                if (page < 2) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filters.forEach { filter ->
                            FilterChip(
                                selected = currentTimeFilter == filter,
                                onClick = { onTimeFilterSelected(filter) },
                                label = { Text(filter.label, fontSize = 13.sp) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                } else {
                    // Custom Filter Page - Row with start - end
                    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTimeFilterSelected(TimeFilter.CUSTOM); onStartDateClick() }
                                .border(1.dp, if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Text(
                                text = customRange?.first?.let { sdf.format(Date(it)) } ?: "Select Start",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Box(modifier = Modifier.padding(horizontal = 12.dp).width(16.dp).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)))
                        
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTimeFilterSelected(TimeFilter.CUSTOM); onEndDateClick() }
                                .border(1.dp, if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Text(
                                text = customRange?.second?.let { 
                                    val todayStr = sdf.format(Date())
                                    val endStr = sdf.format(Date(it))
                                    if(todayStr == endStr) "Today" else endStr
                                } ?: "Today",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if(currentTimeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Small Page Indicator dots
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(5.dp)
                    )
                }
            }

            // Hidden Account part
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("SELECT ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                    
                    val selectedAccountText = if (selectedAccountIds.isEmpty()) "All Accounts"
                                             else allAccounts.filter { it.id in selectedAccountIds }.joinToString { it.name }
                    
                    OutlinedCard(
                        onClick = onAccountSelectorClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAccountText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
