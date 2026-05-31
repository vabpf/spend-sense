package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.WhitelistedAppEntity
import com.spendsense.data.service.NotificationProcessor
import com.spendsense.data.service.ProcessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

data class ImportResult(
    val totalParsed: Int,
    val newAppsWhitelisted: Int,
    val transactionsCreated: Int,
    val inboxCreated: Int,
    val skipped: Int
)

data class SettingsState(
    val defaultCurrency: String = "USD",
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val notificationProcessor: NotificationProcessor
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

    fun clearImportResult() {
        _state.value = _state.value.copy(importResult = null)
    }

    fun importNotificationsFromFile(content: String) {
        _state.value = _state.value.copy(isImporting = true, importResult = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val rawItems = parseFileContent(content)
                    
                    var newAppsCount = 0
                    var transCreatedCount = 0
                    var inboxCreatedCount = 0
                    var skippedCount = 0

                    val existingApps = whitelistedAppDao.getEnabledApps().map { it.packageName }.toSet()

                    for (item in rawItems) {
                        // 1. Check if we need to auto-whitelist the app
                        if (!existingApps.contains(item.packageName)) {
                            whitelistedAppDao.insert(
                                WhitelistedAppEntity(
                                    packageName = item.packageName,
                                    appName = item.appName,
                                    isEnabled = true,
                                    addedAt = System.currentTimeMillis()
                                )
                            )
                            newAppsCount++
                        }

                        // 2. Process notification silently
                        val processOutcome = notificationProcessor.process(
                            packageName = item.packageName,
                            appName = item.appName,
                            title = item.title,
                            text = item.textContent,
                            timestamp = item.postTime,
                            listener = null
                        )

                        when (processOutcome) {
                            ProcessResult.TRANSACTION_CREATED -> transCreatedCount++
                            ProcessResult.INBOX_CREATED -> inboxCreatedCount++
                            ProcessResult.SILENT_SKIPPED -> skippedCount++
                        }
                    }

                    ImportResult(
                        totalParsed = rawItems.size,
                        newAppsWhitelisted = newAppsCount,
                        transactionsCreated = transCreatedCount,
                        inboxCreated = inboxCreatedCount,
                        skipped = skippedCount
                    )
                }

                _state.value = _state.value.copy(
                    isImporting = false,
                    importResult = result
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isImporting = false,
                    importResult = ImportResult(0, 0, 0, 0, 0) // fallback empty result on fatal parse
                )
            }
        }
    }

    private data class ParsedNotification(
        val packageName: String,
        val appName: String,
        val title: String?,
        val textContent: String,
        val postTime: Long
    )

    private fun parseFileContent(content: String): List<ParsedNotification> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        return if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            parseJson(trimmed)
        } else {
            parseCsv(trimmed)
        }
    }

    private fun parseJson(content: String): List<ParsedNotification> {
        val list = mutableListOf<ParsedNotification>()
        try {
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val packageName = obj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
                val appName = obj.optString("appName").takeIf { it.isNotBlank() } ?: "Imported App"
                val title = obj.optString("title").takeIf { it.isNotBlank() && it != "null" }
                val textContent = obj.optString("textContent").takeIf { it.isNotBlank() && it != "null" } ?: ""
                val postTime = obj.optLong("postTime", System.currentTimeMillis())

                list.add(ParsedNotification(packageName, appName, title, textContent, postTime))
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseCsv(content: String): List<ParsedNotification> {
        val list = mutableListOf<ParsedNotification>()
        val lines = content.lines().filter { it.trim().isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val headers = parseCsvLine(lines[0]).map { it.lowercase() }
        
        val pkgIdx = headers.indexOf("packagename")
        val nameIdx = headers.indexOf("appname")
        val titleIdx = headers.indexOf("title")
        val textIdx = headers.indexOf("textcontent")
        val timeIdx = headers.indexOf("posttime")

        if (pkgIdx == -1 || textIdx == -1) return emptyList() // essential fields missing

        for (i in 1 until lines.size) {
            try {
                val columns = parseCsvLine(lines[i])
                if (columns.size <= pkgIdx || columns.size <= textIdx) continue

                val packageName = columns[pkgIdx].takeIf { it.isNotBlank() } ?: continue
                val appName = if (nameIdx != -1 && columns.size > nameIdx && columns[nameIdx].isNotBlank()) columns[nameIdx] else "Imported App"
                val title = if (titleIdx != -1 && columns.size > titleIdx && columns[titleIdx].isNotBlank()) columns[titleIdx] else null
                val textContent = columns[textIdx]
                val postTime = if (timeIdx != -1 && columns.size > timeIdx && columns[timeIdx].isNotBlank()) {
                    columns[timeIdx].toLongOrNull() ?: System.currentTimeMillis()
                } else System.currentTimeMillis()

                list.add(ParsedNotification(packageName, appName, title, textContent, postTime))
            } catch (_: Exception) {}
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val curToken = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                tokens.add(curToken.toString().trim())
                curToken.setLength(0)
            } else {
                curToken.append(ch)
            }
        }
        tokens.add(curToken.toString().trim())
        return tokens
    }
}
