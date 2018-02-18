package holoDownloader.downloader

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.*
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicInteger

class MultiDownloader(private val client: OkHttpClient,
                      private val requests: Array<Fragment>,
                      private val file: File,
                      private val downloaded: AtomicInteger,
                      private val errorFlag: ErrorFlag
) : Downloader {

    override fun startDownload() {
        requests.forEach {
            val request = it.request
            val startPos = it.startPos
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.seek(startPos)

            val call = client.newCall(request)

            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response?) {
                    val body = try {
                        response!!.body() ?: TODO()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        errorFlag.isError = true
                        return
                    }

                    val bufferedInputStream = BufferedInputStream(body.byteStream())
                    val contentLength = body.contentLength()
                    var contentRead = 0
                    val buffer = ByteArray(8192)

                    try {
                        while (contentRead < contentLength) {
                            val length = bufferedInputStream.read(buffer)
                            if (length < 0) {
                                errorFlag.isError = true
                                break
                            }

                            randomAccessFile.write(buffer, 0, length)
                            downloaded.addAndGet(length)
                            contentRead += length
                        }

                        println("fragment done")
                        
                    } catch (e: IOException) {
                        e.printStackTrace()
                        errorFlag.isError = true
                        return

                    } finally {
                        bufferedInputStream.close()
                        randomAccessFile.close()
                        response.close()
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    e!!.printStackTrace()
                }
            })
        }
    }

    data class Fragment(val request: Request, val startPos: Long)
}