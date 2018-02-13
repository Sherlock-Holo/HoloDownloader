package holoDownloader.fragmentDownload

import holoDownloader.errorStatus.ErrorStatus
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL
import java.net.URLConnection
import java.net.UnknownServiceException
import java.util.concurrent.atomic.AtomicInteger

class FragmentDownload(
        private val url: String,
        private var cursor: Long,
        private val size: Long,
        private val file: File,
        private val downloaded: AtomicInteger,
        private val errorFlag: ErrorStatus
) : Runnable {

    private val endPos = cursor + size - 1

    override fun run() {
        for (i in 0 until 5) {
            val conn = connect()

            if (conn == null) {
                errorFlag.isError = true
                return
            }

            setCursor(conn, cursor)

            val bufferedInputStream = try {
                BufferedInputStream(conn.inputStream)
            } catch (e: IOException) {
                continue
            } catch (e: UnknownServiceException) {
                continue
            }

            val randomAccessFile = RandomAccessFile(file, "rw")

            try {
                randomAccessFile.seek(cursor)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }

            val readLength = readData(randomAccessFile, bufferedInputStream)

            if (readLength != null) {
                cursor += readLength
            } else break

            if (cursor >= endPos) return
        }

        errorFlag.isError = true
        println("download failed")
    }

    private fun connect(): URLConnection? {
        for (i in 0 until 5) {
            if (errorFlag.isError) break

            try {
                return URL(url).openConnection()
            } catch (e: IOException) {
            }
        }
        return null
    }

    private fun setCursor(conn: URLConnection, cursor: Long) {
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("Range", "bytes=$cursor-$endPos")
    }

    private fun readData(randomAccessFile: RandomAccessFile, bufferedInputStream: BufferedInputStream): Int? {
        val buffer = ByteArray(8192)

        var contentLength = 0

        try {
            while (contentLength < size) {
                if (errorFlag.isError) return null

                val length = bufferedInputStream.read(buffer, 0, buffer.size)

                if (length < 0) {
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

        return contentLength
    }
}