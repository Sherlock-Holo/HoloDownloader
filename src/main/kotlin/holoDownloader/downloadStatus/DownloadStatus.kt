package holoDownloader.downloadStatus

import holoDownloader.errorFlag.ErrorFlag
import okhttp3.OkHttpClient
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicLong

class DownloadStatus(
        private val downloaded: AtomicLong,
        private val contentLength: Long,
        private val speedInterval: Long,
        private val errorFlag: ErrorFlag,
        private val file: File,
        private val tmpFile: File,
        private val client: OkHttpClient
) : Runnable {

    private val percentFormat = DecimalFormat("0.00")
    private val speedFormat = DecimalFormat("#,###")

    private val terminal = TerminalBuilder.terminal()

    override fun run() {
        val startTime = System.currentTimeMillis()

        var lastTimeLength = 0L

        val sb = StringBuilder()

        while (lastTimeLength < contentLength) {
            if (errorFlag.isError) {
                println()
                println("download failed")
                return
            }

            val percentText =
                    percentFormat.format(downloaded.toDouble() / contentLength.toDouble() * 100) + "%"

            val speedText =
                    speedFormat.format((downloaded.toInt() - lastTimeLength) / 1024 * (1000 / speedInterval.toDouble())) + " KiB/s"

            display(sb, percentText, speedText)

            lastTimeLength = downloaded.toLong()

            if (isFinish(startTime, lastTimeLength)) return

            print('\r')

            Thread.sleep(speedInterval)
        }
    }

    private fun display(sb: StringBuilder, percentText: String, speedText: String) {
        val consoleColumns = terminal.size.columns

        val percent = percentText.substring(0, percentText.lastIndex).toDouble() / 100

        val statusBarLength = consoleColumns - 2 - 3 - percentText.length - speedText.length

        sb.delete(0, sb.length)

        sb.append("[")

        val statusLength = (percent * statusBarLength).toInt()

        for (i in 0 until statusLength) {
            if (i < statusLength - 1) sb.append('=')
            else sb.append('>')
        }

        for (i in 0 until statusBarLength - statusLength) {
            sb.append(' ')
        }

        sb.append("] $percentText  $speedText")

        print(sb.toString())
    }

    private fun isFinish(startTime: Long, lastTimeLength: Long): Boolean {
        return if (lastTimeLength == contentLength) {
            println()
            println()
            val endTime = System.currentTimeMillis()
            val averageSpeed = downloaded.toDouble() / (endTime - startTime) / 1024 * 1000
            println("done, use time: ${(endTime - startTime) / 1_000} seconds, " +
                    "average speed: ${percentFormat.format(averageSpeed)} KiB/s")

            tmpFile.renameTo(file)

            client.dispatcher().executorService().shutdown()
            true
        } else false
    }
}