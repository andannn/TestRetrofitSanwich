package com.example.testretrofitsanwich.network.download

import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Handler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "DownLoadHelper"

data class RequestParam(
    val url: String,
    val destinationPath: String,
    val fileName: String,
)

/**
 * Get the file size from [url].
 */
suspend fun getDownLoadFileSize(url: String) = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder().url(url).head().build()
    val fileSize: Long = try {
        val response = client.newCall(request).execute()
        response.headers["Content-Length"]?.toLong() ?: -1L
    } catch (e: IOException) {
        -1L
    }
    fileSize
}

@Throws(IOException::class)
suspend fun downloadFileOrThrow(
    uri: String,
    destinationPath: String,
    fileName: String,
) = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {}

    val downloader = DownLoadHelper()
    downloader.listener = object : DownLoadHelper.Listener {
        override fun onSuccess(filePath: String) {
            cont.resume(filePath)
        }

        override fun onFailure(exception: java.lang.Exception) {
            cont.resumeWithException(exception)
        }
    }

    downloader.download(
        url = uri,
        destinationPath = destinationPath,
        fileName = fileName,
    )
}

suspend fun startDownLoadTask(
    requestParam: RequestParam,
    onUpdate: (
        readLength: Long,
        totalLength: Long,
        speed: Long,
        timeLeft: Long,
    ) -> Unit,
    onSuccess: (String) -> Unit = {},
    onFailure: (exception: Exception) -> Unit = {},
) = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { cont ->
        val downLoadHelper = DownLoadHelper()
        val handler = android.os.Handler(Looper.getMainLooper())
        cont.invokeOnCancellation {
            downLoadHelper.cancelDownLoad()
        }

        downLoadHelper.listener = object : DownLoadHelper.Listener {
            override fun onUpdate(
                readLength: Long,
                totalLength: Long,
                speed: Long,
                timeLeft: Long,
            ) {
                handler.post {
                    onUpdate(readLength, totalLength, speed, timeLeft)
                }
            }

            override fun onSuccess(filePath: String) {
                cont.resume(Unit)
                handler.post {
                    onSuccess(filePath)
                }
            }

            override fun onFailure(exception: Exception) {
                cont.resume(Unit)
                handler.post {
                    onFailure(exception)
                }
            }
        }

        with(requestParam) {
            downLoadHelper.download(
                url = url,
                destinationPath = destinationPath,
                fileName = fileName,
            )
        }
    }
}

class DownLoadHelper {
    var listener: Listener = EmptyListener
    private val isCanceled = AtomicBoolean(false)
    private var startBytes: Long = 0L

    companion object {
        private val EmptyListener = object : Listener {}
        private const val DOWNLOADING_SUFFIX = ".downloading"
    }

    interface Listener {
        @WorkerThread
        fun onUpdate(
            readLength: Long,
            totalLength: Long,
            speed: Long,
            timeLeft: Long,
        ) {
        }

        @WorkerThread
        fun onFailure(exception: Exception) {
        }

        @WorkerThread
        fun onSuccess(filePath: String) {
        }
    }

    fun download(
        url: String,
        destinationPath: String,
        fileName: String,
    ) {
        isCanceled.set(false)

        val client = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalResponse.body, listener))
                    .build()
            }.build()

        val downloadingFile = File(destinationPath, "$fileName$DOWNLOADING_SUFFIX")
        startBytes = with(downloadingFile) {
            if (exists()) length() else 0
        }
        Timber.tag(TAG).d("download from start $startBytes")
        val request = Request.Builder()
            .url(url)
            .header(
                "Range",
                "bytes=$startBytes-",
            ).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val dictionary = File(destinationPath)
                if (!dictionary.exists()) {
                    dictionary.mkdir()
                }

                val outputStream = FileOutputStream(downloadingFile, true)
                val sink = outputStream.sink().buffer()

                Timber.tag(TAG).d("url $url")
                Timber.tag(TAG).d("destinationPath $destinationPath")
                Timber.tag(TAG).d("fileName $fileName")
                sink.writeAll(response.body!!.source())
                sink.close()

                if (!isCanceled.get()) {
                    Timber.tag(TAG).d("Download success")
                    downloadingFile.renameTo(File(destinationPath, fileName))
                    Timber.tag(TAG).d("Rename success")

                    listener.onSuccess(downloadingFile.absolutePath)
                }
            } else {
                listener.onFailure(IllegalStateException("Server Error"))
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d("Download failed with exception $e")
            isCanceled.set(true)
            listener.onFailure(e)
        }
    }

    fun cancelDownLoad() {
        Timber.tag(TAG).d("cancelDownLoad")
        isCanceled.set(true)
    }

    private inner class ProgressResponseBody(
        private val responseBody: ResponseBody?,
        private val progressListener: Listener,
    ) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? = responseBody?.contentType()

        override fun contentLength(): Long = responseBody?.contentLength() ?: 0

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody!!.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                private var totalBytesRead = 0L
                private var lastUpdateTimeMillis: Long = 0
                private var lastDownloadedBytes: Long = 0

                @SuppressLint("BinaryOperationInTimber")
                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (isCanceled.get()) return -1L // Pause download

                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0

                    // Update speed and time left every second
                    val currentTimeMillis = System.currentTimeMillis()
                    if (currentTimeMillis - lastUpdateTimeMillis >= 1000) {
                        val elapsedTimeSeconds = (currentTimeMillis - lastUpdateTimeMillis) / 1000f
                        val downloadedBytesThisSecond =
                            (totalBytesRead - lastDownloadedBytes).coerceAtLeast(1)
                        val downloadSpeed = downloadedBytesThisSecond / elapsedTimeSeconds
                        val fileSize = contentLength().coerceAtLeast(1)
                        val remainingBytes = fileSize - totalBytesRead
                        val remainingTimeSeconds = remainingBytes / downloadSpeed

                        lastUpdateTimeMillis = currentTimeMillis
                        lastDownloadedBytes = totalBytesRead

                        progressListener.onUpdate(
                            readLength = startBytes + totalBytesRead,
                            totalLength = startBytes + responseBody!!.contentLength(),
                            speed = downloadSpeed.toLong(),
                            timeLeft = remainingTimeSeconds.toLong(),
                        )
                        Timber.tag(TAG).d(
                            "onUpdate: " +
                                    "readLength ${startBytes + totalBytesRead}, " +
                                    "totalLength ${startBytes + responseBody.contentLength()}, " +
                                    "downloadSpeeds $downloadSpeed, " +
                                    "remainingTimeSeconds $remainingTimeSeconds.",
                        )
                    }
                    return bytesRead
                }
            }
        }
    }
}
