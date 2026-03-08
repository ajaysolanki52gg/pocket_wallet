package com.solanki.myapplication.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.ui.theme.ExpenseRed
import com.solanki.myapplication.ui.theme.IncomeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

val indianLocale = Locale("en", "IN")

fun formatCurrency(amount: Double?): String {
    if (amount == null) return "0.00"
    val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount)
}

class IndianAmountTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        if (input.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = input.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInt = formatIndianInteger(integerPart)
        val out = formattedInt + decimalPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val originalIntOffset = if (offset > integerPart.length) integerPart.length else offset
                val transformedIntOffset = formatIndianInteger(integerPart.substring(0, originalIntOffset)).length
                
                return if (offset > integerPart.length) {
                    transformedIntOffset + (offset - integerPart.length)
                } else {
                    transformedIntOffset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0
                var transformedOffset = 0
                while (originalOffset < input.length && transformedOffset < offset) {
                    val char = out[transformedOffset]
                    if (char != ',') {
                        originalOffset++
                    }
                    transformedOffset++
                }
                return originalOffset
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }

    private fun formatIndianInteger(integerPart: String): String {
        if (integerPart.isEmpty()) return ""
        val reversed = integerPart.reversed()
        val result = StringBuilder()
        for (i in reversed.indices) {
            if (i == 3 || (i > 3 && (i - 3) % 2 == 0)) {
                result.append(",")
            }
            result.append(reversed[i])
        }
        var formatted = result.reverse().toString()
        if (formatted.startsWith(",")) formatted = formatted.substring(1)
        return formatted
    }
}

data class SubcategoryInfo(
    val name: String,
    val icon: ImageVector? = null
)

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val subcategories: List<SubcategoryInfo> = emptyList()
)

