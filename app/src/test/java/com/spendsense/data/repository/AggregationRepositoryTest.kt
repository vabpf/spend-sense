package com.spendsense.data.repository

import com.spendsense.data.local.dao.AggregationDao
import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.data.local.entity.DailySpendingAggregationEntity
import com.spendsense.data.local.entity.MonthlyCategoryAggregationEntity
import com.spendsense.data.local.entity.MonthlyPaymentAggregationEntity
import com.spendsense.data.local.entity.TransactionEntity
import com.spendsense.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AggregationRepositoryTest {

    private val dailyStorage = mutableMapOf<String, DailySpendingAggregationEntity>()
    private val categoryStorage = mutableMapOf<String, MonthlyCategoryAggregationEntity>()
    private val paymentStorage = mutableMapOf<String, MonthlyPaymentAggregationEntity>()

    private val fakeAggregationDao = object : AggregationDao {
        override fun getDailySpendingLastNDaysFlow(limit: Int): Flow<List<DailySpendingAggregationEntity>> =
            flowOf(dailyStorage.values.sortedByDescending { it.dateKey }.take(limit))

        override fun getDailySpendingInRangeFlow(startDateKey: String, endDateKey: String): Flow<List<DailySpendingAggregationEntity>> =
            flowOf(dailyStorage.values.filter { it.dateKey in startDateKey..endDateKey }.sortedBy { it.dateKey })

        override fun getDailySpendingForMonthFlow(year: Int, month: Int): Flow<List<DailySpendingAggregationEntity>> =
            flowOf(dailyStorage.values.filter { it.year == year && it.month == month }.sortedBy { it.day })

        override suspend fun getDailyByDateKey(dateKey: String): DailySpendingAggregationEntity? = dailyStorage[dateKey]

        override suspend fun upsertDaily(entity: DailySpendingAggregationEntity) {
            dailyStorage[entity.dateKey] = entity
        }

        override suspend fun upsertDailyAll(entities: List<DailySpendingAggregationEntity>) {
            entities.forEach { dailyStorage[it.dateKey] = it }
        }

        override suspend fun deleteDaily(dateKey: String) {
            dailyStorage.remove(dateKey)
        }

        override fun getMonthlyCategoriesFlow(year: Int, month: Int): Flow<List<MonthlyCategoryAggregationEntity>> =
            flowOf(categoryStorage.values.filter { it.year == year && it.month == month }.sortedByDescending { it.totalAmount })

        override suspend fun getMonthlyCategory(year: Int, month: Int, categoryId: Long): MonthlyCategoryAggregationEntity? =
            categoryStorage["$year-$month-$categoryId"]

        override suspend fun upsertCategory(entity: MonthlyCategoryAggregationEntity) {
            categoryStorage["${entity.year}-${entity.month}-${entity.categoryId}"] = entity
        }

        override suspend fun upsertCategoryAll(entities: List<MonthlyCategoryAggregationEntity>) {
            entities.forEach { categoryStorage["${it.year}-${it.month}-${it.categoryId}"] = it }
        }

        override suspend fun deleteCategory(year: Int, month: Int, categoryId: Long) {
            categoryStorage.remove("$year-$month-$categoryId")
        }

        override fun getMonthlyPaymentsFlow(year: Int, month: Int): Flow<List<MonthlyPaymentAggregationEntity>> =
            flowOf(paymentStorage.values.filter { it.year == year && it.month == month }.sortedByDescending { it.totalAmount })

        override fun getAllMonthlyPaymentsFlow(): Flow<List<MonthlyPaymentAggregationEntity>> =
            flowOf(paymentStorage.values.sortedWith(compareByDescending<MonthlyPaymentAggregationEntity> { it.year }.thenByDescending { it.month }))

        override suspend fun getMonthlyPayment(year: Int, month: Int, source: String, sourceType: String): MonthlyPaymentAggregationEntity? =
            paymentStorage["$year-$month-$source-$sourceType"]

        override suspend fun upsertPayment(entity: MonthlyPaymentAggregationEntity) {
            paymentStorage["${entity.year}-${entity.month}-${entity.paymentSource}-${entity.paymentSourceType}"] = entity
        }

        override suspend fun upsertPaymentAll(entities: List<MonthlyPaymentAggregationEntity>) {
            entities.forEach { paymentStorage["${it.year}-${it.month}-${it.paymentSource}-${it.paymentSourceType}"] = it }
        }

        override suspend fun deletePayment(year: Int, month: Int, source: String, sourceType: String) {
            paymentStorage.remove("$year-$month-$source-$sourceType")
        }

        override suspend fun getDailyAggregationCount(): Int = dailyStorage.size
        override suspend fun clearDailyAggregations() { dailyStorage.clear() }
        override suspend fun clearCategoryAggregations() { categoryStorage.clear() }
        override suspend fun clearPaymentAggregations() { paymentStorage.clear() }
    }

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insert(transaction: TransactionEntity): Long = 1L
        override suspend fun insertAll(transactions: List<TransactionEntity>) {}
        override suspend fun update(transaction: TransactionEntity) {}
        override suspend fun delete(transaction: TransactionEntity) {}
        override suspend fun getById(id: Long): TransactionEntity? = null
        override fun getAllFlow(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getAll(): List<TransactionEntity> = emptyList()
        override suspend fun deleteAll() {}
        override fun getByCategoryFlow(categoryId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getByDateRangeFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getUnsyncedTransactions(): List<TransactionEntity> = emptyList()
        override suspend fun markAsSynced(id: Long, firestoreId: String) {}
    }

    private lateinit var repository: AggregationRepositoryImpl

    @Before
    fun setUp() {
        dailyStorage.clear()
        categoryStorage.clear()
        paymentStorage.clear()
        repository = AggregationRepositoryImpl(fakeAggregationDao, fakeTransactionDao)
    }

    @Test
    fun testRecordTransactionInserted_incrementsDailyCategoryAndPayment() = runBlocking {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 8, 14, 30, 0)
        }
        val txn = Transaction(
            id = 1L,
            amount = 50.0,
            currencyCode = "USD",
            merchant = "Starbucks",
            categoryId = 10L,
            timestamp = cal.timeInMillis,
            sourcePackageName = "com.test",
            sourceAppName = "Test",
            paymentSource = "Chase",
            paymentSourceType = "Credit Card"
        )

        repository.recordTransactionInserted(txn)

        // Check daily
        val daily = fakeAggregationDao.getDailyByDateKey("2026-10-08")
        assertNotNull(daily)
        assertEquals(50.0, daily!!.totalAmount, 0.001)
        assertEquals(1, daily.transactionCount)

        // Check category (October = month index 9)
        val cat = fakeAggregationDao.getMonthlyCategory(2026, 9, 10L)
        assertNotNull(cat)
        assertEquals(50.0, cat!!.totalAmount, 0.001)
        assertEquals(1, cat.transactionCount)

        // Check payment
        val pay = fakeAggregationDao.getMonthlyPayment(2026, 9, "Chase", "Credit Card")
        assertNotNull(pay)
        assertEquals(50.0, pay!!.totalAmount, 0.001)
        assertEquals(1, pay.transactionCount)

        // Insert second transaction on same day & category
        val txn2 = txn.copy(id = 2L, amount = 25.0)
        repository.recordTransactionInserted(txn2)

        val updatedDaily = fakeAggregationDao.getDailyByDateKey("2026-10-08")
        assertEquals(75.0, updatedDaily!!.totalAmount, 0.001)
        assertEquals(2, updatedDaily.transactionCount)
    }

    @Test
    fun testRecordTransactionDeleted_decrementsEntriesProperly() = runBlocking {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 8, 10, 0, 0) }
        val txn = Transaction(
            id = 1L, amount = 100.0, currencyCode = "USD", merchant = "Store",
            categoryId = 5L, timestamp = cal.timeInMillis,
            sourcePackageName = "com.test", sourceAppName = "Test",
            paymentSource = "Debit", paymentSourceType = "Bank Account"
        )

        repository.recordTransactionInserted(txn)
        assertNotNull(fakeAggregationDao.getDailyByDateKey("2026-10-08"))

        repository.recordTransactionDeleted(txn)
        // Since count reached 0, entry is deleted
        assertNull(fakeAggregationDao.getDailyByDateKey("2026-10-08"))
        assertNull(fakeAggregationDao.getMonthlyCategory(2026, 9, 5L))
        assertNull(fakeAggregationDao.getMonthlyPayment(2026, 9, "Debit", "Bank Account"))
    }

    @Test
    fun testRecordTransactionUpdated_appliesDeltaAccurately() = runBlocking {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 8, 10, 0, 0) }
        val oldTxn = Transaction(
            id = 1L, amount = 50.0, currencyCode = "USD", merchant = "Store",
            categoryId = 5L, timestamp = cal.timeInMillis,
            sourcePackageName = "com.test", sourceAppName = "Test",
            paymentSource = "Debit", paymentSourceType = "Bank Account"
        )
        repository.recordTransactionInserted(oldTxn)

        val newTxn = oldTxn.copy(amount = 80.0)
        repository.recordTransactionUpdated(oldTxn, newTxn)

        val daily = fakeAggregationDao.getDailyByDateKey("2026-10-08")
        assertNotNull(daily)
        assertEquals(80.0, daily!!.totalAmount, 0.001)
        assertEquals(1, daily.transactionCount)
    }

    @Test
    fun testRebuildAllAggregations_processesBatchCorrectly() = runBlocking {
        val cal1 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1, 10, 0, 0) }
        val cal2 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 2, 10, 0, 0) }

        val txns = listOf(
            Transaction(1L, 30.0, "USD", "A", 1L, cal1.timeInMillis, "p", "a", "m", "Cash", "Cash"),
            Transaction(2L, 70.0, "USD", "B", 1L, cal1.timeInMillis, "p", "a", "m", "Cash", "Cash"),
            Transaction(3L, 40.0, "USD", "C", 2L, cal2.timeInMillis, "p", "a", "m", "Card", "Credit Card")
        )

        repository.rebuildAllAggregations(txns)

        val oct1 = fakeAggregationDao.getDailyByDateKey("2026-10-01")
        assertNotNull(oct1)
        assertEquals(100.0, oct1!!.totalAmount, 0.001)
        assertEquals(2, oct1.transactionCount)

        val oct2 = fakeAggregationDao.getDailyByDateKey("2026-10-02")
        assertNotNull(oct2)
        assertEquals(40.0, oct2!!.totalAmount, 0.001)
        assertEquals(1, oct2.transactionCount)

        val cat1 = fakeAggregationDao.getMonthlyCategory(2026, 9, 1L)
        assertNotNull(cat1)
        assertEquals(100.0, cat1!!.totalAmount, 0.001)

        val cat2 = fakeAggregationDao.getMonthlyCategory(2026, 9, 2L)
        assertNotNull(cat2)
        assertEquals(40.0, cat2!!.totalAmount, 0.001)
    }
}
