package com.solanki.myapplication.data.repository

import com.solanki.myapplication.data.local.PocketLedgerDao
import com.solanki.myapplication.data.local.PocketLedgerDatabase
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.data.model.Template
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class PocketLedgerRepositoryImpl @Inject constructor(
    private val dao: PocketLedgerDao,
    private val database: PocketLedgerDatabase
) : PocketLedgerRepository {

    override fun getAllAccounts(): Flow<List<Account>> = dao.getAllAccounts()

    override suspend fun getAccountById(id: Long): Account? = dao.getAccountById(id)

    override suspend fun insertAccount(account: Account) {
        dao.insertAccount(account)
    }

    override suspend fun updateAccount(account: Account) {
        dao.updateAccount(account)
    }

    override suspend fun archiveAccount(id: Long) {
        dao.archiveAccount(id)
    }

    override suspend fun deleteAccount(account: Account) {
        dao.deleteAccount(account)
    }

    override fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAccount(accountId)

    override suspend fun getTransactionById(transactionId: Long): Transaction? =
        dao.getTransactionById(transactionId)

    override fun getRecentTransactionsForAccount(accountId: Long, limit: Int): Flow<List<Transaction>> =
        dao.getRecentTransactionsForAccount(accountId, limit)

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.updateTransaction(transaction)
    }

    override suspend fun archiveTransaction(id: Long) {
        dao.archiveTransaction(id)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteTransaction(transaction)
    }

    override fun getTotalIncomeForAccount(accountId: Long): Flow<Double?> =
        dao.getTotalIncomeForAccount(accountId)

    override fun getTotalExpenseForAccount(accountId: Long): Flow<Double?> =
        dao.getTotalExpenseForAccount(accountId)

    override fun getSpendingByCategory(since: Long, until: Long): Flow<List<CategorySum>> =
        dao.getSpendingByCategory(since, until)

    override fun getSpendingByCategoryForAccounts(
        accountIds: List<Long>,
        since: Long,
        until: Long
    ): Flow<List<CategorySum>> = dao.getSpendingByCategoryForAccounts(accountIds, since, until)

    override fun getAllTransactions(since: Long, until: Long): Flow<List<Transaction>> =
        dao.getAllTransactions(since, until)

    override fun getAllTransactionsForAccounts(
        accountIds: List<Long>,
        since: Long,
        until: Long
    ): Flow<List<Transaction>> = dao.getAllTransactionsForAccounts(accountIds, since, until)

    override fun getTransactionsForAnalysis(since: Long, until: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAnalysis(since, until)

    override fun getTransactionsForAnalysisForAccounts(
        accountIds: List<Long>,
        since: Long,
        until: Long
    ): Flow<List<Transaction>> = dao.getTransactionsForAnalysisForAccounts(accountIds, since, until)

    override fun getTopTransactionsByCategory(
        category: String,
        since: Long,
        until: Long
    ): Flow<List<Transaction>> = dao.getTopTransactionsByCategory(category, since, until)

    override fun getTopTransactionsByCategoryForAccounts(
        accountIds: List<Long>,
        category: String,
        since: Long,
        until: Long
    ): Flow<List<Transaction>> =
        dao.getTopTransactionsByCategoryForAccounts(accountIds, category, since, until)

    override fun getSpendingByCategoryForAccount(
        accountId: Long,
        since: Long,
        until: Long
    ): Flow<List<CategorySum>> = dao.getSpendingByCategoryForAccounts(listOf(accountId), since, until)

    override fun getBalanceTrendFlow(
        accountIds: List<Long>,
        since: Long,
        until: Long
    ): Flow<List<Pair<Long, Double>>> {
        return dao.getAllAccounts().combine(dao.getAllTransactions(0, until)) { accounts, allTransactions ->
            val selectedAccounts = if (accountIds.isEmpty()) accounts else accounts.filter { it.id in accountIds }
            val initialBalanceSum = selectedAccounts.sumOf { it.initialBalance }
            
            val filteredTransactions = allTransactions.filter { it.accountId in selectedAccounts.map { acc -> acc.id } }
            val sortedTrans = filteredTransactions.sortedBy { it.date }
            
            val trend = mutableListOf<Pair<Long, Double>>()
            var currentBalance = initialBalanceSum
            
            // Calculate balance before 'since' if applicable
            if (since > 0) {
                val balanceBeforeSince = initialBalanceSum + sortedTrans.filter { it.date < since }.sumOf { 
                    when (it.type) {
                        TransactionType.EXPENSE -> -it.amount
                        TransactionType.INCOME -> it.amount
                        else -> 0.0 // Explicitly ignore other types if they shouldn't affect balance
                    }
                }
                trend.add(since to balanceBeforeSince)
                currentBalance = balanceBeforeSince
            } else {
                trend.add(0L to initialBalanceSum)
            }

            sortedTrans.filter { it.date >= since }.forEach { trans ->
                currentBalance += when (trans.type) {
                    TransactionType.EXPENSE -> -trans.amount
                    TransactionType.INCOME -> trans.amount
                    else -> 0.0
                }
                trend.add(trans.date to currentBalance)
            }
            trend
        }
    }

    override fun getSpendingAnalysisFlow(
        accountIds: List<Long>,
        since: Long,
        until: Long
    ): Flow<List<CategorySum>> = dao.getSpendingByCategoryForAccounts(accountIds, since, until)

    override fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>> {
        return dao.getAllAccounts().combine(dao.getAllTransactions()) { accounts, transactions ->
            accounts.map { account ->
                val balanceChange = transactions
                    .filter { it.accountId == account.id && !it.isArchived }
                    .sumOf { 
                        when (it.type) {
                            TransactionType.EXPENSE -> -it.amount
                            TransactionType.INCOME -> it.amount
                            else -> 0.0
                        }
                    }
                AccountWithBalance(account, account.initialBalance + balanceChange)
            }
        }
    }

    override suspend fun checkpointDatabase() {
    }

    override suspend fun closeDatabase() {
        database.close()
    }

    override fun getAllTransactionNotes(): Flow<List<String>> = dao.getAllTransactionNotes()

    // Template implementation
    override fun getAllTemplates(): Flow<List<Template>> = dao.getAllTemplates()

    override suspend fun insertTemplate(template: Template) {
        dao.insertTemplate(template)
    }

    override suspend fun updateTemplate(template: Template) {
        dao.updateTemplate(template)
    }

    override suspend fun deleteTemplate(template: Template) {
        dao.deleteTemplate(template)
    }
}
