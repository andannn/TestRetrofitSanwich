package com.example.testretrofitsanwich.network

import android.app.Application
import com.example.testretrofitsanwich.network.api.BeerApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import timber.log.Timber

private const val TAG = "ApiProvider"

interface ApiProvider {
    val service: BeerApi
}

class ApiProviderImpl() : ApiProvider {

    private val contentType = "application/json".toMediaType()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = OkHttpClient.Builder()
        .addLogInterceptor(TAG)
        //.addInterceptor(MockInterceptor())
        .build()

    private val retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(BeerApi.BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()

    private val _service = retrofit.create<BeerApi>()

    override val service: BeerApi
        get() = _service
}

fun OkHttpClient.Builder.addLogInterceptor(tag: String) = apply {
    addInterceptor(
        HttpLoggingInterceptor(HttpLog(tag)).apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        },
    )
}

class HttpLog(private val tag: String) : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Timber.tag(tag).d("HttpLogInfo: $message")
    }
}
