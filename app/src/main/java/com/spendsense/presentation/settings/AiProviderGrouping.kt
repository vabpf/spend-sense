package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.AiProviderEntity

data class AiProviderGroup(
    val name: String,
    val baseUrl: String,
    val jobType: String,
    val isPreset: Boolean,
    val models: List<AiProviderEntity>
) {
    val key: String = buildKey(name, baseUrl, jobType)
}

fun buildProviderGroupKey(provider: AiProviderEntity): String {
    return buildKey(provider.name, provider.baseUrl, provider.jobType)
}

fun groupProviders(providers: List<AiProviderEntity>): List<AiProviderGroup> {
    return providers
        .groupBy { buildProviderGroupKey(it) }
        .map { (_, groupedProviders) ->
            val first = groupedProviders.first()
            AiProviderGroup(
                name = first.name,
                baseUrl = first.baseUrl,
                jobType = first.jobType,
                isPreset = first.isPreset,
                models = groupedProviders.sortedWith(
                    compareBy<AiProviderEntity>({ it.defaultModel.lowercase() }, { it.id })
                )
            )
        }
        .sortedWith(compareBy<AiProviderGroup>({ it.name.lowercase() }, { it.baseUrl.lowercase() }))
}

private fun buildKey(name: String, baseUrl: String, jobType: String): String {
    return listOf(name, baseUrl, jobType).joinToString("|")
}