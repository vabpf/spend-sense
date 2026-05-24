package com.spendsense.data.local

import com.spendsense.data.local.dao.ProviderAccountDao
import com.spendsense.data.local.entity.ProviderAccountEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AiProviderPresets {
    const val JOB_REGEX_GEN = "REGEX_GEN"

    const val OPENCODE_BASE_URL = "https://opencode.ai/zen/v1"
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
    const val NVIDIA_NIM_BASE_URL = "https://integrate.api.nvidia.com/v1"
    const val OLLAMA_CLOUD_BASE_URL = "https://ollama.com/api"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"

    private val seedMutex = Mutex()
    @Volatile
    private var seeded = false

    fun accountPresets(): List<ProviderAccountEntity> = listOf(
        ProviderAccountEntity(name = "OpenCode", baseUrl = OPENCODE_BASE_URL, jobType = JOB_REGEX_GEN, isPreset = true),
        ProviderAccountEntity(name = "OpenRouter", baseUrl = OPENROUTER_BASE_URL, jobType = JOB_REGEX_GEN, isPreset = true),
        ProviderAccountEntity(name = "NVIDIA NIM", baseUrl = NVIDIA_NIM_BASE_URL, jobType = JOB_REGEX_GEN, isPreset = true),
        ProviderAccountEntity(name = "Ollama Cloud", baseUrl = OLLAMA_CLOUD_BASE_URL, jobType = JOB_REGEX_GEN, isPreset = true),
        ProviderAccountEntity(name = "Gemini", baseUrl = GEMINI_BASE_URL, jobType = JOB_REGEX_GEN, isPreset = true)
    )

    suspend fun ensureSeeded(dao: ProviderAccountDao) {
        if (seeded) return

        seedMutex.withLock {
            if (seeded) return

            val existingKeys = dao.getAll().map { it.baseUrl }.toSet()

            accountPresets().forEach { preset ->
                if (preset.baseUrl !in existingKeys) {
                    dao.insert(preset)
                }
            }

            seeded = true
        }
    }
}
