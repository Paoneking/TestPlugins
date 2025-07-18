package com.paoneking

import android.net.Uri
import androidx.navigation.R.attr.data
import androidx.navigation.common.R.attr.uri
import com.lagradost.api.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.R.string.episode
import com.lagradost.cloudstream3.R.string.season
import com.lagradost.cloudstream3.R.string.trailer
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
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
                val searchReasponse = res?.slides?.map { it.poster.toSearchResponse() }
                println("searchReasponse: $searchReasponse")
                searchReasponse
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
                HomePageList(request.name, it, false), hasNext = false
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

    override suspend fun load(url: String): LoadResponse? {
        val movieItem: MovieItem = tryParseJson<MovieItem>(url)!!
        return when(val type = getType(movieItem.type)) {
            TvType.Movie -> {
                newMovieLoadResponse(movieItem.title, url, type, movieItem) {
                    this.posterUrl = movieItem.image
                    this.year = movieItem.year
                    this.plot = movieItem.description
                    this.tags = movieItem.genres?.map { it.title }
                    this.rating = movieItem.rating
                    addTrailer(movieItem.trailer?.url)
                }
            }
            TvType.TvSeries -> {
                val res = tryParseJson<List<ApiResponse>>(app.get(SERIES_URL.format(movieItem.id)).toString())
                val episodes = res?.mapIndexed { index, season ->
                    val seasonNum = index + 1
                    season.episodes!!.mapIndexed { index1, episode ->
                        val episodeNum = index1 + 1
                        newEpisode(episode.toJson()) {
                            this.name = episode.title
                            this.episode = episodeNum
                            this.season = seasonNum
                            this.posterUrl = episode.image
                        }
                    }
                }
                newTvSeriesLoadResponse(movieItem.title, url, TvType.TvSeries, episodes?.flatten() ?: emptyList()) {
                    this.posterUrl = movieItem.image
                    this.year = movieItem.year
                    this.plot = movieItem.description
                    this.tags = movieItem.genres?.map { it.title }
                    this.rating = movieItem.rating
                    addTrailer(movieItem.trailer?.url)
                }
            }
            else -> {
                null
            }
        }
    }

    private fun MovieItem.toSearchResponse(): SearchResponse {
        val title = title
//        val url = "$BASE_URL/$id?title=$title"
        val url = this.toJson()
        return when (val tvType = getType(type)) {
            TvType.Movie -> newMovieSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
                this.id = this@toSearchResponse.id
            }

            TvType.TvSeries -> newTvSeriesSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
                this.id = this@toSearchResponse.id
            }

            else -> newMovieSearchResponse(
                title,
                url,
                tvType
            ) {
                this.posterUrl = image
                this.year = this@toSearchResponse.year
                this.id = this@toSearchResponse.id
            }
        }
    }

    private fun getType(type: String?): TvType {
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
        const val SERIES_URL = "$BASE_URL/season/by/serie/%s/$ID"
    }
}

fun main() = runBlocking {
    val veexProvider = VeexProvider()
     /*val ss = veexProvider.getMainPage(
         1,
         MainPageRequest(
             "Movies",
             FIRST_URL,
             false
         )
     )*/
    val json = """
        {"id":4265,"type":"serie","title":"Squid Game","label":null,"sublabel":null,"description":"Hundreds of cash-strapped players accept a strange invitation to compete in children's games. Inside, a tempting prize awaits — with deadly high stakes.","year":2021,"imdb":7.862,"comment":true,"rating":0,"duration":"3 Seasons","downloadas":"1","playas":"1","classification":null,"image":"https://netflix.veex.cc/uploads/cache/poster_thumb/uploads/jpg/960c8422ffd42361cfdf07f427b4ab12.jpg","cover":"https://netflix.veex.cc/uploads/cache/cover_thumb/uploads/jpg/cb061fb71e28cf56fc0d6a73cfcc62c4.jpg","genres":[{"id":2,"title":"Drama"},{"id":13,"title":"Mystery"},{"id":20,"title":"Action & Adventure"},{"id":28,"title":"New on Netflix"}],"trailer":{"id":14768,"type":"youtube","url":"https://www.youtube.com/watch?v=oqxAJKy0ii4"},"sources":[]}
    """.trimIndent()
    val ss = veexProvider.load(json)
    println("ss: $ss")
}