package com.spendsense.domain.repository

import com.spendsense.domain.model.DailyAggregation
import com.spendsense.domain.model.MonthlyCategoryAggregation
import com.spendsense.domain.model.MonthlyPaymentAggregation
import com.spendsense.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface AggregationRepository {
    fun getDailySpendingLastNDaysFlow(limit: Int): Flow<List<DailyAggregation>>
    fun getDailySpendingInRangeFlow(startDateKey: String, endDateKey: String): Flow<List<DailyAggregation>>
    fun getDailySpendingForMonthFlow(year: Int, month: Int): Flow<List<DailyAggregation>>
    fun getMonthlyCategoriesFlow(year: Int, month: Int): Flow<List<MonthlyCategoryAggregation>>
    fun getMonthlyPaymentsFlow(year: Int, month: Int): Flow<List<MonthlyPaymentAggregation>>
    fun getAllMonthlyPaymentsFlow(): Flow<List<MonthlyPaymentAggregation>>

    suspend fun recordTransactionInserted(transaction: Transaction)
    suspend fun recordTransactionDeleted(transaction: Transaction)
    suspend fun recordTransactionUpdated(oldTransaction: Transaction, newTransaction: Transaction)
    suspend fun ensureBackfillCompleted()
    suspend fun rebuildAllAggregations(transactions: List<Transaction>)
}
