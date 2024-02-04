package com.example.testretrofitsanwich.data

import com.example.testretrofitsanwich.network.ApiProvider
import com.example.testretrofitsanwich.network.api.BeerApi
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class InternalServerException(
    val code: Int,
    override val message: String? = null,
) : Throwable()

sealed interface ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Error<T>(val throwable: Throwable) : ApiResult<T>
}

suspend fun <T> ApiProvider.getRemoteResult(
    block: suspend BeerApi.() -> ApiResponse<T>,
): ApiResult<T> = withContext(Dispatchers.IO) {
    when (val response = this@getRemoteResult.service.block()) {
        is ApiResponse.Success -> ApiResult.Success(response.data)
        is ApiResponse.Failure.Exception,
        is ApiResponse.Failure.Error,
        -> {
            ApiResult.Error(response.resolveException())
        }
    }
}

fun <I, O> ApiResult<I>.map(mapper: (I) -> O) = when (this) {
    is ApiResult.Error -> ApiResult.Error(throwable)
    is ApiResult.Success -> ApiResult.Success(mapper(data))
}

fun <T> ApiResult<T>.successOrThrow() : T {
    when (this) {
        is ApiResult.Error -> throw throwable;
        is ApiResult.Success -> return data
    }
}

fun ApiResponse<*>.resolveException() = when (this) {
    is ApiResponse.Success -> error("no exception for a success response")
    is ApiResponse.Failure.Exception -> {
        throwable
    }

    is ApiResponse.Failure.Error -> {
        val response = (payload as? Response<*>)
        val statusCode = response!!.code()
        val message = response.errorBody()?.string()

// TODO: server error occurred, map this error to defined Exception by state code.
// For example:
//        when (statusCode) {
//            509 -> InvalidTokenError()
//            510 -> UserNotAvailable()
//            else -> InternalServerException(statusCode, message)
//        }

        InternalServerException(statusCode, message)
    }
}