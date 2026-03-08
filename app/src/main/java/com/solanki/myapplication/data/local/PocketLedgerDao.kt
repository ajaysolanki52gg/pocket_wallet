package com.solanki.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PocketLedgerDao {

    // Account operations - Added ORDER BY orderIndex
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY orderIndex ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdSync(accountId: Long): Account?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :accountId")
    suspend fun archiveAccount(accountId: Long)

    @Delete
    suspend fun deleteAccount(account: Account)

    // Transaction operations
    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR :accountId = -1) AND isArchived = 0 ORDER BY date DESC, time DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR :accountId = -1) AND isArchived = 0 ORDER BY date DESC, time DESC LIMIT :limit")
    fun getRecentTransactionsForAccount(accountId: Long, limit: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET isArchived = 1 WHERE id = :transactionId")
    suspend fun archiveTransaction(transactionId: Long)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'INCOME' AND isArchived = 0")
    fun getTotalIncomeForAccount(accountId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'EXPENSE' AND isArchived = 0")
    fun getTotalExpenseForAccount(accountId: Long): Flow<Double?>

    // Analytics Queries - Improved date filtering logic to avoid "no data" when since/until are 0
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) GROUP BY category")
    fun getSpendingByCategory(since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<CategorySum>>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE accountId IN (:accountIds) AND type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) GROUP BY category")
    fun getSpendingByCategoryForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<CategorySum>>

    @Query("SELECT * FROM transactions WHERE isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY date DESC, time DESC")
    fun getAllTransactions(since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId IN (:accountIds) AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY date DESC, time DESC")
    fun getAllTransactionsForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY date DESC")
    fun getTransactionsForAnalysis(since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId IN (:accountIds) AND type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY date DESC")
    fun getTransactionsForAnalysisForAccounts(accountIds: List<Long>, since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category LIKE :category || '%' AND type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY amount DESC")
    fun getTopTransactionsByCategory(category: String, since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId IN (:accountIds) AND category LIKE :category || '%' AND type = 'EXPENSE' AND isArchived = 0 AND (:since = 0 OR date >= :since) AND (:until = 0 OR :until = 9223372036854775807 OR date <= :until) ORDER BY amount DESC")
    fun getTopTransactionsByCategoryForAccounts(accountIds: List<Long>, category: String, since: Long = 0L, until: Long = Long.MAX_VALUE): Flow<List<Transaction>>
}
