package com.spendsense.data.repository

import com.spendsense.data.local.dao.MerchantCategoryMappingDao
import com.spendsense.data.local.dao.NotificationPatternDao
import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.data.local.entity.TransactionEntity
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.AggregationRepository
import com.spendsense.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val notificationPatternDao: NotificationPatternDao,
    private val aggregationRepository: AggregationRepository
) : TransactionRepository {

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insert(transaction.toEntity())
        val savedTransaction = if (transaction.id == 0L) transaction.copy(id = id) else transaction
        merchantCategoryMappingDao.upsert(
            MerchantCategoryMappingEntity(
                merchant = transaction.merchant.lowercase(),
                categoryId = transaction.categoryId
            )
        )
        if (transaction.patternId != null) {
            val pattern = notificationPatternDao.getById(transaction.patternId)
            if (pattern != null) {
                notificationPatternDao.upsert(pattern.copy(
                    matchCount = pattern.matchCount + 1
                ))
            }
        }
        aggregationRepository.recordTransactionInserted(savedTransaction)
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val oldEntity = transactionDao.getById(transaction.id)
        val oldDomain = oldEntity?.toDomain()
        transactionDao.update(transaction.toEntity())
        if (oldDomain != null) {
            aggregationRepository.recordTransactionUpdated(oldDomain, transaction)
        } else {
            aggregationRepository.recordTransactionInserted(transaction)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
        if (transaction.patternId != null) {
            val pattern = notificationPatternDao.getById(transaction.patternId)
            if (pattern != null) {
                notificationPatternDao.upsert(pattern.copy(
                    matchCount = maxOf(0, pattern.matchCount - 1)
                ))
            }
        }
        aggregationRepository.recordTransactionDeleted(transaction)
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getByCategoryFlow(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRangeFlow(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = amount,
        currencyCode = currencyCode,
        merchant = merchant,
        categoryId = categoryId,
        timestamp = timestamp,
        sourcePackageName = sourcePackageName,
        sourceAppName = sourceAppName,
        notes = notes,
        paymentSource = paymentSource,
        paymentSourceType = paymentSourceType,
        patternId = patternId
    )

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        currencyCode = currencyCode,
        merchant = merchant,
        categoryId = categoryId,
        timestamp = timestamp,
        sourcePackageName = sourcePackageName,
        sourceAppName = sourceAppName,
        notes = notes,
        paymentSource = paymentSource,
        paymentSourceType = paymentSourceType,
        patternId = patternId
    )
}
