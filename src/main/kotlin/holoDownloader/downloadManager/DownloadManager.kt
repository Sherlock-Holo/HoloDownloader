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
                      timeout: Long,
                      private val speedInterval: Long = 1000
) {

    var smallFileSize = 4194304L // 4 MiB
    private val downloaded = AtomicLong(0)
    private val client =
            OkHttpClient.Builder()
                    .connectTimeout(timeout, TimeUnit.SECONDS)
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
        val fileName = filePath ?: url.split('/').last()
        val tmpFileName = fileName + ".tmp"

        val file = checkFile(fileName)
        val tmpFile = File(tmpFileName)

        if (tmpFile.exists()) tmpFile.delete()

        val randomAccessFile = RandomAccessFile(tmpFile, "rw")

        randomAccessFile.setLength(contentLength)

        when {
            contentLength <= smallFileSize || response.code() != 206 -> {
                println("single mode")
                response.close()
                singleDownload(request, tmpFile)
            }
            else -> {
                println("$threadNum threads mode")
                response.close()
                multiDownload(tmpFile, contentLength)
            }
        }

        Thread(DownloadStatus(downloaded, contentLength, speedInterval, errorFlag, file, tmpFile, client)).start()
    }


    private fun singleDownload(request: Request, tmpFile: File) {
        SingleDownloader(client, request, tmpFile, errorFlag).startDownload()
    }

    private fun multiDownload(tmpFile: File, contentLength: Long) {
        val fragmentLength = contentLength / threadNum

        val requests = Array(threadNum) {
            val builder = Request.Builder().url(url)
            val startPos = it * fragmentLength

            val request =
                    if (it != threadNum - 1) builder.header("Range", "bytes=$startPos-${startPos + fragmentLength - 1}").build()
                    else builder.header("Range", "bytes=$startPos-").build()

            MultiDownloader.Fragment(request, startPos)
        }

        MultiDownloader(client, requests, tmpFile, errorFlag).startDownload()
    }

    private fun checkFile(fileName: String): File {
        val file = File(fileName)
        if (file.exists()) {
            print("file already exists, delete it? [y/yes, n/no]: ")

            val answer = System.`in`.bufferedReader().readLine().toLowerCase()

            when (answer) {
                "n", "no" -> {
                    file.renameTo(File(file.path + ".backup"))

                    println("original file is ${file.path + ".backup"}")
                    println()

                    return File(fileName)
                }

                else -> {
                    file.delete()
                    println()
                    return file
                }
            }
        } else return file
    }
}