package com.spendsense.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spendsense.data.local.dao.CategoryDao
import com.spendsense.data.local.dao.MerchantCategoryMappingDao
import com.spendsense.data.local.dao.RawNotificationDao
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var rawNotificationDao: RawNotificationDao

    @Inject
    lateinit var merchantCategoryMappingDao: MerchantCategoryMappingDao

    @Inject
    lateinit var categoryDao: CategoryDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SAVE -> handleSave(intent)
            ACTION_REJECT -> handleReject(intent)
        }
    }

    private fun handleSave(intent: Intent) {
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: return
        val currencyCode = intent.getStringExtra(EXTRA_CURRENCY) ?: "USD"
        val sourcePackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val sourceAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
        val rawNotificationId = intent.getLongExtra(EXTRA_RAW_NOTIFICATION_ID, -1L)
        val suggestedCategoryId = intent.getLongExtra(EXTRA_SUGGESTED_CATEGORY_ID, -1L)

        scope.launch {
            val categoryId = if (suggestedCategoryId > 0) {
                suggestedCategoryId
            } else {
                val categories = categoryDao.getAll()
                categories.firstOrNull { it.name == "Other" }?.id
                    ?: categories.firstOrNull()?.id
                    ?: return@launch
            }

            transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    currencyCode = currencyCode,
                    merchant = merchant,
                    categoryId = categoryId,
                    timestamp = System.currentTimeMillis(),
                    sourcePackageName = sourcePackageName,
                    sourceAppName = sourceAppName
                )
            )

            merchantCategoryMappingDao.upsert(
                MerchantCategoryMappingEntity(
                    merchant = merchant.lowercase(),
                    categoryId = categoryId
                )
            )

            if (rawNotificationId > 0) {
                rawNotificationDao.markAsProcessed(rawNotificationId)
            }
        }
    }

    private fun handleReject(intent: Intent) {
        val rawNotificationId = intent.getLongExtra(EXTRA_RAW_NOTIFICATION_ID, -1L)
        if (rawNotificationId > 0) {
            scope.launch {
                rawNotificationDao.markAsProcessed(rawNotificationId)
            }
        }
    }

    companion object {
        const val ACTION_SAVE = "com.spendsense.SAVE_TRANSACTION"
        const val ACTION_REJECT = "com.spendsense.REJECT_TRANSACTION"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_CURRENCY = "extra_currency"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_RAW_NOTIFICATION_ID = "extra_raw_notification_id"
        const val EXTRA_SUGGESTED_CATEGORY_ID = "extra_suggested_category_id"
    }
}
