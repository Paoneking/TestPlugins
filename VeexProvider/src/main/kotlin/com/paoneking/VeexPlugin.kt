package com.paoneking

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class VeexPlugin : Plugin() {

    override fun load() {
        // All providers should be added in this manner
        registerMainAPI(VeexNetFlixProvider())
        registerMainAPI(VeexPrimeProvider())
        registerMainAPI(VeexZee5Provider())
        registerMainAPI(VeexHotstarProvider())
        registerMainAPI(VeexSonyLivProvider())
    }
}