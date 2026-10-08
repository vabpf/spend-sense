package com.spendsense.data.repository

import com.spendsense.data.local.dao.AggregationDao
import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.data.local.entity.DailySpendingAggregationEntity
import com.spendsense.data.local.entity.MonthlyCategoryAggregationEntity
import com.spendsense.data.local.entity.MonthlyPaymentAggregationEntity
import com.spendsense.domain.model.DailyAggregation
import com.spendsense.domain.model.MonthlyCategoryAggregation
import com.spendsense.domain.model.MonthlyPaymentAggregation
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.AggregationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AggregationRepositoryImpl @Inject constructor(
    private val aggregationDao: AggregationDao,
    private val transactionDao: TransactionDao
) : AggregationRepository {

    override fun getDailySpendingLastNDaysFlow(limit: Int): Flow<List<DailyAggregation>> {
        return aggregationDao.getDailySpendingLastNDaysFlow(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDailySpendingInRangeFlow(startDateKey: String, endDateKey: String): Flow<List<DailyAggregation>> {
        return aggregationDao.getDailySpendingInRangeFlow(startDateKey, endDateKey).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDailySpendingForMonthFlow(year: Int, month: Int): Flow<List<DailyAggregation>> {
        return aggregationDao.getDailySpendingForMonthFlow(year, month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMonthlyCategoriesFlow(year: Int, month: Int): Flow<List<MonthlyCategoryAggregation>> {
        return aggregationDao.getMonthlyCategoriesFlow(year, month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMonthlyPaymentsFlow(year: Int, month: Int): Flow<List<MonthlyPaymentAggregation>> {
        return aggregationDao.getMonthlyPaymentsFlow(year, month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllMonthlyPaymentsFlow(): Flow<List<MonthlyPaymentAggregation>> {
        return aggregationDao.getAllMonthlyPaymentsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordTransactionInserted(transaction: Transaction) = withContext(Dispatchers.IO) {
        val (dateKey, cal, dayStart) = extractDateInfo(transaction.timestamp)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // 1. Daily Aggregation
        val existingDaily = aggregationDao.getDailyByDateKey(dateKey)
        if (existingDaily != null) {
            aggregationDao.upsertDaily(
                existingDaily.copy(
                    totalAmount = existingDaily.totalAmount + transaction.amount,
                    transactionCount = existingDaily.transactionCount + 1
                )
            )
        } else {
            aggregationDao.upsertDaily(
                DailySpendingAggregationEntity(
                    dateKey = dateKey,
                    year = year,
                    month = month,
                    day = day,
                    timestampDayStart = dayStart,
                    totalAmount = transaction.amount,
                    transactionCount = 1
                )
            )
        }

        // 2. Monthly Category Aggregation
        val existingCat = aggregationDao.getMonthlyCategory(year, month, transaction.categoryId)
        if (existingCat != null) {
            aggregationDao.upsertCategory(
                existingCat.copy(
                    totalAmount = existingCat.totalAmount + transaction.amount,
                    transactionCount = existingCat.transactionCount + 1
                )
            )
        } else {
            aggregationDao.upsertCategory(
                MonthlyCategoryAggregationEntity(
                    year = year,
                    month = month,
                    categoryId = transaction.categoryId,
                    totalAmount = transaction.amount,
                    transactionCount = 1
                )
            )
        }

        // 3. Monthly Payment Source Aggregation
        val source = transaction.paymentSource.trim().ifBlank { "Manual" }
        val sourceType = transaction.paymentSourceType.trim().ifBlank { "Manual" }
        val existingPay = aggregationDao.getMonthlyPayment(year, month, source, sourceType)
        if (existingPay != null) {
            aggregationDao.upsertPayment(
                existingPay.copy(
                    totalAmount = existingPay.totalAmount + transaction.amount,
                    transactionCount = existingPay.transactionCount + 1
                )
            )
        } else {
            aggregationDao.upsertPayment(
                MonthlyPaymentAggregationEntity(
                    year = year,
                    month = month,
                    paymentSource = source,
                    paymentSourceType = sourceType,
                    totalAmount = transaction.amount,
                    transactionCount = 1
                )
            )
        }
    }

    override suspend fun recordTransactionDeleted(transaction: Transaction) = withContext(Dispatchers.IO) {
        val (dateKey, cal, _) = extractDateInfo(transaction.timestamp)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        // 1. Daily
        val existingDaily = aggregationDao.getDailyByDateKey(dateKey)
        if (existingDaily != null) {
            val newCount = existingDaily.transactionCount - 1
            val newTotal = (existingDaily.totalAmount - transaction.amount).coerceAtLeast(0.0)
            if (newCount <= 0 || newTotal <= 0.0) {
                aggregationDao.deleteDaily(dateKey)
            } else {
                aggregationDao.upsertDaily(existingDaily.copy(totalAmount = newTotal, transactionCount = newCount))
            }
        }

        // 2. Category
        val existingCat = aggregationDao.getMonthlyCategory(year, month, transaction.categoryId)
        if (existingCat != null) {
            val newCount = existingCat.transactionCount - 1
            val newTotal = (existingCat.totalAmount - transaction.amount).coerceAtLeast(0.0)
            if (newCount <= 0 || newTotal <= 0.0) {
                aggregationDao.deleteCategory(year, month, transaction.categoryId)
            } else {
                aggregationDao.upsertCategory(existingCat.copy(totalAmount = newTotal, transactionCount = newCount))
            }
        }

        // 3. Payment
        val source = transaction.paymentSource.trim().ifBlank { "Manual" }
        val sourceType = transaction.paymentSourceType.trim().ifBlank { "Manual" }
        val existingPay = aggregationDao.getMonthlyPayment(year, month, source, sourceType)
        if (existingPay != null) {
            val newCount = existingPay.transactionCount - 1
            val newTotal = (existingPay.totalAmount - transaction.amount).coerceAtLeast(0.0)
            if (newCount <= 0 || newTotal <= 0.0) {
                aggregationDao.deletePayment(year, month, source, sourceType)
            } else {
                aggregationDao.upsertPayment(existingPay.copy(totalAmount = newTotal, transactionCount = newCount))
            }
        }
    }

    override suspend fun recordTransactionUpdated(
        oldTransaction: Transaction,
        newTransaction: Transaction
    ) = withContext(Dispatchers.IO) {
        val (oldDateKey, oldCal, _) = extractDateInfo(oldTransaction.timestamp)
        val (newDateKey, newCal, _) = extractDateInfo(newTransaction.timestamp)

        val sameDate = oldDateKey == newDateKey
        val sameCategory = oldTransaction.categoryId == newTransaction.categoryId
        val samePayment = oldTransaction.paymentSource == newTransaction.paymentSource &&
                oldTransaction.paymentSourceType == newTransaction.paymentSourceType

        if (sameDate && sameCategory && samePayment) {
            // Fast delta path
            val deltaAmount = newTransaction.amount - oldTransaction.amount
            if (deltaAmount == 0.0) return@withContext

            val year = oldCal.get(Calendar.YEAR)
            val month = oldCal.get(Calendar.MONTH)

            val existingDaily = aggregationDao.getDailyByDateKey(oldDateKey)
            if (existingDaily != null) {
                aggregationDao.upsertDaily(
                    existingDaily.copy(
                        totalAmount = (existingDaily.totalAmount + deltaAmount).coerceAtLeast(0.0)
                    )
                )
            }

            val existingCat = aggregationDao.getMonthlyCategory(year, month, oldTransaction.categoryId)
            if (existingCat != null) {
                aggregationDao.upsertCategory(
                    existingCat.copy(
                        totalAmount = (existingCat.totalAmount + deltaAmount).coerceAtLeast(0.0)
                    )
                )
            }

            val source = oldTransaction.paymentSource.trim().ifBlank { "Manual" }
            val sourceType = oldTransaction.paymentSourceType.trim().ifBlank { "Manual" }
            val existingPay = aggregationDao.getMonthlyPayment(year, month, source, sourceType)
            if (existingPay != null) {
                aggregationDao.upsertPayment(
                    existingPay.copy(
                        totalAmount = (existingPay.totalAmount + deltaAmount).coerceAtLeast(0.0)
                    )
                )
            }
        } else {
            // Properties changed: decrement old and increment new
            recordTransactionDeleted(oldTransaction)
            recordTransactionInserted(newTransaction)
        }
    }

    override suspend fun ensureBackfillCompleted() = withContext(Dispatchers.IO) {
        val count = aggregationDao.getDailyAggregationCount()
        if (count == 0) {
            val allEntities = transactionDao.getAll()
            if (allEntities.isNotEmpty()) {
                val transactions = allEntities.map { entity ->
                    Transaction(
                        id = entity.id,
                        amount = entity.amount,
                        currencyCode = entity.currencyCode,
                        merchant = entity.merchant,
                        categoryId = entity.categoryId,
                        timestamp = entity.timestamp,
                        sourcePackageName = entity.sourcePackageName,
                        sourceAppName = entity.sourceAppName,
                        notes = entity.notes,
                        paymentSource = entity.paymentSource,
                        paymentSourceType = entity.paymentSourceType,
                        patternId = entity.patternId
                    )
                }
                rebuildAllAggregations(transactions)
            }
        }
    }

    override suspend fun rebuildAllAggregations(transactions: List<Transaction>) = withContext(Dispatchers.IO) {
        aggregationDao.clearAllAggregations()
        if (transactions.isEmpty()) return@withContext

        // High-performance native batch rollup when available
        val nativeRollup = com.spendsense.core.SpendSenseCore.aggregateBatch(transactions)
        if (nativeRollup != null) {
            aggregationDao.upsertDailyAll(nativeRollup.daily.map {
                DailySpendingAggregationEntity(
                    dateKey = it.dateKey,
                    year = it.year,
                    month = it.month,
                    day = it.day,
                    timestampDayStart = it.timestampDayStart,
                    totalAmount = it.totalAmount,
                    transactionCount = it.transactionCount
                )
            })
            aggregationDao.upsertCategoryAll(nativeRollup.categories.map {
                MonthlyCategoryAggregationEntity(
                    year = it.year,
                    month = it.month,
                    categoryId = it.categoryId,
                    totalAmount = it.totalAmount,
                    transactionCount = it.transactionCount
                )
            })
            aggregationDao.upsertPaymentAll(nativeRollup.payments.map {
                MonthlyPaymentAggregationEntity(
                    year = it.year,
                    month = it.month,
                    paymentSource = it.paymentSource,
                    paymentSourceType = it.paymentSourceType,
                    totalAmount = it.totalAmount,
                    transactionCount = it.transactionCount
                )
            })
            return@withContext
        }

        // 1. Group daily
        val dailyMap = mutableMapOf<String, DailySpendingAggregationEntity>()
        // 2. Group monthly category
        val categoryMap = mutableMapOf<Triple<Int, Int, Long>, MonthlyCategoryAggregationEntity>()
        // 3. Group monthly payment
        val paymentMap = mutableMapOf<Pair<Pair<Int, Int>, Pair<String, String>>, MonthlyPaymentAggregationEntity>()

        for (txn in transactions) {
            val (dateKey, cal, dayStart) = extractDateInfo(txn.timestamp)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            // Daily
            val curDaily = dailyMap[dateKey]
            if (curDaily != null) {
                dailyMap[dateKey] = curDaily.copy(
                    totalAmount = curDaily.totalAmount + txn.amount,
                    transactionCount = curDaily.transactionCount + 1
                )
            } else {
                dailyMap[dateKey] = DailySpendingAggregationEntity(
                    dateKey = dateKey,
                    year = year,
                    month = month,
                    day = day,
                    timestampDayStart = dayStart,
                    totalAmount = txn.amount,
                    transactionCount = 1
                )
            }

            // Category
            val catKey = Triple(year, month, txn.categoryId)
            val curCat = categoryMap[catKey]
            if (curCat != null) {
                categoryMap[catKey] = curCat.copy(
                    totalAmount = curCat.totalAmount + txn.amount,
                    transactionCount = curCat.transactionCount + 1
                )
            } else {
                categoryMap[catKey] = MonthlyCategoryAggregationEntity(
                    year = year,
                    month = month,
                    categoryId = txn.categoryId,
                    totalAmount = txn.amount,
                    transactionCount = 1
                )
            }

            // Payment
            val source = txn.paymentSource.trim().ifBlank { "Manual" }
            val sourceType = txn.paymentSourceType.trim().ifBlank { "Manual" }
            val payKey = Pair(Pair(year, month), Pair(source, sourceType))
            val curPay = paymentMap[payKey]
            if (curPay != null) {
                paymentMap[payKey] = curPay.copy(
                    totalAmount = curPay.totalAmount + txn.amount,
                    transactionCount = curPay.transactionCount + 1
                )
            } else {
                paymentMap[payKey] = MonthlyPaymentAggregationEntity(
                    year = year,
                    month = month,
                    paymentSource = source,
                    paymentSourceType = sourceType,
                    totalAmount = txn.amount,
                    transactionCount = 1
                )
            }
        }

        aggregationDao.upsertDailyAll(dailyMap.values.toList())
        aggregationDao.upsertCategoryAll(categoryMap.values.toList())
        aggregationDao.upsertPaymentAll(paymentMap.values.toList())
    }

    private fun extractDateInfo(timestamp: Long): Triple<String, Calendar, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        return Triple(dateKey, cal, cal.timeInMillis)
    }

    private fun DailySpendingAggregationEntity.toDomain() = DailyAggregation(
        dateKey = dateKey,
        year = year,
        month = month,
        day = day,
        timestampDayStart = timestampDayStart,
        totalAmount = totalAmount,
        transactionCount = transactionCount
    )

    private fun MonthlyCategoryAggregationEntity.toDomain() = MonthlyCategoryAggregation(
        year = year,
        month = month,
        categoryId = categoryId,
        totalAmount = totalAmount,
        transactionCount = transactionCount
    )

    private fun MonthlyPaymentAggregationEntity.toDomain() = MonthlyPaymentAggregation(
        year = year,
        month = month,
        paymentSource = paymentSource,
        paymentSourceType = paymentSourceType,
        totalAmount = totalAmount,
        transactionCount = transactionCount
    )
}
