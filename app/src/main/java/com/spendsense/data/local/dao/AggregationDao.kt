package com.spendsense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.spendsense.data.local.entity.DailySpendingAggregationEntity
import com.spendsense.data.local.entity.MonthlyCategoryAggregationEntity
import com.spendsense.data.local.entity.MonthlyPaymentAggregationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AggregationDao {

    // ── Daily Aggregations ──────────────────────────────────────────────────
    @Query("SELECT * FROM daily_spending_aggregations ORDER BY dateKey DESC LIMIT :limit")
    fun getDailySpendingLastNDaysFlow(limit: Int): Flow<List<DailySpendingAggregationEntity>>

    @Query("SELECT * FROM daily_spending_aggregations WHERE dateKey BETWEEN :startDateKey AND :endDateKey ORDER BY dateKey ASC")
    fun getDailySpendingInRangeFlow(startDateKey: String, endDateKey: String): Flow<List<DailySpendingAggregationEntity>>

    @Query("SELECT * FROM daily_spending_aggregations WHERE year = :year AND month = :month ORDER BY day ASC")
    fun getDailySpendingForMonthFlow(year: Int, month: Int): Flow<List<DailySpendingAggregationEntity>>

    @Query("SELECT * FROM daily_spending_aggregations WHERE dateKey = :dateKey")
    suspend fun getDailyByDateKey(dateKey: String): DailySpendingAggregationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(entity: DailySpendingAggregationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyAll(entities: List<DailySpendingAggregationEntity>)

    @Query("DELETE FROM daily_spending_aggregations WHERE dateKey = :dateKey")
    suspend fun deleteDaily(dateKey: String)

    // ── Monthly Category Aggregations ───────────────────────────────────────
    @Query("SELECT * FROM monthly_category_aggregations WHERE year = :year AND month = :month ORDER BY totalAmount DESC")
    fun getMonthlyCategoriesFlow(year: Int, month: Int): Flow<List<MonthlyCategoryAggregationEntity>>

    @Query("SELECT * FROM monthly_category_aggregations WHERE year = :year AND month = :month AND categoryId = :categoryId")
    suspend fun getMonthlyCategory(year: Int, month: Int, categoryId: Long): MonthlyCategoryAggregationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(entity: MonthlyCategoryAggregationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryAll(entities: List<MonthlyCategoryAggregationEntity>)

    @Query("DELETE FROM monthly_category_aggregations WHERE year = :year AND month = :month AND categoryId = :categoryId")
    suspend fun deleteCategory(year: Int, month: Int, categoryId: Long)

    // ── Monthly Payment Aggregations ────────────────────────────────────────
    @Query("SELECT * FROM monthly_payment_aggregations WHERE year = :year AND month = :month ORDER BY totalAmount DESC")
    fun getMonthlyPaymentsFlow(year: Int, month: Int): Flow<List<MonthlyPaymentAggregationEntity>>

    @Query("SELECT * FROM monthly_payment_aggregations ORDER BY year DESC, month DESC")
    fun getAllMonthlyPaymentsFlow(): Flow<List<MonthlyPaymentAggregationEntity>>

    @Query("SELECT * FROM monthly_payment_aggregations WHERE year = :year AND month = :month AND paymentSource = :source AND paymentSourceType = :sourceType")
    suspend fun getMonthlyPayment(year: Int, month: Int, source: String, sourceType: String): MonthlyPaymentAggregationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(entity: MonthlyPaymentAggregationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentAll(entities: List<MonthlyPaymentAggregationEntity>)

    @Query("DELETE FROM monthly_payment_aggregations WHERE year = :year AND month = :month AND paymentSource = :source AND paymentSourceType = :sourceType")
    suspend fun deletePayment(year: Int, month: Int, source: String, sourceType: String)

    // ── Global Maintenance / Resync ─────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM daily_spending_aggregations")
    suspend fun getDailyAggregationCount(): Int

    @Transaction
    @Query("DELETE FROM daily_spending_aggregations")
    suspend fun clearDailyAggregations()

    @Transaction
    @Query("DELETE FROM monthly_category_aggregations")
    suspend fun clearCategoryAggregations()

    @Transaction
    @Query("DELETE FROM monthly_payment_aggregations")
    suspend fun clearPaymentAggregations()

    @Transaction
    suspend fun clearAllAggregations() {
        clearDailyAggregations()
        clearCategoryAggregations()
        clearPaymentAggregations()
    }
}
