package com.spendsense.presentation.settings

import com.spendsense.domain.model.Category

data class CategoriesState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)
