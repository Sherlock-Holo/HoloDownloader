package holoDownloader.downloader

import holoDownloader.downloadFragment.DownloadFragment
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicIntegerArray

class Downloader(val url: String, val threadNum: Int, val filePath: String) {
    val smallFileSize = 4194304 // 4 MiB
    private val downloadLog = AtomicIntegerArray(intArrayOf(0, 0))

    fun download() {
        val url = URL(this.url)

        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=0-")

        val responseCode = conn.responseCode
        val contentLength = conn.contentLength.toLong()

        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }

        if (responseCode != 206 || contentLength <= smallFileSize) {
            if (singleDownload(conn, contentLength, file)) println("finish, length: $contentLength bytes")
            return
        }

        println("file name: ${url.file.split('/').last()}")

        val randomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.setLength(contentLength)

        val fragmentLength = contentLength / threadNum

        val lastFragmentLength =
                if (fragmentLength * threadNum != contentLength) contentLength - fragmentLength * threadNum + fragmentLength
                else fragmentLength

        for (i in 0 until threadNum) {
            if (i != threadNum - 1) {
                Thread(DownloadFragment(this.url, i * fragmentLength, fragmentLength, file)).start()
            } else {
                Thread(DownloadFragment(this.url, i * fragmentLength, lastFragmentLength, file)).start()
            }
        }
    }

    private fun singleDownload(conn: HttpURLConnection, contentLength: Long, file: File): Boolean {
        val buffer = ByteArray(8192)
        val bufferedInputStream = BufferedInputStream(conn.inputStream)
        val fileOutputStream = FileOutputStream(file)

        var length = 0
        while (length < contentLength) {
            val dataLength = bufferedInputStream.read(buffer, 0, buffer.size)
            if (dataLength < 0) TODO()

            fileOutputStream.write(buffer, 0, dataLength)
            length += dataLength
        }
        return true
    }
}