package com.paoneking

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.paoneking.VeexProvider.Companion.FIRST_URL
import kotlinx.coroutines.runBlocking

class VeexProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var name = "Veex Netflix"
    override val hasMainPage = true
    override var lang = "ne"
    override val instantLinkLoading = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        FIRST_URL to "Home",
        MAIN_PAGE_URL.format("movie", 0) to "Movies",
        MAIN_PAGE_URL.format("serie", 0) to "Series",
        *genreMap.map { (id, name) ->
            MAIN_PAGE_URL.format("movie", id) to name
            MAIN_PAGE_URL.format("serie", id) to name
        }.toTypedArray()
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        println("request: $request")
        val searchResponses: List<SearchResponse>? = when (request.data) {
            FIRST_URL -> {
                println("req")
                val res = app.get(request.data).parsedSafe<FirstApiResponse>()
                println("res: $res")
                res?.slides?.map { it.toSearchResponse() }
                /*val firstResponses = res?.genre?.map {
                    HomePageList(it.title ?: "", it.posters.map {
                        it.toSearchResponse()
                    }, false)
                }
                return newHomePageResponse(HomePageList(request.name, firstResponses), false)*/
            }

            else -> {
                val res = tryParseJson<List<MovieItem>>(app.get(request.data).toString())
                res?.map { it.toSearchResponse() }
                    ?: throw ErrorLoadingException("Invalid JSON response")
            }
        }
        return searchResponses?.let {
            newHomePageResponse(
                HomePageList(request.name, it, false)
            )
        } ?: run {
            newHomePageResponse(
                request.name, emptyList(), false
            )
        }
    }

    override suspend fun quickSearch(query: String) = search(query)

    override suspend fun search(query: String): List<SearchResponse>? {
        val res = app.get(SEARCH_URL.format(query)).parsedSafe<ApiResponse>()
        return res?.posters?.map {
            it.toSearchResponse()
        }
    }

    private fun MovieItem.toSearchResponse(): SearchResponse {
        val title = "$title"
        val url = id.toString()
        when (val tvType = getType(type)) {
            TvType.Movie -> newMovieSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
            }

            TvType.TvSeries -> newTvSeriesSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
            }

            else -> newMovieSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
            }
        }
        return newMovieSearchResponse(
            "$title ($year)",
            id.toString(),
            getType(type)
        ) {
            this.posterUrl = image
            this.year = this@toSearchResponse.year
        }
    }

    private fun getType(type: String): TvType {
        if (type == "movie") return TvType.Movie
        if (type == "serie") return TvType.TvSeries
        return TvType.Others
    }

    companion object {
        const val ID = "4F5A9C3D9A86FA54EACEDDD635185/26a3547f-6db2-44f3-b4c8-3b8dcf1e871a/"
        const val BASE_URL = "https://netflix.veex.cc/api"
        const val MAIN_PAGE_URL = "$BASE_URL/%s/by/filtres/%d/created/0/$ID"
        const val SEARCH_URL = "$BASE_URL/search/%s/$ID"
        const val FIRST_URL = "$BASE_URL/first/$ID"
    }
}

fun main() = runBlocking {
    val veexProvider = VeexProvider()
    val ss = veexProvider.getMainPage(
        1,
        MainPageRequest(
            "Movies",
            FIRST_URL,
            false
        )
    )
//    val ss = veexProvider.search("squid")
    println("ss: $ss")
}