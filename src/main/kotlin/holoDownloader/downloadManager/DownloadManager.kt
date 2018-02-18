package holoDownloader.downloadManager

import holoDownloader.downloadStatus.DownloadStatus
import holoDownloader.downloadStatus.ProgressInterceptor
import holoDownloader.downloader.MultiDownloader
import holoDownloader.downloader.SingleDownloader
import holoDownloader.errorFlag.ErrorFlag
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DownloadManager(private val url: String,
                      private val threadNum: Int,
                      private var filePath: String?,
                      private val speedInterval: Long = 1000
) {

    var smallFileSize = 4194304L // 4 MiB
    private val downloaded = AtomicLong(0)
    private val client =
            OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(ProgressInterceptor(downloaded))
                    .retryOnConnectionFailure(true)
                    .build()

    private val errorFlag = ErrorFlag()

    fun download() {
        val request = Request.Builder().url(url).header("Range", "bytes=0-").build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        val contentLength = response.body()!!.contentLength()
        var file =
                if (filePath == null) {
                    File(url.split('/').last())
                } else File(filePath)

        if (file.exists()) {
            print("file already exists, delete it? [y/yes, n/no]: ")

            val answer = System.`in`.bufferedReader().readLine().toLowerCase()

            when (answer) {
                "y", "yes" -> {
                    file.renameTo(File(file.path + ".backup"))

                    file =
                            if (filePath == null) File(url.split('/').last())
                            else File(filePath)

                    println("original file is ${file.path + ".backup"}")
                    println()
                }

                else -> {
                    file.delete()
                    println()
                }
            }
        }

        val randomAccessFile = RandomAccessFile(file, "rw")

        randomAccessFile.setLength(contentLength)

        when {
            contentLength <= smallFileSize || response.code() != 206 -> {
                println("single mode")
                response.close()
                singleDownload(request, file)
            }
            else -> {
                println("$threadNum threads mode")
                response.close()
                multiDownload(file, contentLength)
            }
        }

        Thread(DownloadStatus(downloaded, contentLength, speedInterval, errorFlag, client)).start()
    }


    private fun singleDownload(request: Request, file: File) {
        SingleDownloader(client, request, file, errorFlag).startDownload()
    }

    private fun multiDownload(file: File, contentLength: Long) {
        val fragmentLength = contentLength / threadNum

        val requests = Array(threadNum) {
            val builder = Request.Builder().url(url)
            val startPos = it * fragmentLength

            val request =
                    if (it != threadNum - 1) builder.header("Range", "bytes=$startPos-${startPos + fragmentLength - 1}").build()
                    else builder.header("Range", "bytes=$startPos-").build()

            MultiDownloader.Fragment(request, startPos)
        }

        MultiDownloader(client, requests, file, errorFlag).startDownload()
    }
}