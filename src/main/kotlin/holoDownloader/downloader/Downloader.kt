package holoDownloader.downloader

import holoDownloader.downloadFragment.DownloadFragment
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class Downloader(val url: String, val threadNum: Int, val filePath: String) {

    fun download() {
        val url = URL(this.url)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=1-")

        val responseCode = conn.responseCode
        val contentLength = conn.contentLength.toLong()

        println("file length is $contentLength bytes")

        if (responseCode != 206) TODO("not support multi threads")


        val file = File(filePath)

        if (file.exists()) {
            file.delete()
        }

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
}