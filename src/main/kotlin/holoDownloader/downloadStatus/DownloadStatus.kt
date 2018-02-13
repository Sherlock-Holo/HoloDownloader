package holoDownloader.downloadStatus

import holoDownloader.errorFlag.ErrorFlag
import org.jline.terminal.TerminalBuilder
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

class DownloadStatus(
        private val downloaded: AtomicInteger,
        private val contentLength: Long,
        private val speedInterval: Long,
        private val errorFlag: ErrorFlag
) : Runnable {

    private val percentFormat = DecimalFormat("0.00")
    private val speedFormat = DecimalFormat("#,###")

    private val terminal = TerminalBuilder.terminal()

    override fun run() {
        if (errorFlag.isError) {
            println()
            println("download failed")
            return
        }

        val startTime = System.currentTimeMillis()

        var lastTimeLength = 0

        val sb = StringBuilder()

        while (lastTimeLength < contentLength) {
            val consoleColumns = terminal.size.columns

            val percentText = percentFormat.format(downloaded.toDouble() / contentLength.toDouble() * 100) + "%"
            val speedText = speedFormat.format((downloaded.toInt() - lastTimeLength) / 1024 * (1000 / speedInterval.toDouble())) + " KiB/s"

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

            if (percentText == "100.00%") {
                println()
                println()
                val endTime = System.currentTimeMillis()
                val averageSpeed = downloaded.toDouble() / (endTime - startTime) / 1024 * 1000
                println("done, use time: ${(endTime - startTime) / 1_000} seconds, " +
                        "average speed: ${percentFormat.format(averageSpeed)} KiB/s")
                break
            }

            print('\r')

            lastTimeLength = downloaded.toInt()

            Thread.sleep(speedInterval)
        }
    }
}