val CATEGORY_DATA = listOf(
    CategoryInfo("Food", Icons.Default.Restaurant, Color(0xFFFF5722), listOf(
        SubcategoryInfo("Bar, cafe", Icons.Default.LocalCafe),
        SubcategoryInfo("Restaurant", Icons.Default.Restaurant),
        SubcategoryInfo("Fast-food", Icons.Default.Fastfood),
        SubcategoryInfo("Snacks", Icons.Default.Cookie),
        SubcategoryInfo("Drinks", Icons.Default.LocalDrink)
    )),
    CategoryInfo("Groceries", Icons.Default.LocalGroceryStore, Color(0xFF4CAF50), listOf(
        SubcategoryInfo("Milk", Icons.Default.WaterDrop),
        SubcategoryInfo("Vegetables", Icons.Default.Grass),
        SubcategoryInfo("Fruits", Icons.Default.Kitchen),
        SubcategoryInfo("Bakery", Icons.Default.BakeryDining),
        SubcategoryInfo("Meat", Icons.Default.KebabDining)
    )),
    CategoryInfo("Shopping", Icons.Default.ShoppingBag, Color(0xFF2196F3), listOf(
        SubcategoryInfo("Clothing", Icons.Default.Checkroom),
        SubcategoryInfo("Footwear", Icons.Default.ShoppingBag),
        SubcategoryInfo("Electronics", Icons.Default.Computer),
        SubcategoryInfo("Accessories", Icons.Default.Watch),
        SubcategoryInfo("Home Decor", Icons.Default.Window),
        SubcategoryInfo("Gifts", Icons.Default.CardGiftcard)
    )),
    CategoryInfo("Money", Icons.Default.SwapHoriz, Color(0xFF2E7D32), listOf(
        SubcategoryInfo("Udhaar", Icons.Default.RequestQuote),
        SubcategoryInfo("Udhaar Vapis", Icons.Default.Paid),
        SubcategoryInfo("Money Sent", Icons.AutoMirrored.Filled.Send),
        SubcategoryInfo("Money Received", Icons.Default.Paid),
        SubcategoryInfo("MiddleMan", Icons.Default.SwapHoriz)
        )),
    CategoryInfo("Housing", Icons.Default.Home, Color(0xFFFF9800), listOf(
        SubcategoryInfo("Rent", Icons.Default.Key),
        SubcategoryInfo("Repair", Icons.Default.Build),
        SubcategoryInfo("Electricity", Icons.Default.Bolt),
        SubcategoryInfo("Water", Icons.Default.Water),
        SubcategoryInfo("Gas", Icons.Default.PropaneTank),
        SubcategoryInfo("Cleaning", Icons.Default.CleaningServices),
        SubcategoryInfo("Maintenance", Icons.Default.Handyman),
        SubcategoryInfo("Furniture", Icons.Default.Chair)
    )),
    CategoryInfo("Transportation", Icons.Default.DirectionsBus, Color(0xFF607D8B), listOf(
        SubcategoryInfo("Cab", Icons.Default.LocalTaxi),
        SubcategoryInfo("Train", Icons.Default.Train),
        SubcategoryInfo("Aeroplane", Icons.Default.Flight),
        SubcategoryInfo("Bus", Icons.Default.DirectionsBus),
        SubcategoryInfo("Fuel", Icons.Default.LocalGasStation),
        SubcategoryInfo("Parking", Icons.Default.LocalParking),
        SubcategoryInfo("Toll", Icons.Default.AddRoad)
    )),
    CategoryInfo("Vehicle", Icons.Default.DirectionsCar, Color(0xFF9C27B0), listOf(
        SubcategoryInfo("Maintenance", Icons.Default.Build),
        SubcategoryInfo("Insurance", Icons.Default.Policy),
        SubcategoryInfo("Registration", Icons.Default.AppRegistration),
        SubcategoryInfo("Wash", Icons.Default.LocalCarWash)
    )),
    CategoryInfo("Life & Entertainment", Icons.Default.TheaterComedy, Color(0xFFE91E63), listOf(
        SubcategoryInfo("Movie", Icons.Default.Movie),
        SubcategoryInfo("Games", Icons.Default.SportsEsports),
        SubcategoryInfo("Concert", Icons.Default.MusicNote),
        SubcategoryInfo("Hobbies", Icons.Default.Brush),
        SubcategoryInfo("Party", Icons.Default.Celebration),
        SubcategoryInfo("Streaming", Icons.Default.LiveTv),
        SubcategoryInfo("Gym", Icons.Default.FitnessCenter)
    )),
    CategoryInfo("Communication, PC", Icons.Default.Devices, Color(0xFF03A9F4), listOf(
        SubcategoryInfo("Phone", Icons.Default.Smartphone),
        SubcategoryInfo("Internet", Icons.Default.Wifi),
        SubcategoryInfo("Software", Icons.Default.Code),
        SubcategoryInfo("Hardware", Icons.Default.Hardware),
        SubcategoryInfo("Mobile Recharge", Icons.Default.SettingsCell)
    )),
    CategoryInfo("Financial expenses", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFFF44336), listOf(
        SubcategoryInfo("Taxes", Icons.AutoMirrored.Filled.ReceiptLong),
        SubcategoryInfo("Insurance", Icons.Default.Shield),
        SubcategoryInfo("Bank Fees", Icons.Default.AccountBalanceWallet),
        SubcategoryInfo("Interest", Icons.Default.Percent),
        SubcategoryInfo("Advisory", Icons.Default.SupportAgent)
    )),
    CategoryInfo("Investments", Icons.Default.AccountBalance, Color(0xFF009688), listOf(
        SubcategoryInfo("Stocks", Icons.AutoMirrored.Filled.ShowChart),
        SubcategoryInfo("Mutual Funds", Icons.Default.PieChart),
        SubcategoryInfo("Real Estate", Icons.Default.LocationCity),
        SubcategoryInfo("Gold", Icons.Default.Savings),
        SubcategoryInfo("Crypto", Icons.Default.CurrencyBitcoin)
    )),
    CategoryInfo("Health", Icons.Default.MedicalServices, Color(0xFFF44336), listOf(
        SubcategoryInfo("Doctor", Icons.Default.Person),
        SubcategoryInfo("Medicine", Icons.Default.Medication),
        SubcategoryInfo("Insurance", Icons.Default.HealthAndSafety),
        SubcategoryInfo("Dental", Icons.Default.MedicalInformation),
        SubcategoryInfo("Eye Care", Icons.Default.Visibility)
    )),
    CategoryInfo("Education", Icons.Default.School, Color(0xFF3F51B5), listOf(
        SubcategoryInfo("Books", Icons.Default.Book),
        SubcategoryInfo("Courses", Icons.Default.School),
        SubcategoryInfo("Fees", Icons.Default.Payments),
        SubcategoryInfo("Stationery", Icons.Default.Edit)
    )),
    CategoryInfo("Salary", Icons.Default.Payments, Color(0xFF4CAF50), listOf(
        SubcategoryInfo("Main Job", Icons.Default.Work),
        SubcategoryInfo("Freelance", Icons.Default.Computer),
        SubcategoryInfo("Bonus", Icons.Default.Stars)
    )),
    CategoryInfo("Gift", Icons.Default.Redeem, Color(0xFFFF4081), listOf(
        SubcategoryInfo("Birthday", Icons.Default.Cake),
        SubcategoryInfo("Wedding", Icons.Default.Favorite),
        SubcategoryInfo("Anniversary", Icons.Default.Event)
    )),
    CategoryInfo("Charity", Icons.Default.VolunteerActivism, Color(0xFF03A9F9), listOf(
        SubcategoryInfo("Donation", Icons.Default.VolunteerActivism),
        SubcategoryInfo("Relief Fund", Icons.Default.Emergency),
        SubcategoryInfo("Temple", Icons.Default.Fort),
        SubcategoryInfo("NGO", Icons.Default.Public),
        SubcategoryInfo("Zakat", Icons.Default.Savings),
        SubcategoryInfo("Community Support", Icons.Default.Groups)
    )),
    CategoryInfo("Other", Icons.Default.Category, Color(0xFF9E9E9E))
)

