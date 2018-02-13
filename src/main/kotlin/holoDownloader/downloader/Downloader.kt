package holoDownloader.downloader

import holoDownloader.downloadFragment.FragmentDownload
import holoDownloader.errorStatus.ErrorStatus
import holoDownloader.status.DownloadStatus
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class Downloader(private val url: String, private val threadNum: Int, private var filePath: String?) {
    var smallFileSize = 4194304 // 4 MiB
    private val downloaded = AtomicInteger(0)

    fun download() {
        val url = URL(this.url)

        val conn = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            println("open connection failed")
            return
        }

        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=0-")

        val responseCode = try {
            conn.responseCode
        } catch (e: IOException) {
            println("get response code failed")
            return
        }

        val contentLength = conn.contentLength.toLong()
        val downloadFileName = url.file.split('/').last()

        if (filePath == null) filePath = downloadFileName

        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }

        when {
            responseCode != 206 -> {
                println("server not support multi threads download, use single mode\n")
                singleDownload(conn, contentLength, file)
            }

            contentLength <= smallFileSize -> {
                println("download file is smaller than 4MB, use single thread mode\n")
                singleDownload(conn, contentLength, file)
            }

            else -> {
                conn.disconnect()
                println("download Threads: $threadNum\n")
                multiDownload(file, contentLength)
            }
        }

        // print the download status
        Thread(DownloadStatus(downloaded, contentLength)).start()
    }


    private fun singleDownload(conn: HttpURLConnection, contentLength: Long, file: File) {
        val buffer = ByteArray(8192)
        val bufferedInputStream = BufferedInputStream(conn.inputStream)
        val bufferedOutputStream = BufferedOutputStream(file.outputStream(), 8192)

        var length = 0
        try {
            while (length < contentLength) {
                val dataLength = bufferedInputStream.read(buffer, 0, buffer.size)
                if (dataLength < 0) {
                    println("download failed")
                    break
                }

                bufferedOutputStream.write(buffer, 0, dataLength)
                length += dataLength
                downloaded.addAndGet(dataLength)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            conn.disconnect()
            bufferedOutputStream.flush()
            bufferedInputStream.close()
            bufferedOutputStream.close()
        }
    }

    private fun multiDownload(file: File, contentLength: Long) {
        val randomAccessFile = RandomAccessFile(file, "rwd")
        randomAccessFile.setLength(contentLength)
        randomAccessFile.close()

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