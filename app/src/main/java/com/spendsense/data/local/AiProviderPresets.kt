package com.spendsense.data.local

import com.spendsense.data.local.dao.AiProviderDao
import com.spendsense.data.local.entity.AiProviderEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AiProviderPresets {
    private const val JOB_REGEX_GEN = "REGEX_GEN"

    private const val OPENCODE_BASE_URL = "https://opencode.ai/zen/v1"
    private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
    private const val NVIDIA_NIM_BASE_URL = "https://integrate.api.nvidia.com/v1"
    private const val OLLAMA_CLOUD_BASE_URL = "https://api.ollama.com/v1"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"

    private val seedMutex = Mutex()
    @Volatile
    private var seeded = false

    fun all(): List<AiProviderEntity> {
        val providers = mutableListOf<AiProviderEntity>()

        val opencodeModels = listOf(
            "minimax-m2.5-free",
            "trinity-large-preview-free",
            "nemotron-3-super-free"
        )
        providers += opencodeModels.map { model ->
            AiProviderEntity(
                name = "OpenCode",
                baseUrl = OPENCODE_BASE_URL,
                defaultModel = model,
                jobType = JOB_REGEX_GEN,
                isPreset = true
            )
        }

        val openRouterModels = listOf(
            "lyria-3-pro-preview",
            "lyria-3-clip-preview",
            "elephant-alpha",
            "gemma-4-26b-a4b-it:free",
            "gemma-4-31b-it:free",
            "nemotron-3-super-120b-a12b:free",
            "qwen3-next-80b-a3b-instruct:free",
            "qwen3-coder:free",
            "nemotron-3-nano-30b-a3b:free"
        )
        providers += openRouterModels.map { model ->
            AiProviderEntity(
                name = "OpenRouter",
                baseUrl = OPENROUTER_BASE_URL,
                defaultModel = model,
                jobType = JOB_REGEX_GEN,
                isPreset = true
            )
        }

        val nvidiaModels = listOf(
            "moonshotai/kimi-k2.5",
            "z-ai/glm4.7"
        )
        providers += nvidiaModels.map { model ->
            AiProviderEntity(
                name = "NVIDIA NIM",
                baseUrl = NVIDIA_NIM_BASE_URL,
                defaultModel = model,
                jobType = JOB_REGEX_GEN,
                isPreset = true
            )
        }

        val ollamaCloudModels = listOf(
            "gpt-oss:120b",
            "kimi-k2.5",
            "glm-5",
            "minimax-m2.5",
            "glm-4.7-flash",
            "qwen3.5"
        )
        providers += ollamaCloudModels.map { model ->
            AiProviderEntity(
                name = "Ollama Cloud",
                baseUrl = OLLAMA_CLOUD_BASE_URL,
                defaultModel = model,
                jobType = JOB_REGEX_GEN,
                isPreset = true
            )
        }

        val geminiModels = listOf(
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-flash-image-preview",
            "gemini-3-flash-preview",
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemma-4-31b-it"
        )
        providers += geminiModels.map { model ->
            AiProviderEntity(
                name = "Gemini",
                baseUrl = GEMINI_BASE_URL,
                defaultModel = model,
                jobType = JOB_REGEX_GEN,
                isPreset = true
            )
        }

        return providers
    }

    suspend fun ensureSeeded(aiProviderDao: AiProviderDao) {
        if (seeded) return

        seedMutex.withLock {
            if (seeded) return

            val existing = aiProviderDao.getAllProviders()
            val existingKeys = existing.map { Triple(it.name, it.baseUrl, it.defaultModel) }.toSet()

            all().forEach { preset ->
                val key = Triple(preset.name, preset.baseUrl, preset.defaultModel)
                if (key !in existingKeys) {
                    aiProviderDao.insert(preset)
                }
            }

            seeded = true
        }
    }
}
