package com.solanki.myapplication.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.ui.viewmodel.HomeViewModel
import com.solanki.myapplication.util.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAccountDetail: (Long) -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAccountList: () -> Unit,
    onNavigateToAddTransaction: (Long, Long) -> Unit,
    onNavigateToCategorySpending: (Long) -> Unit,
    onNavigateToAnalytics: (Long, Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accounts by viewModel.accountsWithBalance.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsState()
    val balanceTrend by viewModel.balanceTrend.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountWithBalance?>(null) }
    var showCategoryPickerForTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val dbExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { 
            scope.launch {
                // Checkpointing with FULL merges WAL into the main DB file.
                viewModel.checkpointDatabase()
                exportDatabase(context, it)
            }
        }
    }
    
    val dbImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            scope.launch {
                viewModel.closeDatabase()
                val success = importDatabase(context, it)
                if (success) {
                    restartApp(context)
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { scope.launch { exportTransactionsToCsv(context, it, transactions, accounts.map { it.account }) } }
    }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete " + accountToDelete?.account?.name + "? All its transactions will be removed.") } ,
            confirmButton = {
                TextButton(onClick = {
                    accountToDelete?.let { viewModel.deleteAccount(it.account) }
                    accountToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showCategoryPickerForTransaction != null) {
        CategoryPickerDialog(
            onCategorySelected = { newCategory: String ->
                viewModel.updateTransactionCategory(showCategoryPickerForTransaction!!, newCategory)
                showCategoryPickerForTransaction = null
            },
            onDismiss = { showCategoryPickerForTransaction = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pocket Wallet", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export DB") },
                            onClick = { 
                                showMenu = false
                                dbExportLauncher.launch("pocket_wallet_backup_" + System.currentTimeMillis() + ".db")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import DB") },
                            onClick = { 
                                showMenu = false
                                dbImportLauncher.launch(arrayOf("*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to CSV") },
                            onClick = { 
                                showMenu = false
                                csvExportLauncher.launch("transactions_" + System.currentTimeMillis() + ".csv")
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val firstSelectedId = selectedAccountIds.firstOrNull() ?: -1L
                onNavigateToAddTransaction(firstSelectedId, -1L)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                AccountGrid(
                    accounts = accounts,
                    selectedAccountIds = selectedAccountIds,
                    isSelectionMode = isSelectionMode,
                    onAccountClick = { id ->
                        if (isSelectionMode) {
                            viewModel.toggleAccountSelection(id)
                        } else {
                            viewModel.selectAllAccounts()
                            viewModel.toggleAccountSelection(id)
                        }
                    },
                    onAccountLongClick = { 
                        isSelectionMode = true
                        viewModel.toggleAccountSelection(it.account.id)
                    },
                    onSelectAllClick = { 
                        isSelectionMode = false
                        viewModel.selectAllAccounts() 
                    },
                    onAddAccountClick = onNavigateToAddAccount,
                    onSettingsClick = onNavigateToAccountList,
                    onExportCsvClick = {
                        csvExportLauncher.launch("transactions_" + System.currentTimeMillis() + ".csv")
                    }
                )
            }
            
            item {
                RecentTransactions(
                    transactions = transactions,
                    allAccounts = accounts,
                    onTransactionClick = { onNavigateToAddTransaction(it.accountId, it.id) },
                    onCategoryClick = { showCategoryPickerForTransaction = it },
                    onViewAllClick = { 
                        val firstId = selectedAccountIds.firstOrNull() ?: -1L
                        onNavigateToAccountDetail(firstId) 
                    }
                )
            }

            item {
                SpendingAnalysis(
                    data = spendingByCategory,
                    onGoDeeperClick = {
                        val firstId = selectedAccountIds.firstOrNull() ?: -1L
                        onNavigateToCategorySpending(firstId)
                    }
                )
            }

            item {
                val effectiveIds = if (selectedAccountIds.isEmpty()) accounts.map { it.account.id } else selectedAccountIds.toList()
                val totalBalance = accounts.filter { it.account.id in effectiveIds }.sumOf { it.balance }
                BalanceTrendCard(
                    title = "Balance Trend",
                    todayBalance = totalBalance,
                    trendData = balanceTrend,
                    onShowMoreClick = {
                        onNavigateToAnalytics(selectedAccountIds.firstOrNull() ?: -1L, 2)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountGrid(
    accounts: List<AccountWithBalance>,
    selectedAccountIds: Set<Long>,
    isSelectionMode: Boolean,
    onAccountClick: (Long) -> Unit,
    onAccountLongClick: (AccountWithBalance) -> Unit,
    onSelectAllClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportCsvClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("List of accounts", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onSettingsClick, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage Accounts", modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val chunkedAccounts = accounts.chunked(2)
            chunkedAccounts.forEach { rowAccounts ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowAccounts.forEach { account ->
                        SmallBankCard(
                            account = account,
                            isSelected = selectedAccountIds.isEmpty() || selectedAccountIds.contains(account.account.id),
                            onClick = { onAccountClick(account.account.id) },
                            onLongClick = { onAccountLongClick(account) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAccounts.size == 1) {
                        AddAccountCard(onAddAccountClick, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (accounts.isEmpty() || accounts.size % 2 == 0) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    AddAccountCard(onAddAccountClick, modifier = Modifier.weight(if (accounts.isEmpty()) 1f else 0.5f))
                    if (accounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelectAllClick) {
                    Text(if (isSelectionMode) "Done" else "Select all", color = MaterialTheme.colorScheme.primary)
                }
                
                IconButton(onClick = onExportCsvClick) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallBankCard(
    account: AccountWithBalance,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = try {
        Color(android.graphics.Color.parseColor(account.account.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier
            .height(72.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseColor
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), 
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                account.account.name, 
                fontWeight = FontWeight.Bold, 
                color = Color.White, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            val currencySymbol = if (account.account.currency == "INR") "₹" else account.account.currency
            val formattedBalance = formatCurrency(account.balance)
            Text(
                currencySymbol + " " + formattedBalance, 
                color = Color.White, 
                fontSize = 13.sp,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
fun AddAccountCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Add account", fontSize = 13.sp)
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendingAnalysis(
    data: List<CategorySum>,
    onGoDeeperClick: () -> Unit
) {
    val totalSum = data.sumOf { it.total }
    var selectedCategoryValue by remember(data) { mutableStateOf<Pair<String, Double>?>(null) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val aggregatedData = remember(data) {
        val map = mutableMapOf<String, Double>()
        data.forEach { item ->
            val mainCategory = CATEGORY_DATA.find { cat -> 
                cat.name.lowercase(Locale.getDefault()) == item.category.lowercase(Locale.getDefault()) || 
                cat.subcategories.any { sub -> sub.name.lowercase(Locale.getDefault()) == item.category.lowercase(Locale.getDefault()) }
            }?.name ?: "Other"
            map[mainCategory] = (map[mainCategory] ?: 0.0) + item.total
        }
        map.map { CategorySum(it.key, it.value) }.sortedByDescending { it.total }
    }

    val centerText = remember(totalSum, selectedCategoryValue) {
        if (selectedCategoryValue != null) {
            val formattedValue = formatCurrency(selectedCategoryValue!!.second)
            selectedCategoryValue!!.first + "\n₹ " + formattedValue
        } else {
            val formattedTotal = formatCurrency(totalSum)
            "Total \n₹ " + formattedTotal
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expenses structure", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onGoDeeperClick) {
                    Text("Go Deeper")
                }
            }

            if (aggregatedData.isNotEmpty()) {
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
                            setCenterTextColor(onSurfaceColor)
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    val entry = e as? PieEntry
                                    if (entry != null) {
                                        selectedCategoryValue = entry.label to entry.value.toDouble()
                                    }
                                }

                                override fun onNothingSelected() {
                                    selectedCategoryValue = null
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    update = { pieChart ->
                        val entries = aggregatedData.map { PieEntry(it.total.toFloat(), it.category) }
                        val dataSet = PieDataSet(entries, "").apply {
                            colors = aggregatedData.map { item ->
                                val color = CATEGORY_DATA.find { it.name == item.category }?.color ?: Color.Gray
                                android.graphics.Color.argb(
                                    (color.alpha * 255).toInt(),
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt()
                                )
                            }
                            valueTextColor = android.graphics.Color.WHITE
                            valueTextSize = 12f
                            setDrawValues(false)
                        }
                        pieChart.data = PieData(dataSet)
                        pieChart.centerText = centerText
                        pieChart.setCenterTextColor(onSurfaceColor)
                        pieChart.invalidate()
                    }
                )
            } else {
                Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    Text("No spending data available.", color = Color.Gray)
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                aggregatedData.forEach { item ->
                    val catColor = CATEGORY_DATA.find { it.name == item.category }?.color ?: Color.Gray
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(catColor))
                        Spacer(Modifier.width(4.dp))
                        Text(text = item.category, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactions(
    transactions: List<Transaction>,
    allAccounts: List<AccountWithBalance>,
    onTransactionClick: (Transaction) -> Unit,
    onCategoryClick: (Transaction) -> Unit,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transactions", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onViewAllClick) {
                    Text("See More")
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet.", color = Color.Gray)
                    }
                } else {
                    repeat(5) { index ->
                        if (index < transactions.size) {
                            val transaction = transactions[index]
                            val account = allAccounts.find { it.account.id == transaction.accountId }
                            TransactionItem(
                                transaction = transaction,
                                accountName = account?.account?.name,
                                onClick = { onTransactionClick(transaction) },
                                onCategoryClick = { onCategoryClick(transaction) }
                            )
                        } else {
                            TransactionPlaceholder()
                        }
                        if (index < 4) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
    }
}

private suspend fun exportDatabase(context: Context, uri: Uri) {
    withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("pocket_ledger_db")
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Database exported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

private suspend fun importDatabase(context: Context, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("pocket_ledger_db")
        val shmFile = File(dbFile.path + "-shm")
        val walFile = File(dbFile.path + "-wal")
        
        try {
            context.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            if (shmFile.exists()) shmFile.delete()
            if (walFile.exists()) walFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: " + e.message, Toast.LENGTH_LONG).show()
            }
            false
        }
    }
}

private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    exitProcess(0)
}
