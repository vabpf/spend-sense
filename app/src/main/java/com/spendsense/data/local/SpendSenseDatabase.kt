package com.spendsense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RegexPatternEntity::class,
        WhitelistedAppEntity::class,
        RawNotificationEntity::class,
        AiProviderEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class SpendSenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun regexPatternDao(): RegexPatternDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun aiProviderDao(): AiProviderDao


    companion object {
        const val DATABASE_NAME = "spend_sense.db"

        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO ai_providers (name, baseUrl, defaultModel, jobType, isPreset) VALUES " +
                    "('OpenCode (MiniMax)', 'https://opencode.ai/zen/v1', 'oc/minimax-m2.5-free', 'REGEX_GEN', 1), " +
                    "('OpenCode (Trinity)', 'https://opencode.ai/zen/v1', 'oc/trinity-large-preview-free', 'REGEX_GEN', 1), " +
                    "('OpenCode (Nemotron)', 'https://opencode.ai/zen/v1', 'oc/nemotron-3-super-free', 'REGEX_GEN', 1), " +
                    "('NVIDIA NIM', 'https://integrate.api.nvidia.com/v1', 'meta/llama-3.1-8b-instruct', 'REGEX_GEN', 1), " +
                    "('Google Gemini', 'https://generativelanguage.googleapis.com/v1beta/openai/', 'gemini-1.5-flash', 'REGEX_GEN', 1)")
            }
        }
    }
}
