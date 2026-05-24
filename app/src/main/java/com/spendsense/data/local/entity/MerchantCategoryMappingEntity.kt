package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMappingEntity(
    @PrimaryKey
    val merchant: String,
    val categoryId: Long,
    val usageCount: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
