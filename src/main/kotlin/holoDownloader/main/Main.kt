package holoDownloader.main

import holoDownloader.cmdParser.CmdParser
import holoDownloader.downloader.Downloader

fun main(args: Array<String>) {
    val parser = CmdParser()
    parser.addParameter("-o")
    parser.addParameter("-u")
    parser.addParameter("-n")
    parser.parse(args)

    val link = parser.getParameter("-u")
    val threadNum =
            if (parser.getParameter("-n") != null) parser.getParameter("-n")!!.toInt()
            else 10

    if (link == null) throw Throwable("no download link or error download link")

    val downloader = Downloader(link, threadNum, parser.getParameter("-o"))
    downloader.download()
}