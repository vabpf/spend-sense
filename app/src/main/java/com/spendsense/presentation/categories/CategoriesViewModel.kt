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

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _state.value = _state.value.copy(categories = categories)
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
