package com.paoneking

import com.lagradost.api.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse

class VeexProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var name = "Veex Netflix"
    override val hasMainPage = true
    override var lang = "ne"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        BASE_URL.format("movie") to "Movies",
//        BASE_URL.format("serie") to "Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        runCatching {
            Log.d("veex", "getMainPage")
            val res = app.get(request.data).parsedSafe<List<MovieItem>>()
            val searchResponses: List<SearchResponse> = res?.map { it.toSearchResponse() }
                ?: throw ErrorLoadingException("Invalid JSON response")
            Log.d("veex", "searchResponses: $searchResponses")
            val homePage =  newHomePageResponse(
                request, searchResponses
            )
            Log.d("veex", "homePage: $homePage")
            return homePage
        }.getOrElse {
            Log.e("veex", "error: $it")
            throw it
        }
    }

    private fun MovieItem.toSearchResponse(): SearchResponse {
        val movieSearchResponse = newMovieSearchResponse(
            title,
            id.toString(),
            TvType.Movie
        ) {
            this.posterUrl = cover
        }
        Log.d("veex", "movieSearchResponse: $movieSearchResponse")
        return movieSearchResponse
    }

    companion object {
        const val BASE_URL =
            "https://netflix.veex.cc/api/%s/by/filtres/0/created/0/4F5A9C3D9A86FA54EACEDDD635185/26a3547f-6db2-44f3-b4c8-3b8dcf1e871a/"
    }
}