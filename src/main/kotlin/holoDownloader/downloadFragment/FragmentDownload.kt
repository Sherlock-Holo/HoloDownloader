package holoDownloader.downloadFragment

import holoDownloader.errorStatus.ErrorStatus
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL
import java.net.UnknownServiceException
import java.util.concurrent.atomic.AtomicInteger

class FragmentDownload(
        private val url: String,
        private val cursor: Long,
        private val size: Long,
        private val file: File,
        private val downloaded: AtomicInteger,
        private val errorFlag: ErrorStatus
) : Runnable {

    override fun run() {
        val conn = try {
            URL(url).openConnection()
        } catch (e: IOException) {
            println("download failed")
            errorFlag.isError = true
            return
        }

        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=$cursor-${cursor + size - 1}")

        val fragmentLength = conn.contentLength.toLong()

        if (fragmentLength != size) throw Exception("fragmentLength != size")

        val bufferedInputStream = try {
            BufferedInputStream(conn.inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        } catch (e: UnknownServiceException) {
            e.printStackTrace()
            return
        }

        val randomAccessFile = RandomAccessFile(file, "rwd")

        try {
            randomAccessFile.seek(cursor)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        val buffer = ByteArray(8192)

        var contentLength = 0

        try {
            while (contentLength < size) {
                if (errorFlag.isError) break

                val length = bufferedInputStream.read(buffer, 0, buffer.size)

                if (length < 0) {
                    println("download failed")
                    errorFlag.isError = true
                    break
                }

                randomAccessFile.write(buffer, 0, length)
                contentLength += length
                downloaded.addAndGet(length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bufferedInputStream.close()
            randomAccessFile.close()
        }
    }
}