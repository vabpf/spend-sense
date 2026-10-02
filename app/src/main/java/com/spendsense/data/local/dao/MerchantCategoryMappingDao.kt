package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity

@Dao
interface MerchantCategoryMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: MerchantCategoryMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<MerchantCategoryMappingEntity>)

    @Query("SELECT * FROM merchant_category_mappings ORDER BY usageCount DESC")
    suspend fun getAll(): List<MerchantCategoryMappingEntity>

    @Query("DELETE FROM merchant_category_mappings")
    suspend fun deleteAll()

    @Query("SELECT * FROM merchant_category_mappings WHERE merchant = :merchant")
    suspend fun getByMerchant(merchant: String): MerchantCategoryMappingEntity?
}
