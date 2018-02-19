package holoDownloader.downloader

interface Pauseable {
    fun pauseDownload()

    fun continueDownload()
}