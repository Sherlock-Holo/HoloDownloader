package holoDownloader.downloader

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.*
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class MultiDownloader(private val client: OkHttpClient,
                      private val requests: Array<Fragment>,
                      private val file: File,
                      private val errorFlag: ErrorFlag
) : Downloader, Pauseable {

    private var stop = false

    val isFinish: Boolean
        get() = requests.all { it.finish }

    override fun startDownload() {
        requests.forEach {
            val request = it.request
            val startPos = it.startPos
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.seek(startPos)

            val call = client.newCall(request)

            call.enqueue(DownloadCallBack(it, randomAccessFile))
        }
    }

    override fun pauseDownload() {
        stop = true
    }

    override fun continueDownload() {
        stop = false
        startDownload()
    }

    inner class Fragment(val request: Request, var startPos: Long) {
        var finish = false
    }

    private inner class DownloadCallBack(private val fragment: Fragment, private val randomAccessFile: RandomAccessFile) : Callback {
        override fun onResponse(call: Call, response: Response) {
            var bufferedInputStream: BufferedInputStream? = null

            try {
                val body = response.body() ?: TODO()

                bufferedInputStream = BufferedInputStream(body.byteStream())
                val contentLength = body.contentLength()
                var contentRead = 0L
                val buffer = ByteArray(8192)

                while (contentRead < contentLength) {
                    if (stop) return

                    val length = bufferedInputStream.read(buffer)
                    if (length < 0) {
                        errorFlag.isError = true
                        return
                    }

                    fragment.startPos += length

                    randomAccessFile.write(buffer, 0, length)
                    contentRead += length
                }

                fragment.finish = true

            } catch (e: IOException) {
                e.printStackTrace()
                errorFlag.isError = true
                return

            } finally {
                bufferedInputStream?.close()
                randomAccessFile.close()
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }
    }
}