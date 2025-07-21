package com.paoneking

import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlinx.coroutines.runBlocking

class VeexZee5Provider: VeexProvider("zee5") {
    override var name: String = "Veex Zee5"
}

class VeexNetFlixProvider: VeexProvider("netflix") {
    override var name: String = "Veex Netflix"
}

class VeexPrimeProvider: VeexProvider("prime") {
    override var name: String = "Veex Prime"
}

class VeexHotstarProvider: VeexProvider("hotstar") {
    override var name: String = "Veex Hotstar"
}

class VeexSonyLivProvider: VeexProvider("sonyliv") {
    override var name: String = "Veex SonyLiv"
}

fun main() = runBlocking {
    val playlistUrl = "https://live9.veex.cc/A-Brother-And-7-Siblings-TT32881480-2025/file-Transcode/playlist.m3u8"

    val veexProvider = VeexSonyLivProvider()
    veexProvider.invokeSubtitles(playlistUrl) {
        println("lang: ${it.lang}, url: ${it.url}")
    }
    /*veexProvider.mainPage.forEach {
        val ss = veexProvider.getMainPage(
            1,
            MainPageRequest(
                it.name,
                it.data,
                false
            )
        )
        println("ss: $ss")
    }*/
}