package com.spendsense.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.domain.model.Category
import com.spendsense.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesState(
    val categories: List<Category> = emptyList(),
    val isAddingOrEditing: Boolean = false,
    val editingCategory: Category? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    private val defaultCategoryOrder = listOf(
        "Food", "Shopping", "Entertainment", "Transport", "Bills", "Health", "Other"
    )

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                val sorted = categories.sortedBy { cat ->
                    val idx = defaultCategoryOrder.indexOfFirst { it.equals(cat.name, ignoreCase = true) }
                    if (idx >= 0) {
                        if (cat.name.equals("Other", ignoreCase = true)) Int.MAX_VALUE else idx
                    } else {
                        defaultCategoryOrder.size + cat.id.toInt()
                    }
                }
                _state.value = _state.value.copy(categories = sorted)
            }
        }
    }

    fun showAddEditDialog(category: Category?) {
        _state.value = _state.value.copy(
            isAddingOrEditing = true,
            editingCategory = category
        )
    }

    fun hideAddEditDialog() {
        _state.value = _state.value.copy(
            isAddingOrEditing = false,
            editingCategory = null
        )
    }

    fun saveCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val editingCategory = _state.value.editingCategory
            if (editingCategory != null) {
                categoryRepository.updateCategory(
                    editingCategory.copy(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                )
            } else {
                categoryRepository.insertCategory(
                    Category(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                )
            }
            hideAddEditDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
