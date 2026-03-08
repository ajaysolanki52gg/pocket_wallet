package com.solanki.myapplication.util

import com.solanki.myapplication.data.model.CategorySum
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.TransactionType
import com.solanki.myapplication.domain.model.AccountWithBalance
import com.solanki.myapplication.ui.screen.CATEGORY_DATA
import java.util.*

object DataUtils {
    fun calculateBalanceTrend(
        selectedAccounts: List<AccountWithBalance>,
        allTransactions: List<Transaction>,
        since: Long,
        until: Long
    ): List<Pair<Long, Double>> {
        if (selectedAccounts.isEmpty()) return emptyList()

        val result = mutableListOf<Pair<Long, Double>>()
        val initialBalanceSum = selectedAccounts.sumOf { it.account.initialBalance }
        
        val startCal = Calendar.getInstance().apply { 
            timeInMillis = since
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = startCal.timeInMillis
        
        // 1. Calculate balance at the very start of the range
        val balanceAtStart = initialBalanceSum + allTransactions
            .filter { it.date < startTime }
            .sumOf { 
                when(it.type) {
                    TransactionType.INCOME -> it.amount
                    TransactionType.EXPENSE -> -it.amount
                    else -> 0.0
                }
            }
        result.add(startTime to balanceAtStart)

        // 2. Add points for each transaction within the range
        val transactionsInRange = allTransactions
            .filter { it.date >= startTime && it.date <= until }
            .sortedBy { it.date }

        var currentBalance = balanceAtStart
        transactionsInRange.forEach { transaction ->
            currentBalance += when(transaction.type) {
                TransactionType.INCOME -> transaction.amount
                TransactionType.EXPENSE -> -transaction.amount
                else -> 0.0
            }
            result.add(transaction.date to currentBalance)
        }

        // 3. Add a final point for 'now' if it's within range
        val now = System.currentTimeMillis()
        val effectiveUntil = if (until > now) now else until
        if (effectiveUntil > (result.lastOrNull()?.first ?: 0L)) {
            result.add(effectiveUntil to currentBalance)
        }
        
        // Ensure sorted and unique
        return result.sortedBy { it.first }.distinctBy { it.first }
    }

    fun groupSubcategoriesToMain(list: List<CategorySum>): List<CategorySum> {
        val grouped = mutableMapOf<String, Double>()
        list.forEach { item ->
            val mainCategory = findMainCategory(item.category)
            grouped[mainCategory] = (grouped[mainCategory] ?: 0.0) + item.total
        }
        return grouped.map { CategorySum(it.key, it.value) }.sortedByDescending { it.total }
    }

    fun findMainCategory(category: String): String {
        val cat = category.trim().lowercase(Locale.getDefault())
        if (cat.isEmpty()) return "Other"
        CATEGORY_DATA.forEach { mainCat ->
            if (mainCat.name.equals(cat, ignoreCase = true) || 
                mainCat.subcategories.any { it.name.equals(cat, ignoreCase = true) }) {
                return mainCat.name
            }
        }
        CATEGORY_DATA.forEach { mainCat ->
            val mainLower = mainCat.name.lowercase(Locale.getDefault())
            val stem = mainLower.removeSuffix("s")
            if (cat.contains(stem) || (stem.length >= 4 && mainLower.contains(cat))) {
                return mainCat.name
            }
        }
        return "Other"
    }
}
