package holoDownloader.downloadManager

import holoDownloader.downloadStatus.DownloadStatus
import holoDownloader.downloader.MultiDownloader
import holoDownloader.downloader.SingleDownloader
import holoDownloader.errorFlag.ErrorFlag
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DownloadManager(private val url: String,
                      private val threadNum: Int,
                      private var filePath: String?,
                      private val speedInterval: Long = 1000
) {

    var smallFileSize = 4194304L // 4 MiB
    private val downloaded = AtomicInteger(0)
    private val tmp = File("/tmp/holoDownloader.tmp")
    private val client =
            OkHttpClient.Builder()
                    .cache(Cache(tmp, smallFileSize))
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build()

    private val errorFlag = ErrorFlag()

    init {
        if (tmp.exists()) tmp.delete()
        tmp.deleteOnExit()
    }

    fun download() {
        val request = Request.Builder().url(url).header("Range", "bytes=0-").build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        val contentLength = response.body()!!.contentLength()
        val file =
                if (filePath == null) {
                    File(url.split('/').last())
                } else File(filePath)

        if (file.exists()) file.delete()

        val randomAccessFile = RandomAccessFile(file, "rw")

        randomAccessFile.setLength(contentLength)

        when {
            contentLength <= smallFileSize || response.code() != 206 -> {
                println("single mode")
                response.close()
                singleDownload(request, file, contentLength)
            }
            else -> {
                println("multi mode")
                response.close()
                multiDownload(file, contentLength)
            }
        }

//        Thread(DownloadStatus(downloaded, contentLength, speedInterval, errorFlag)).start()
    }


    private fun singleDownload(request: Request, file: File, contentLength: Long) {
        SingleDownloader(client, request, file, contentLength, downloaded, errorFlag).startDownload()
    }

    private fun multiDownload(file: File, contentLength: Long) {
        val fragmentLength = contentLength / threadNum
        val lastFragmentLength =
                if (fragmentLength * threadNum != contentLength) contentLength - fragmentLength * threadNum + fragmentLength
                else fragmentLength

        val requests = Array(threadNum) {
            val builder = Request.Builder().url(url)
            val startPos = it * fragmentLength

            val request =
                    if (it != threadNum - 1) builder.header("range", "$startPos-${startPos + fragmentLength - 1}").build()
                    else builder.header("range", "$startPos-${startPos + lastFragmentLength - 1}").build()

            MultiDownloader.Fragment(request, startPos)
        }

        MultiDownloader(client, requests, file, downloaded, errorFlag).startDownload()
    }
}