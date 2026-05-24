package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity

@Dao
interface MerchantCategoryMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantCategoryMappingEntity)

    @Query("SELECT * FROM merchant_category_mappings WHERE merchant = :merchant")
    suspend fun getByMerchant(merchant: String): MerchantCategoryMappingEntity?
}
