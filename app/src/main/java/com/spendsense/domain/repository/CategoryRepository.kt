package com.spendsense.domain.repository

import com.spendsense.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryById(id: Long): Category?
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getAllCategoriesList(): List<Category>
    suspend fun initializeDefaultCategories()
}
