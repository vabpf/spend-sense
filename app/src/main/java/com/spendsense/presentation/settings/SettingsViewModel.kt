package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.backup.BackupPayload
import com.spendsense.data.backup.BackupRestoreManager
import com.spendsense.data.backup.RestoreSummary
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.WhitelistedAppDao
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
import java.io.OutputStream
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
    val isDailyReportEnabled: Boolean = false,
    val dailyReportTime: String = "20:00",
    val backgroundTheme: String = "CYBERPUNK_DEFAULT",
    val customBackgroundPath: String? = null,
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportError: String? = null,
    val isRestoring: Boolean = false,
    val pendingRestorePayload: BackupPayload? = null,
    val restoreSummary: RestoreSummary? = null,
    val restoreError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val notificationProcessor: NotificationProcessor,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.value = SettingsState(
            defaultCurrency = securePreferences.getDefaultCurrency(),
            isDailyReportEnabled = securePreferences.isDailyReportEnabled(),
            dailyReportTime = securePreferences.getDailyReportTime(),
            backgroundTheme = securePreferences.getBackgroundTheme(),
            customBackgroundPath = securePreferences.getCustomBackgroundPath()
        )
    }

    fun updateBackgroundTheme(theme: String) {
        securePreferences.setBackgroundTheme(theme)
        _state.value = _state.value.copy(backgroundTheme = theme)
    }

    fun setCustomBackground(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = java.io.File(context.filesDir, "custom_background.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                securePreferences.setCustomBackgroundPath(destFile.absolutePath)
                securePreferences.setBackgroundTheme("CUSTOM_IMAGE")
                _state.value = _state.value.copy(
                    backgroundTheme = "CUSTOM_IMAGE",
                    customBackgroundPath = destFile.absolutePath
                )
            } catch (_: Exception) {}
        }
    }

    fun updateDefaultCurrency(currencyCode: String) {
        securePreferences.setDefaultCurrency(currencyCode)
        _state.value = _state.value.copy(defaultCurrency = currencyCode)
    }

    fun updateDailyReportEnabled(enabled: Boolean, context: android.content.Context) {
        securePreferences.setDailyReportEnabled(enabled)
        _state.value = _state.value.copy(isDailyReportEnabled = enabled)
        com.spendsense.data.service.DailySpentReportWorker.schedule(context, _state.value.dailyReportTime, enabled)
    }

    fun updateDailyReportTime(time: String, context: android.content.Context) {
        securePreferences.setDailyReportTime(time)
        _state.value = _state.value.copy(dailyReportTime = time)
        com.spendsense.data.service.DailySpentReportWorker.schedule(context, time, _state.value.isDailyReportEnabled)
    }

    fun clearImportResult() {
        _state.value = _state.value.copy(importResult = null)
    }

    fun exportBackup(outputStream: OutputStream) {
        _state.value = _state.value.copy(isExporting = true, exportError = null, exportSuccessMessage = null)
        viewModelScope.launch {
            try {
                val json = backupRestoreManager.createBackupJson()
                withContext(Dispatchers.IO) {
                    outputStream.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }
                _state.value = _state.value.copy(
                    isExporting = false,
                    exportSuccessMessage = "Full backup successfully saved!"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isExporting = false,
                    exportError = e.message ?: "Failed to export data"
                )
            }
        }
    }

    fun onBackupFileSelected(content: String) {
        _state.value = _state.value.copy(restoreError = null)
        try {
            val payload = backupRestoreManager.parseBackupJson(content)
            _state.value = _state.value.copy(pendingRestorePayload = payload)
        } catch (e: Exception) {
            _state.value = _state.value.copy(restoreError = e.message ?: "Invalid backup file")
        }
    }

    fun dismissRestoreConfirmDialog() {
        _state.value = _state.value.copy(pendingRestorePayload = null)
    }

    fun executeRestore(replaceExisting: Boolean) {
        val payload = _state.value.pendingRestorePayload ?: return
        _state.value = _state.value.copy(isRestoring = true, pendingRestorePayload = null, restoreError = null)
        viewModelScope.launch {
            try {
                val summary = backupRestoreManager.restoreBackup(payload, replaceExisting)
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreSummary = summary,
                    defaultCurrency = securePreferences.getDefaultCurrency(),
                    isDailyReportEnabled = securePreferences.isDailyReportEnabled(),
                    dailyReportTime = securePreferences.getDailyReportTime()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreError = "Failed to restore backup: ${e.message}"
                )
            }
        }
    }

    fun clearExportMessage() {
        _state.value = _state.value.copy(exportSuccessMessage = null, exportError = null)
    }

    fun clearRestoreSummary() {
        _state.value = _state.value.copy(restoreSummary = null)
    }

    fun clearRestoreError() {
        _state.value = _state.value.copy(restoreError = null)
    }

    fun importNotificationsFromFile(content: String) {
        _state.value = _state.value.copy(isImporting = true, importResult = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val rawItems = parseFileContent(content)
                    
                    var transCreatedCount = 0
                    var inboxCreatedCount = 0
                    var skippedCount = 0

                    val existingApps = whitelistedAppDao.getEnabledApps().map { it.packageName }.toSet()

                    for (item in rawItems) {
                        // 1. Skip if app is not whitelisted
                        if (!existingApps.contains(item.packageName)) {
                            skippedCount++
                            continue
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
                        newAppsWhitelisted = 0,
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
