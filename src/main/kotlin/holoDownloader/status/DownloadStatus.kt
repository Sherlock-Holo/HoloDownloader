package holoDownloader.status

import org.jline.terminal.TerminalBuilder
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

class DownloadStatus(private val downloaded: AtomicInteger, private val contentLength: Long) : Runnable {
    private val percentFormat = DecimalFormat("0.00")
    private val speedFormat = DecimalFormat("#,###")

    private val terminal = TerminalBuilder.terminal()

    override fun run() {
        val startTime = System.currentTimeMillis()

        var lastTimeLength = 0

        val sb = StringBuilder()

        while (lastTimeLength < contentLength) {
            val consoleColumns = terminal.size.columns

            val percent = percentFormat.format(downloaded.toDouble() / contentLength.toDouble() * 100) + "%"
            val speed = speedFormat.format((downloaded.toInt() - lastTimeLength) / 1024) + " KB/s"

            val percentNum = percent.substring(0, percent.lastIndex).toDouble() / 100

            val statusBarLength = consoleColumns - 2 - 3 - percent.length - speed.length

            sb.delete(0, sb.length)

            sb.append("[")

            val statusLength = (percentNum * statusBarLength).toInt()

            for (i in 0 until statusLength) {
                if (i < statusLength - 1) sb.append('=')
                else sb.append('>')
            }

            for (i in 0 until statusBarLength - statusLength) {
                sb.append(' ')
            }

            sb.append("] $percent  $speed")

            print(sb.toString())

            if (percent == "100.00%") {
                println()
                println()
                println("done, use time: ${percentFormat.format((System.currentTimeMillis() - startTime) / 1_000)} seconds")
                break
            }

            print('\r')

            lastTimeLength = downloaded.toInt()

            Thread.sleep(1000)
        }
    }
}