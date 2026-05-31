package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.NotificationPatternDao
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.data.service.NotificationProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPatternsViewModel @Inject constructor(
    private val notificationPatternDao: NotificationPatternDao,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val securePreferences: SecurePreferences,
    private val notificationProcessor: NotificationProcessor
) : ViewModel() {

    val patterns: StateFlow<List<NotificationPatternEntity>> =
        notificationPatternDao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableApps = MutableStateFlow<List<RegexTargetApp>>(emptyList())
    val availableApps: StateFlow<List<RegexTargetApp>> = _availableApps.asStateFlow()

    val appNameMap: StateFlow<Map<String, String>> = _availableApps
        .map { apps -> apps.associate { it.packageName to it.appName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _newPackageName = MutableStateFlow("")
    val newPackageName: StateFlow<String> = _newPackageName.asStateFlow()

    private val _newTitle = MutableStateFlow("")
    val newTitle: StateFlow<String> = _newTitle.asStateFlow()

    private val _newRegex = MutableStateFlow("")
    val newRegex: StateFlow<String> = _newRegex.asStateFlow()

    private val _newIsTransaction = MutableStateFlow(true)
    val newIsTransaction: StateFlow<Boolean> = _newIsTransaction.asStateFlow()

    private val _newCurrencyCode = MutableStateFlow("")
    val newCurrencyCode: StateFlow<String> = _newCurrencyCode.asStateFlow()

    private val _selectedAppIndex = MutableStateFlow(-1)
    val selectedAppIndex: StateFlow<Int> = _selectedAppIndex.asStateFlow()

    // Edit dialog state
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _editingPatternId = MutableStateFlow(-1L)
    val editingPatternId: StateFlow<Long> = _editingPatternId.asStateFlow()

    private val _editPackageName = MutableStateFlow("")
    val editPackageName: StateFlow<String> = _editPackageName.asStateFlow()

    private val _editTitle = MutableStateFlow("")
    val editTitle: StateFlow<String> = _editTitle.asStateFlow()

    private val _editRegex = MutableStateFlow("")
    val editRegex: StateFlow<String> = _editRegex.asStateFlow()

    private val _editIsTransaction = MutableStateFlow(true)
    val editIsTransaction: StateFlow<Boolean> = _editIsTransaction.asStateFlow()

    private val _editCurrencyCode = MutableStateFlow("")
    val editCurrencyCode: StateFlow<String> = _editCurrencyCode.asStateFlow()

    private val _editSelectedAppIndex = MutableStateFlow(-1)
    val editSelectedAppIndex: StateFlow<Int> = _editSelectedAppIndex.asStateFlow()

    init {
        loadApps()
        _newCurrencyCode.value = securePreferences.getDefaultCurrency()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = whitelistedAppDao.getEnabledApps()
                .map { RegexTargetApp(packageName = it.packageName, appName = it.appName) }
                .sortedBy { it.appName.lowercase() }
            _availableApps.value = apps
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
        _newPackageName.value = ""
        _newTitle.value = ""
        _newRegex.value = ""
        _newIsTransaction.value = true
        _selectedAppIndex.value = -1
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun updateNewTitle(title: String) {
        _newTitle.value = title
    }

    fun updateNewRegex(regex: String) {
        _newRegex.value = regex
    }

    fun updateNewIsTransaction(isTransaction: Boolean) {
        _newIsTransaction.value = isTransaction
    }

    fun updateNewCurrencyCode(code: String) {
        _newCurrencyCode.value = code
    }

    fun selectApp(index: Int) {
        _selectedAppIndex.value = index
        if (index >= 0 && index < _availableApps.value.size) {
            _newPackageName.value = _availableApps.value[index].packageName
        }
    }

    fun saveNewPattern() {
        val title = _newTitle.value.trim()
        val packageName = _newPackageName.value.trim()
        if (title.isBlank() || packageName.isBlank()) return

        viewModelScope.launch {
            val pattern = NotificationPatternEntity(
                packageName = packageName,
                notificationTitle = title,
                regex = _newRegex.value.trim().takeIf { it.isNotBlank() },
                currencyCode = _newCurrencyCode.value,
                isTransaction = _newIsTransaction.value
            )
            notificationPatternDao.upsert(pattern)
            
            val appName = _availableApps.value
                .firstOrNull { it.packageName == packageName }
                ?.appName ?: "App"
            
            // Reprocess the pending inbox for this newly saved pattern
            notificationProcessor.reprocessInboxForPattern(pattern, appName = appName)

            hideAddDialog()
        }
    }

    fun deletePattern(id: Long) {
        viewModelScope.launch {
            notificationPatternDao.deleteById(id)
        }
    }

    fun updatePattern(id: Long, regex: String?, isTransaction: Boolean) {
        viewModelScope.launch {
            notificationPatternDao.update(id, regex, isTransaction)
        }
    }

    fun startEdit(pattern: NotificationPatternEntity) {
        _editingPatternId.value = pattern.id
        _editPackageName.value = pattern.packageName
        _editTitle.value = pattern.notificationTitle
        _editRegex.value = pattern.regex ?: ""
        _editIsTransaction.value = pattern.isTransaction
        _editCurrencyCode.value = pattern.currencyCode
        _editSelectedAppIndex.value = availableApps.value.indexOfFirst { it.packageName == pattern.packageName }
        _showEditDialog.value = true
    }

    fun hideEditDialog() {
        _showEditDialog.value = false
    }

    fun updateEditTitle(title: String) {
        _editTitle.value = title
    }

    fun updateEditRegex(regex: String) {
        _editRegex.value = regex
    }

    fun updateEditIsTransaction(isTransaction: Boolean) {
        _editIsTransaction.value = isTransaction
    }

    fun updateEditCurrencyCode(code: String) {
        _editCurrencyCode.value = code
    }

    fun selectEditApp(index: Int) {
        _editSelectedAppIndex.value = index
        if (index >= 0 && index < _availableApps.value.size) {
            _editPackageName.value = _availableApps.value[index].packageName
        }
    }

    fun saveEditedPattern() {
        val id = _editingPatternId.value
        val title = _editTitle.value.trim()
        val packageName = _editPackageName.value.trim()
        if (title.isBlank() || packageName.isBlank()) return

        viewModelScope.launch {
            val pattern = NotificationPatternEntity(
                id = id,
                packageName = packageName,
                notificationTitle = title,
                regex = _editRegex.value.trim().takeIf { it.isNotBlank() },
                currencyCode = _editCurrencyCode.value,
                isTransaction = _editIsTransaction.value
            )
            notificationPatternDao.updateAll(
                id = id,
                packageName = packageName,
                notificationTitle = title,
                regex = pattern.regex,
                currencyCode = pattern.currencyCode,
                isTransaction = pattern.isTransaction
            )
            
            val appName = _availableApps.value
                .firstOrNull { it.packageName == packageName }
                ?.appName ?: "App"

            // Reprocess the pending inbox for this edited pattern
            notificationProcessor.reprocessInboxForPattern(pattern, appName = appName)

            hideEditDialog()
        }
    }
}
