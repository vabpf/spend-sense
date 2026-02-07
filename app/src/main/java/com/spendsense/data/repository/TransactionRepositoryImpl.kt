package com.spendsense.data.repository

import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.data.local.entity.TransactionEntity
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
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
        merchant = merchant,
        categoryId = categoryId,
        timestamp = timestamp,
        sourcePackageName = sourcePackageName,
        sourceAppName = sourceAppName,
        notes = notes
    )

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        merchant = merchant,
        categoryId = categoryId,
        timestamp = timestamp,
        sourcePackageName = sourcePackageName,
        sourceAppName = sourceAppName,
        notes = notes
    )
}
