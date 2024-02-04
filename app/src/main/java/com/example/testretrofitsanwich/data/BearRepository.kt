package com.example.testretrofitsanwich.data

import com.example.testretrofitsanwich.data.model.BeerModel
import com.example.testretrofitsanwich.data.model.toModel
import com.example.testretrofitsanwich.network.ApiProvider

interface BearRepository {
    suspend fun getBeers(page: Int, pageCount: Int): ApiResult<List<BeerModel>>
}

class BearRepositoryImpl(private val apiProvider: ApiProvider) : BearRepository {
    override suspend fun getBeers(page: Int, pageCount: Int) =
        apiProvider.getRemoteResult {
            getBeers(page = page, pageCount = pageCount)
        }.map { resultList ->
            resultList.map { it.toModel() }
        }
}