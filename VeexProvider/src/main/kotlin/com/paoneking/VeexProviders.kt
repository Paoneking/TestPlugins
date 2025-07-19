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
        println("ss: $ss")
    }
}