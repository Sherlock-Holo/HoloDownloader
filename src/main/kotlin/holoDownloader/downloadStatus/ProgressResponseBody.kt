package holoDownloader.downloadStatus

import okhttp3.ResponseBody
import okio.*
import java.util.concurrent.atomic.AtomicLong

class ProgressResponseBody(private val responseBody: ResponseBody, private val downloaded: AtomicLong) : ResponseBody() {
    private lateinit var bufferedSource: BufferedSource


    private fun updateStatus(length: Long) {
        downloaded.addAndGet(length)
    }

    override fun contentLength() = responseBody.contentLength()

    override fun contentType() = responseBody.contentType()

    override fun source(): BufferedSource {
        return try {
            bufferedSource
        } catch (e: UninitializedPropertyAccessException) {
            bufferedSource = Okio.buffer(source(responseBody.source()))!!
            bufferedSource
        }
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            override fun read(sink: Buffer, byteCount: Long): Long {
                val length = super.read(sink, byteCount)
                updateStatus(length)
                return length
            }
        }
    }
}