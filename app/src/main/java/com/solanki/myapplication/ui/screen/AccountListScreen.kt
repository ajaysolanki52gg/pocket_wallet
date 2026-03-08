package com.solanki.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.ui.viewmodel.HomeViewModel
import java.util.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState()
    var list by remember { mutableStateOf(accountsWithBalance) }
    var showDeleteConfirm by remember { mutableStateOf<AccountWithBalance?>(null) }
    
    LaunchedEffect(accountsWithBalance) {
        list = accountsWithBalance
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        list = list.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.updateAccountOrder(list)
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete " + showDeleteConfirm!!.account.name + "? All its transactions will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(showDeleteConfirm!!.account)
                    showDeleteConfirm = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditAccount(-1L) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(list, key = { _, item -> item.account.id }) { index, accountWithBalance ->
                ReorderableItem(reorderableState, key = accountWithBalance.account.id) { isDragging ->
                    val elevation = if (isDragging) 8.dp else 0.dp
                    Surface(shadowElevation = elevation) {
                        AccountListItem(
                            accountWithBalance = accountWithBalance,
                            onClick = { onNavigateToEditAccount(accountWithBalance.account.id) },
                            onDeleteClick = { showDeleteConfirm = accountWithBalance },
                            modifier = Modifier.draggableHandle()
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun AccountListItem(
    accountWithBalance: AccountWithBalance,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = accountWithBalance.account
    val color = try {
        Color(android.graphics.Color.parseColor(account.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(account.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = { 
            val formattedBalance = String.format(Locale.US, "%.2f", accountWithBalance.balance)
            Text(
                text = account.currency + " " + formattedBalance,
                color = Color.Gray
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = Color.LightGray,
                    modifier = modifier
                )
            }
        }
    )
}
