package holoDownloader.downloader

import holoDownloader.downloadFragment.FragmentDownload
import holoDownloader.errorStatus.ErrorStatus
import holoDownloader.status.DownloadStatus
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class Downloader(val url: String, val threadNum: Int, var filePath: String?) {
    val smallFileSize = 4194304 // 4 MiB
    private val downloaded = AtomicInteger(0)

    fun download() {
        val url = URL(this.url)

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=0-")

        val responseCode = conn.responseCode
        val contentLength = conn.contentLength.toLong()
        val downloadFileName = url.file.split('/').last()

        if (filePath == null) filePath = downloadFileName

        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }

        when {
            responseCode != 206 -> {
                println("server not support multi threads download, use single mode")
                singleDownload(conn, contentLength, file)
            }

            contentLength <= smallFileSize -> {
                println("download file is smaller than 4MB, use single thread mode")
                singleDownload(conn, contentLength, file)
            }

            else -> {
                println("use $threadNum threads to download file")
                multiDownload(file, contentLength)
            }
        }

        /*if (responseCode != 206 || contentLength <= smallFileSize) {
            println("download file is smaller than 4MB, use single thread mode")
            singleDownload(conn, contentLength, file)
        } else {
            println("use $threadNum threads to download file")
            multiDownload(file, contentLength)
        }*/

        // print the download status
        Thread(DownloadStatus(downloaded, contentLength)).start()
    }


    private fun singleDownload(conn: HttpURLConnection, contentLength: Long, file: File) {
        val buffer = ByteArray(8192)
        val bufferedInputStream = BufferedInputStream(conn.inputStream)
        val fileOutputStream = FileOutputStream(file)

        var length = 0
        try {
            while (length < contentLength) {
                val dataLength = bufferedInputStream.read(buffer, 0, buffer.size)
                if (dataLength < 0) {
                    println("download failed")
                    break
                }

                fileOutputStream.write(buffer, 0, dataLength)
                length += dataLength
                downloaded.addAndGet(length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bufferedInputStream.close()
            fileOutputStream.close()
        }
    }

    private fun multiDownload(file: File, contentLength: Long) {
        val randomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.setLength(contentLength)

        val errorFlag = ErrorStatus()

        val fragmentLength = contentLength / threadNum

        val lastFragmentLength =
                if (fragmentLength * threadNum != contentLength) contentLength - fragmentLength * threadNum + fragmentLength
                else fragmentLength

        for (i in 0 until threadNum) {
            if (i != threadNum - 1) {
                Thread(FragmentDownload(this.url, i * fragmentLength, fragmentLength, file, downloaded, errorFlag)).start()
            } else {
                Thread(FragmentDownload(this.url, i * fragmentLength, lastFragmentLength, file, downloaded, errorFlag)).start()
            }
        }
    }
}