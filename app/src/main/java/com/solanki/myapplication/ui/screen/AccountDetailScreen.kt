package com.solanki.myapplication.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.ui.theme.ExpenseRed
import com.solanki.myapplication.ui.theme.IncomeGreen
import com.solanki.myapplication.ui.viewmodel.AccountDetailViewModel
import com.solanki.myapplication.ui.viewmodel.TransactionFilter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val account by viewModel.account.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val incomeTotal by viewModel.incomeTotal.collectAsState()
    val expenseTotal by viewModel.expenseTotal.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export to CSV") },
                            onClick = {
                                exportToCsv(context, transactions)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddTransaction(accountId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            account?.let { acc ->
                SummarySection(
                    balance = balance,
                    incomeTotal = incomeTotal,
                    expenseTotal = expenseTotal,
                    currency = acc.currency,
                    color = Color(android.graphics.Color.parseColor(acc.color))
                )
            }

            SearchBar(query = searchQuery, onQueryChange = viewModel::onSearchQueryChange)
            
            FilterChips(selectedFilter, viewModel::onFilterChange)

            TransactionListSection(transactions)
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search transactions...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true
    )
}

@Composable
fun FilterChips(selectedFilter: TransactionFilter, onFilterChange: (TransactionFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
fun SummarySection(
    balance: Double,
    incomeTotal: Double,
    expenseTotal: Double,
    currency: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Balance", color = Color.White.copy(alpha = 0.8f))
            Text(
                "$currency ${String.format(Locale.US, "%.2f", balance)}",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(label = "Income", amount = incomeTotal, currency = currency, color = Color.White)
                SummaryItem(label = "Expenses", amount = expenseTotal, currency = currency, color = Color.White)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, currency: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        Text(
            "$currency ${String.format(Locale.US, "%.2f", amount)}",
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun TransactionListSection(transactions: List<Transaction>) {
    val groupedTransactions = transactions.groupBy {
        val date = Date(it.date)
        val calendar = Calendar.getInstance()
        calendar.time = date
        val today = Calendar.getInstance()
        
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today"
        } else {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedTransactions.forEach { (date, items) ->
            item {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(items) { transaction ->
                TransactionCard(transaction)
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.DarkGray)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (transaction.notes.isNotEmpty()) {
                    Text(
                        transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
            
            val (text, color) = if (transaction.type == TransactionType.INCOME) {
                Pair("${transaction.currency} ${String.format(Locale.US, "%.2f", transaction.amount)}", IncomeGreen)
            } else {
                Pair("-${transaction.currency} ${String.format(Locale.US, "%.2f", transaction.amount)}", ExpenseRed)
            }
            
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun exportToCsv(context: Context, transactions: List<Transaction>) {
    val fileName = "transactions_${System.currentTimeMillis()}.csv"
    val file = File(context.cacheDir, fileName)
    
    try {
        FileWriter(file).use { writer ->
            writer.append("ID,Type,Amount,Category,Title,Notes,Date\n")
            transactions.forEach { trans ->
                writer.append("${trans.id},${trans.type},${trans.amount},${trans.category},${trans.title},${trans.notes},${trans.date}\n")
            }
        }
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Transactions"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
