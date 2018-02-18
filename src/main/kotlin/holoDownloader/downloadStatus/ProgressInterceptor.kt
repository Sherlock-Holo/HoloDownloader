package holoDownloader.downloadStatus

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicLong

class ProgressInterceptor(private val downloaded: AtomicLong) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder()
                .body(ProgressResponseBody(response.body()!!, downloaded))
                .build()
    }
}