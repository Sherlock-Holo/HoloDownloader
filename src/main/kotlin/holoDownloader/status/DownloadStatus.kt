package holoDownloader.status

import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class DownloadStatus(private val downloaded: AtomicInteger, private val contentLength: Long) : Runnable {
    private val percentFormat = DecimalFormat("0.00")
    private val speedFormat = DecimalFormat("#,###")

    override fun run() {
        var lastTimeLength = 0
        val startTime = Instant.now()
        do {
            val percent = percentFormat.format(downloaded.toDouble() / contentLength.toDouble() * 100)
            val speed = speedFormat.format((downloaded.toInt() - lastTimeLength) / 1024)

            println("donwload status: $percent%, speed: $speed KB/s")

            if (percent == "100.00") {
                println("done, use time: ${Duration.between(startTime, Instant.now()).seconds} seconds")
                break
            }

            lastTimeLength = downloaded.toInt()

            Thread.sleep(1000)

        } while (lastTimeLength < contentLength)
    }
}