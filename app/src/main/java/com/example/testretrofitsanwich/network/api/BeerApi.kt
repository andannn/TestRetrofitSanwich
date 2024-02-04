package com.example.testretrofitsanwich.network.api

import com.example.testretrofitsanwich.network.model.BeerDto
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BeerApi {
    @GET("beers")
    suspend fun getBeers(
        @Query("page") page: Int,
        @Query("per_page") pageCount: Int
    ): ApiResponse<List<BeerDto>>

    companion object {
        const val BASE_URL = "https://api.punkapi.com/v2/"
    }
}
