package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import com.spendsense.data.local.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsState(
    val defaultCurrency: String = "USD"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.value = SettingsState(
            defaultCurrency = securePreferences.getDefaultCurrency()
        )
    }

    fun updateDefaultCurrency(currencyCode: String) {
        securePreferences.setDefaultCurrency(currencyCode)
        _state.value = _state.value.copy(defaultCurrency = currencyCode)
    }
}