@Composable
fun AccountSelectorDialog(
    allAccounts: List<Account>,
    selectedAccountIds: Set<Long>,
    onDismiss: () -> Unit,
    onToggleAccount: (Long) -> Unit,
    onSelectAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Accounts", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allAccounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleAccount(account.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedAccountIds.isEmpty() || selectedAccountIds.contains(account.id),
                                onCheckedChange = { onToggleAccount(account.id) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(account.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSelectAll) {
                        Text("Select All")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

suspend fun exportTransactionsToCsv(
    context: Context,
    uri: Uri,
    transactions: List<Transaction>,
    accounts: List<Account>
) {
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    // Header
                    writer.append("ID,Date,Time,Account,Title,Amount,Type,Category,Notes\n")
                    
                    // Rows
                    val sdf = SimpleDateFormat("yyyy-MM-dd,HH:mm:ss", Locale.getDefault())
                    transactions.forEach { transaction ->
                        val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "N/A"
                        val (date, time) = sdf.format(Date(transaction.date)).split(',')
                        writer.append(
                            "\"${transaction.id}\"," +
                            "\"$date\"," +
                            "\"$time\"," +
                            "\"${accountName.replace("\"", "\"\"")}\"," +
                            "\"${transaction.title.replace("\"", "\"\"")}\"," +
                            "${transaction.amount}," +
                            "\"${transaction.type}\"," +
                            "\"${transaction.category.replace("\"", "\"\"")}\"," +
                            "\"${transaction.notes.replace("\"", "\"\"")}\"\n"
                        )
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


