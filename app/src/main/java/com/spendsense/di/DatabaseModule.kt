package com.spendsense.di

import android.content.Context
import androidx.room.Room
import com.spendsense.data.local.SpendSenseDatabase
import com.spendsense.data.local.MIGRATION_3_4
import com.spendsense.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpendSenseDatabase {
        return Room.databaseBuilder(
            context,
            SpendSenseDatabase::class.java,
            SpendSenseDatabase.DATABASE_NAME
        )
            .addCallback(SpendSenseDatabase.CALLBACK)
            .addMigrations(MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideTransactionDao(database: SpendSenseDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: SpendSenseDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideRegexPatternDao(database: SpendSenseDatabase): RegexPatternDao {
        return database.regexPatternDao()
    }

    @Provides
    fun provideWhitelistedAppDao(database: SpendSenseDatabase): WhitelistedAppDao {
        return database.whitelistedAppDao()
    }

    @Provides
    fun provideRawNotificationDao(database: SpendSenseDatabase): RawNotificationDao {
        return database.rawNotificationDao()
    }

    @Provides
    fun provideAiProviderDao(database: SpendSenseDatabase): AiProviderDao {
        return database.aiProviderDao()
    }
}

