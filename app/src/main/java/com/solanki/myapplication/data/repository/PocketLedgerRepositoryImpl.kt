package com.solanki.myapplication.data.repository

import com.solanki.myapplication.data.local.PocketLedgerDao
import com.solanki.myapplication.data.local.PocketLedgerDatabase
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import com.solanki.myapplication.util.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
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

    override suspend fun updateAccount(account: Account) = dao.updateAccount(account)

    override suspend fun archiveAccount(id: Long) = dao.archiveAccount(id)

    override suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    override fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAccount(accountId)

    override suspend fun getTransactionById(transactionId: Long): Transaction? =
        dao.getTransactionById(transactionId)

    override fun getRecentTransactionsForAccount(accountId: Long, limit: Int): Flow<List<Transaction>> =
        dao.getRecentTransactionsForAccount(accountId, limit)

    override suspend fun insertTransaction(transaction: Transaction) = dao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: Transaction) = dao.updateTransaction(transaction)

    override suspend fun archiveTransaction(id: Long) = dao.archiveTransaction(id)

    override suspend fun deleteTransaction(transaction: Transaction) = dao.deleteTransaction(transaction)

    override fun getTotalIncomeForAccount(accountId: Long): Flow<Double?> = dao.getTotalIncomeForAccount(accountId)

    override fun getTotalExpenseForAccount(accountId: Long): Flow<Double?> = dao.getTotalExpenseForAccount(accountId)

    override fun getSpendingByCategory(since: Long, until: Long): Flow<List<CategorySum>> = 
        dao.getSpendingByCategory(since, until)

    override fun getSpendingByCategoryForAccounts(accountIds: List<Long>, since: Long, until: Long): Flow<List<CategorySum>> = 
        dao.getSpendingByCategoryForAccounts(accountIds, since, until)

    override fun getAllTransactions(since: Long, until: Long): Flow<List<Transaction>> =
        dao.getAllTransactions(since, until)

    override fun getAllTransactionsForAccounts(accountIds: List<Long>, since: Long, until: Long): Flow<List<Transaction>> =
        dao.getAllTransactionsForAccounts(accountIds, since, until)

    override fun getTransactionsForAnalysis(since: Long, until: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAnalysis(since, until)

    override fun getTransactionsForAnalysisForAccounts(accountIds: List<Long>, since: Long, until: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAnalysisForAccounts(accountIds, since, until)

    override fun getTopTransactionsByCategory(category: String, since: Long, until: Long): Flow<List<Transaction>> =
        dao.getTopTransactionsByCategory(category, since, until)

    override fun getTopTransactionsByCategoryForAccounts(accountIds: List<Long>, category: String, since: Long, until: Long): Flow<List<Transaction>> =
        dao.getTopTransactionsByCategoryForAccounts(accountIds, category, since, until)

    override fun getSpendingByCategoryForAccount(accountId: Long, since: Long, until: Long): Flow<List<CategorySum>> {
        return if (accountId == -1L) dao.getSpendingByCategory(since, until)
        else dao.getSpendingByCategoryForAccounts(listOf(accountId), since, until)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getBalanceTrendFlow(accountIds: List<Long>, since: Long, until: Long): Flow<List<Pair<Long, Double>>> {
        return combine(
            getAccountsWithBalanceFlow(),
            dao.getAllTransactions(0, Long.MAX_VALUE)
        ) { accounts, allTransactions ->
            val ids = if (accountIds.isEmpty()) accounts.map { it.account.id } else accountIds
            DataUtils.calculateBalanceTrend(
                selectedAccounts = accounts.filter { it.account.id in ids },
                allTransactions = allTransactions.filter { it.accountId in ids },
                since = since,
                until = until
            )
        }
    }

    override fun getSpendingAnalysisFlow(accountIds: List<Long>, since: Long, until: Long): Flow<List<CategorySum>> {
        val rawSpending = if (accountIds.isEmpty()) {
            dao.getSpendingByCategory(since, until)
        } else {
            dao.getSpendingByCategoryForAccounts(accountIds, since, until)
        }
        return rawSpending.map { DataUtils.groupSubcategoriesToMain(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>> = dao.getAllAccounts()
        .flatMapLatest { accounts ->
            if (accounts.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val sortedAccounts = accounts.sortedBy { it.orderIndex }
            val flows = sortedAccounts.map { account ->
                combine(
                    dao.getTotalIncomeForAccount(account.id),
                    dao.getTotalExpenseForAccount(account.id)
                ) { income, expense ->
                    val balance = account.initialBalance + (income ?: 0.0) - (expense ?: 0.0)
                    AccountWithBalance(account, balance)
                }
            }
            combine(flows) { it.toList() }
        }

    override suspend fun checkpointDatabase() {
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            database.openHelper.writableDatabase.query("VACUUM").close()
        }
    }

    override suspend fun closeDatabase() {
        withContext(Dispatchers.IO) {
            if (database.isOpen) {
                database.close()
            }
        }
    }
}
