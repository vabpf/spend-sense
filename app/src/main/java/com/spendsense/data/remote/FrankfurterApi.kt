package com.spendsense.data.remote

import com.spendsense.data.remote.model.FrankfurterRateResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrankfurterApi {
    @GET("v2/rate/{base}/{quote}")
    suspend fun getRate(
        @Path("base") base: String,
        @Path("quote") quote: String,
        @Query("date") date: String? = null
    ): FrankfurterRateResponse
}
