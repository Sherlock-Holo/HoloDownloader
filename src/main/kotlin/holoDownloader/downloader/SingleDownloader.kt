package holoDownloader.downloader

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

class SingleDownloader(
        private val client: OkHttpClient,
        private val request: Request,
        private val file: File,
        private val errorFlag: ErrorFlag
) : Downloader {

    override fun startDownload() {
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                var httpInputStream: BufferedInputStream? = null
                var fileOutputStream: BufferedOutputStream? = null

                try {
                    val body = response.body() ?: TODO()

                    httpInputStream = BufferedInputStream(body.byteStream())
                    fileOutputStream = BufferedOutputStream(file.outputStream())

                    httpInputStream.transferTo(fileOutputStream)
                } catch (e: IOException) {
                    errorFlag.isError = true
                    e.printStackTrace()
                    return
                } finally {
                    fileOutputStream?.close()
                    httpInputStream?.close()
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }
}