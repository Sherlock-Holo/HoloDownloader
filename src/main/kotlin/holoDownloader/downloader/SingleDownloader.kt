package holoDownloader.downloader

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.*
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.IOException

class SingleDownloader(
        private val client: OkHttpClient,
        private val request: Request,
        private val file: File,
        private val errorFlag: ErrorFlag
) : Downloader, Pauseable {

    private lateinit var resp: Response

    var isFinish = false
        private set

    override fun startDownload() {
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                resp = response

                var bodySource: BufferedSource? = null
                var fileSink: BufferedSink? = null

                try {
                    val body = response.body() ?: TODO()

                    bodySource = body.source()
                    fileSink = Okio.buffer(Okio.sink(file))

                    fileSink.writeAll(bodySource!!)
                    isFinish = true

                } catch (e: IOException) {
                    errorFlag.isError = true
                    e.printStackTrace()
                    return
                } finally {
                    bodySource?.close()
                    fileSink?.close()
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    override fun pauseDownload() {
        resp.close()
    }

    override fun continueDownload() {
        startDownload()
    }
}