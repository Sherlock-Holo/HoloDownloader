package holoDownloader.main

import holoDownloader.cmdParser.CmdParser
import holoDownloader.downloadManager.DownloadManager


fun main(args: Array<String>) {
    val parser = CmdParser()
    parser.addParameter("-o")
    parser.addParameter("-n")
    parser.addParameter("--timeout")
    parser.parse(args)

    val link = parser.link

    val threadNum = parser.getParameter("-n")?.toInt() ?: 16

    val timeout = parser.getParameter("timeout")?.toLong() ?: 0

    if (link == null) {
        println("no download link or error download link")
        System.exit(1)
    }

    val downloader = DownloadManager(link!!, threadNum, parser.getParameter("-o"), timeout, 500)
    downloader.download()
}