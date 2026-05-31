package com.spendsense.di

import com.spendsense.data.repository.CategoryRepositoryImpl
import com.spendsense.data.repository.ExchangeRateRepositoryImpl
import com.spendsense.data.repository.TransactionRepositoryImpl
import com.spendsense.data.repository.WhitelistedAppRepositoryImpl
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.ExchangeRateRepository
import com.spendsense.domain.repository.TransactionRepository
import com.spendsense.domain.repository.WhitelistedAppRepository
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
    abstract fun bindExchangeRateRepository(
        exchangeRateRepositoryImpl: ExchangeRateRepositoryImpl
    ): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindWhitelistedAppRepository(
        whitelistedAppRepositoryImpl: WhitelistedAppRepositoryImpl
    ): WhitelistedAppRepository
}
