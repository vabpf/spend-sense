package com.spendsense.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Restaurant
import org.junit.Assert.assertEquals
import org.junit.Test

class IconUtilTest {

    @Test
    fun getCategoryIcon_withValidName_returnsCorrectIcon() {
        val result = getCategoryIcon("Restaurant")
        assertEquals(Icons.Rounded.Restaurant, result)
    }

    @Test
    fun getCategoryIcon_withInvalidName_returnsFallbackIcon() {
        val result = getCategoryIcon("UnknownIcon")
        assertEquals(Icons.Rounded.Category, result)
    }
}