@Composable
fun BalanceTrendCard(
    title: String,
    todayBalance: Double,
    trendData: List<Pair<Long, Double>>,
    onShowMoreClick: (() -> Unit)? = null
) {
    var selectedEntry by remember { mutableStateOf<Pair<Long, Double>?>(null) }
    var ticketX by remember { mutableStateOf(0f) }
    var ticketY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    LaunchedEffect(trendData) {
        selectedEntry = null
    }

    // Determine light/dark mode for dynamic coloring
    val isLight = MaterialTheme.colorScheme.surface.toArgb().let { 
        (it shr 16 and 0xFF) * 0.299 + (it shr 8 and 0xFF) * 0.587 + (it and 0xFF) * 0.114 > 128 
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = if (!isLight) Color(0xFF64B5F6) else primaryColor
    val labelColor = if (isLight) Color.Black else Color.White
    val gridColorInt = if (isLight) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = labelColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Today", fontSize = 12.sp, color = Color.Gray)
            Text(
                "₹ ${formatCurrency(todayBalance)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trendData.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AndroidView(
                        factory = { context ->
                            LineChart(context).apply {
                                description.isEnabled = false
                                legend.isEnabled = false
                                setTouchEnabled(true)
                                setScaleEnabled(false)
                                setPinchZoom(false)
                                setDrawGridBackground(false)
                                setNoDataText("Loading chart...")
                                
                                xAxis.apply {
                                    position = XAxis.XAxisPosition.BOTTOM
                                    setDrawGridLines(false)
                                    setDrawAxisLine(true)
                                    textColor = if(isLight) android.graphics.Color.BLACK else android.graphics.Color.GRAY
                                    valueFormatter = object : ValueFormatter() {
                                        private val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                        override fun getFormattedValue(value: Float): String {
                                            return sdf.format(Date(value.toLong()))
                                        }
                                    }
                                }
                                axisLeft.apply {
                                    setDrawGridLines(true)
                                    setDrawAxisLine(false)
                                    textColor = if(isLight) android.graphics.Color.BLACK else android.graphics.Color.GRAY
                                    gridColor = gridColorInt
                                    valueFormatter = object : ValueFormatter() {
                                        override fun getFormattedValue(value: Float): String {
                                            return when {
                                                value >= 1000000f || value <= -1000000f -> String.format(Locale.US, "%.1fM", value / 1000000f)
                                                value >= 1000f || value <= -1000f -> String.format(Locale.US, "%.0fk", value / 1000f)
                                                else -> value.toInt().toString()
                                            }
                                        }
                                    }
                                }
                                axisRight.isEnabled = false
                                
                                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                                        if (e != null && h != null) {
                                            selectedEntry = e.x.toLong() to e.y.toDouble()
                                            ticketX = h.xPx
                                            ticketY = h.yPx
                                        }
                                    }

                                    override fun onNothingSelected() {
                                        selectedEntry = null
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { lineChart ->
                            // Sort entries by X value as required by LineChart
                            val entries = trendData
                                .map { Entry(it.first.toFloat(), it.second.toFloat()) }
                                .sortedBy { it.x }

                            val dataSet = LineDataSet(entries, "Balance").apply {
                                color = lineColor.toArgb()
                                setDrawCircles(false)
                                setDrawCircleHole(false)
                                setDrawValues(false)
                                lineWidth = 2.5f
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawFilled(true)
                                fillColor = lineColor.toArgb()
                                fillAlpha = 35
                                highLightColor = if(isLight) android.graphics.Color.BLACK else android.graphics.Color.LTGRAY
                                setDrawHorizontalHighlightIndicator(false)
                            }
                            
                            lineChart.xAxis.textColor = if(isLight) android.graphics.Color.BLACK else android.graphics.Color.GRAY
                            lineChart.axisLeft.textColor = if(isLight) android.graphics.Color.BLACK else android.graphics.Color.GRAY
                            lineChart.axisLeft.gridColor = gridColorInt
                            
                            lineChart.data = LineData(dataSet)
                            lineChart.notifyDataSetChanged()
                            lineChart.invalidate()
                        }
                    )

                    // Dynamic Selection Ticket/Card
                    if (selectedEntry != null) {
                        val ticketXDp = with(density) { ticketX.toDp() }
                        val ticketYDp = with(density) { ticketY.toDp() }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .offset(x = ticketXDp - 45.dp, y = ticketYDp - 65.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedEntry!!.first)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "₹ ${formatCurrency(selectedEntry!!.second)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No trend data available", color = Color.Gray)
                }
            }

            if (onShowMoreClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = onShowMoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show more", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    isSelected: Boolean = false,
    accountName: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onCategoryClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        val (icon, color) = getCategoryIconAndColor(transaction.category)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .then(if (onCategoryClick != null) Modifier.clickable { onCategoryClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title.ifEmpty { transaction.category },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            if (!accountName.isNullOrEmpty()) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val amountColor = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
            val prefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
            Text(
                text = "$prefix ₹ ${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    val cat = category.trim()
    for (categoryInfo in CATEGORY_DATA) {
        if (categoryInfo.name.equals(cat, ignoreCase = true)) {
            return categoryInfo.icon to categoryInfo.color
        }
        val subMatch = categoryInfo.subcategories.find { it.name.equals(cat, ignoreCase = true) }
        if (subMatch != null) {
            return (subMatch.icon ?: categoryInfo.icon) to categoryInfo.color
        }
    }
    return Icons.Default.Category to Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerDialog(
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryInfo?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != null && searchQuery.isEmpty()) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = if (selectedCategory != null && searchQuery.isEmpty()) selectedCategory!!.name else "Select Category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        if (it.isNotEmpty()) selectedCategory = null
                    },
                    placeholder = { Text("Search categories...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                AnimatedContent(
                    targetState = selectedCategory to searchQuery,
                    transitionSpec = {
                        if (targetState.first != null && initialState.first == null) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "CategoryNavigation"
                ) { (currentCategory, query) ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (query.isNotEmpty()) {
                            // Search View: Flat list of matching main and subcategories
                            val filteredResults = mutableListOf<Triple<String, CategoryInfo, ImageVector>>()
                            CATEGORY_DATA.forEach { cat ->
                                if (cat.name.contains(query, ignoreCase = true)) {
                                    filteredResults.add(Triple(cat.name, cat, cat.icon))
                                }
                                cat.subcategories.forEach { sub ->
                                    if (sub.name.contains(query, ignoreCase = true)) {
                                        filteredResults.add(Triple(sub.name, cat, sub.icon ?: cat.icon))
                                    }
                                }
                            }

                            items(filteredResults) { (name, info, icon) ->
                                ListItem(
                                    headlineContent = { Text(name) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(info.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    modifier = Modifier.clickable { onCategorySelected(name) }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        } else if (currentCategory == null) {
                            // Main Categories View
                            items(CATEGORY_DATA) { category ->
                                val hasSubcategories = category.subcategories.isNotEmpty()
                                ListItem(
                                    headlineContent = { Text(category.name, fontWeight = FontWeight.Bold) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(category.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(category.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    trailingContent = {
                                        if (hasSubcategories) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "View subcategories")
                                        }
                                    },
                                    modifier = Modifier.clickable { 
                                        if (hasSubcategories) {
                                            selectedCategory = category
                                        } else {
                                            onCategorySelected(category.name)
                                        }
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        } else {
                            // Subcategories View for selectedCategory
                            item {
                                ListItem(
                                    headlineContent = { Text(currentCategory.name, fontWeight = FontWeight.Bold) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(currentCategory.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(currentCategory.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    supportingContent = { Text("Main Category", fontSize = 12.sp) },
                                    modifier = Modifier.clickable { onCategorySelected(currentCategory.name) }
                                )
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            }
                            items(currentCategory.subcategories) { sub ->
                                ListItem(
                                    headlineContent = { Text(sub.name) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(currentCategory.color.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(sub.icon ?: currentCategory.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    modifier = Modifier.clickable { onCategorySelected(sub.name) }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
