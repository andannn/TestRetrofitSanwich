package com.example.testretrofitsanwich

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testretrofitsanwich.data.ApiResult
import com.example.testretrofitsanwich.data.BearRepository
import com.example.testretrofitsanwich.data.BearRepositoryImpl
import com.example.testretrofitsanwich.data.model.BeerModel
import com.example.testretrofitsanwich.data.successOrThrow
import com.example.testretrofitsanwich.network.ApiProvider
import com.example.testretrofitsanwich.network.ApiProviderImpl
import com.example.testretrofitsanwich.network.download.RequestParam
import com.example.testretrofitsanwich.network.download.startDownLoadTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO: Use some Data inject library to provide repository.
val apiProvider: ApiProvider = ApiProviderImpl()
val beerRepository: BearRepository = BearRepositoryImpl(apiProvider)

private const val TAG = "MainViewModel"
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _beerList = MutableStateFlow<List<BeerModel>>(emptyList())
    val beerList = _beerList.asStateFlow()

    init {
// Method 1:
//        viewModelScope.launch {
//            when (val result = beerRepository.getBeers(page = 1, pageCount = 20)) {
//                is ApiResult.Error -> {
//                    Timber.tag(TAG).d(" getBeers error ${result.throwable}")
//                }
//                is ApiResult.Success -> {
//                    _beerList.value = result.data
//                }
//            }
//        }

// Method 2:
        viewModelScope.launch {
            try {
                val beerList = beerRepository.getBeers(page = 1, pageCount = 20)
                _beerList.value = beerList.successOrThrow()
            } catch (e: Exception) {
                Timber.tag(TAG).d(" getBeers error $e")
            }
        }

        // Test Download Task.
        viewModelScope.launch {
            startDownLoadTask(
                requestParam = RequestParam(
                    url = "https://publicobject.com/helloworld.txt",
                    destinationPath = application.filesDir.absolutePath,
                    fileName = "helloworld.txt"
                ),
                onUpdate = { readLength: Long, totalLength: Long, speed: Long, timeLeft: Long ->

                },
                onSuccess = {
                    Timber.tag(TAG).d("onSuccess")
                },
                onFailure = {
                    Timber.tag(TAG).d("onFailure $it")
                }
            )
        }
    }
}