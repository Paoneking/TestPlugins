package com.paoneking

import com.lagradost.cloudstream3.MainPageRequest
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
    val veexProvider = VeexSonyLivProvider()
    veexProvider.mainPage.forEach {
        val ss = veexProvider.getMainPage(
            1,
            MainPageRequest(
                it.name,
                it.data,
                false
            )
        )
        /*val json = """
            {"id":4265,"type":"serie","title":"Squid Game","label":null,"sublabel":null,"description":"Hundreds of cash-strapped players accept a strange invitation to compete in children's games. Inside, a tempting prize awaits — with deadly high stakes.","year":2021,"imdb":7.862,"comment":true,"rating":0,"duration":"3 Seasons","downloadas":"1","playas":"1","classification":null,"image":"https://netflix.veex.cc/uploads/cache/poster_thumb/uploads/jpg/960c8422ffd42361cfdf07f427b4ab12.jpg","cover":"https://netflix.veex.cc/uploads/cache/cover_thumb/uploads/jpg/cb061fb71e28cf56fc0d6a73cfcc62c4.jpg","genres":[{"id":2,"title":"Drama"},{"id":13,"title":"Mystery"},{"id":20,"title":"Action & Adventure"},{"id":28,"title":"New on Netflix"}],"trailer":{"id":14768,"type":"youtube","url":"https://www.youtube.com/watch?v=oqxAJKy0ii4"},"sources":[]}
        """.trimIndent()
        val ss = veexProvider.load(json)*/
        println("ss: $ss")
    }
}