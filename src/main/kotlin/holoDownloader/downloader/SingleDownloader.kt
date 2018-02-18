package holoDownloader.downloader

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class SingleDownloader(
        private val client: OkHttpClient,
        private val request: Request,
        private val file: File,
        private val contentLength: Long,
        private val downloaded: AtomicInteger,
        private val errorFlag: ErrorFlag
) : Downloader {

    override fun startDownload() {
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = try {
                    response!!.body() ?: TODO()
                } catch (e: IOException) {
                    e.printStackTrace()
                    errorFlag.isError = true
                    return
                }

                val httpInputStream = BufferedInputStream(body.byteStream())
                val fileOutputStream = BufferedOutputStream(file.outputStream())
                val buffer = ByteArray(8192)
                var contentRead = 0
                try {
                    while (contentRead < contentLength) {
                        val length = httpInputStream.read(buffer)
                        if (length < 0) {
                            errorFlag.isError = true
                            break
                        }

                        fileOutputStream.write(buffer, 0, length)
                        downloaded.addAndGet(length)
                        contentRead += length
                    }
                } catch (e: IOException) {
                    errorFlag.isError = true
                    e.printStackTrace()
                    return
                } finally {
                    fileOutputStream.close()
                    httpInputStream.close()
                    response.close()
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                e!!.printStackTrace()
            }
        })
    }
}