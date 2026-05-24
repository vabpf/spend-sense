package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_models")
data class ProviderModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerAccountId: Long,
    val modelId: String,
    val displayName: String?,
    val isEnabled: Boolean = false,
    val stale: Boolean = false,
    val lastRefreshedAt: Long = 0
)
