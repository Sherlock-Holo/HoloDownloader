package holoDownloader.main

import holoDownloader.downloader.Downloader

fun main(args: Array<String>) {
    val downloader = Downloader("https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.7/releases/x86_64/alpine-minirootfs-3.7.0-x86_64.tar.gz", 2, "/tmp/test.tgz")
    downloader.download()
}