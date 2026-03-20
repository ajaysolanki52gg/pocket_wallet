package com.solanki.myapplication.domain.repository

import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.Template
import com.solanki.myapplication.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow

interface PocketLedgerRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(id: Long)
    suspend fun deleteAccount(account: Account)

    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(transactionId: Long): Transaction?
    fun getRecentTransactionsForAccount(accountId: Long, limit: Int): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun archiveTransaction(id: Long)
    suspend fun deleteTransaction(transaction: Transaction)

    fun getTotalIncomeForAccount(accountId: Long): Flow<Double?>
    fun getTotalExpenseForAccount(accountId: Long): Flow<Double?>
    
    fun getSpendingByCategory(since: Long = 0L, until: Long = 0L): Flow<List<CategorySum>>
    fun getSpendingByCategoryForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = 0L): Flow<List<CategorySum>>
    
    fun getAllTransactions(since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>
    fun getAllTransactionsForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>

    fun getTransactionsForAnalysis(since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>
    fun getTransactionsForAnalysisForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>
    
    fun getTopTransactionsByCategory(category: String, since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>
    fun getTopTransactionsByCategoryForAccounts(accountIds: List<Long>, category: String, since: Long = 0L, until: Long = 0L): Flow<List<Transaction>>
    
    // Helper for multi-account or single account cases
    fun getSpendingByCategoryForAccount(accountId: Long, since: Long = 0L, until: Long = 0L): Flow<List<CategorySum>>

    // Unified Data Methods
    fun getBalanceTrendFlow(accountIds: List<Long>, since: Long, until: Long): Flow<List<Pair<Long, Double>>>
    fun getSpendingAnalysisFlow(accountIds: List<Long>, since: Long, until: Long): Flow<List<CategorySum>>
    fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>>

    suspend fun checkpointDatabase()
    suspend fun closeDatabase()

    fun getAllTransactionNotes(): Flow<List<String>>

    // Template operations
    fun getAllTemplates(): Flow<List<Template>>
    suspend fun insertTemplate(template: Template)
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(template: Template)
}
