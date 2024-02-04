package com.example.testretrofitsanwich

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.testretrofitsanwich.data.model.BeerModel
import com.example.testretrofitsanwich.network.download.RequestParam
import com.example.testretrofitsanwich.network.download.startDownLoadTask
import com.example.testretrofitsanwich.ui.theme.TestRetrofitSanwichTheme
import timber.log.Timber

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        setContent {
            TestRetrofitSanwichTheme {
                val bears by viewModel.beerList.collectAsState()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn {
                        items(
                            items = bears,
                            key = { it.id }
                        ) {
                            BearItem(bear = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BearItem(
    modifier: Modifier = Modifier,
    bear: BeerModel,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()

    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp)
        ) {
            Text(text = bear.name)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestRetrofitSanwichTheme {
        BearItem(
            bear = BeerModel(
                id = 1,
                name = "dfsdfsdf",
                tagline = "Dfsdfsf",
                description = "dfsdfdadsfafd",
                firstBrewed = "sdfsdfsf",
                imageUrl = "dvvvev",
            )
        )
    }
}