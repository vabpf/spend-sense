package com.spendsense.di

import com.spendsense.data.repository.CategoryRepositoryImpl
import com.spendsense.data.repository.RegexPatternRepositoryImpl
import com.spendsense.data.repository.TransactionRepositoryImpl
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.RegexPatternRepository
import com.spendsense.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindRegexPatternRepository(
        regexPatternRepositoryImpl: RegexPatternRepositoryImpl
    ): RegexPatternRepository

    @Binds
    @Singleton
    abstract fun bindWhitelistedAppRepository(
        whitelistedAppRepositoryImpl: com.spendsense.data.repository.WhitelistedAppRepositoryImpl
    ): com.spendsense.domain.repository.WhitelistedAppRepository
}
