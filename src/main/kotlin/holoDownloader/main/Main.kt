package holoDownloader.main

import holoDownloader.cmdParser.CmdParser
import holoDownloader.downloadManager.DownloadManager


fun main(args: Array<String>) {
    val parser = CmdParser()
    parser.addParameter("-o")
    parser.addParameter("-n")
    parser.parse(args)

    val link = parser.link
    val threadNum =
            if (parser.getParameter("-n") != null) parser.getParameter("-n")!!.toInt()
            else 16

    if (link == null) {
        println("no download link or error download link")
        System.exit(1)
    }

    val downloader = DownloadManager(link!!, threadNum, parser.getParameter("-o"), 500)
    downloader.download()
}