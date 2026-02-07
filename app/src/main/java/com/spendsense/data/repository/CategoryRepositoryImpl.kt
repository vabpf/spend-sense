package com.spendsense.data.repository

import com.spendsense.data.local.dao.CategoryDao
import com.spendsense.data.local.entity.CategoryEntity
import com.spendsense.domain.model.Category
import com.spendsense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllCategoriesList(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    override suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getAll()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food", iconName = "Restaurant", colorHex = "#FF6B6B", isDefault = true),
                CategoryEntity(name = "Transport", iconName = "DirectionsCar", colorHex = "#4ECDC4", isDefault = true),
                CategoryEntity(name = "Shopping", iconName = "ShoppingCart", colorHex = "#FFE66D", isDefault = true),
                CategoryEntity(name = "Entertainment", iconName = "Movie", colorHex = "#95E1D3", isDefault = true),
                CategoryEntity(name = "Bills", iconName = "Receipt", colorHex = "#F38181", isDefault = true),
                CategoryEntity(name = "Health", iconName = "LocalHospital", colorHex = "#AA96DA", isDefault = true),
                CategoryEntity(name = "Other", iconName = "MoreHoriz", colorHex = "#FCBAD3", isDefault = true)
            )
            categoryDao.insertAll(defaultCategories)
        }
    }

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault
    )

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault
    )
}
