package com.spendsense.di

import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
import com.spendsense.data.remote.FrankfurterApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FrankfurterOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FrankfurterRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/") // Default base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatCompletionApi(retrofit: Retrofit): ChatCompletionApi {
        return retrofit.create(ChatCompletionApi::class.java)
    }

    @FrankfurterOkHttp
    @Provides
    @Singleton
    fun provideFrankfurterOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @FrankfurterRetrofit
    @Provides
    @Singleton
    fun provideFrankfurterRetrofit(@FrankfurterOkHttp client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFrankfurterApi(@FrankfurterRetrofit retrofit: Retrofit): FrankfurterApi {
        return retrofit.create(FrankfurterApi::class.java)
    }
}
