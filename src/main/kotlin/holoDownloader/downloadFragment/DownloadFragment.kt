package holoDownloader.downloadFragment

import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.URL

class DownloadFragment(private val url: String, private val cursor: Long, private val size: Long, private val file: File) : Runnable {

    override fun run() {
        val conn = URL(url).openConnection()
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=$cursor-${cursor + size - 1}")

        val fragmentLength = conn.contentLength.toLong()

        if (fragmentLength != size) throw Exception("fragmentLength != size")

        val bufferedInputStream = BufferedInputStream(conn.inputStream)

        val randomAccessFile = RandomAccessFile(file, "rw")

        assert(file.exists())

        randomAccessFile.seek(cursor)

        val buffer = ByteArray(8192)

        var contentLength = 0

        while (contentLength < size) {
            val length = bufferedInputStream.read(buffer, 0, buffer.size)

            if (length < 0) TODO()

            randomAccessFile.write(buffer, 0, length)
            contentLength += length
        }
        bufferedInputStream.close()
        randomAccessFile.close()

        println("finish，length: $size bytes")
    }
}