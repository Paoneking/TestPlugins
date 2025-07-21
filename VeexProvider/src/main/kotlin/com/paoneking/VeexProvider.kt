package com.paoneking

import com.lagradost.api.Log
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.runAllAsync
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

abstract class VeexProvider(domain: String) :
    MainAPI() { // all providers must be an instance of MainAPI
    abstract override var name: String
    override val hasMainPage = true
    override var lang = "ne"
    override val instantLinkLoading = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    private val baseUrl = "https://$domain.veex.cc/api"
    private val urlId = "4F5A9C3D9A86FA54EACEDDD635185/26a3547f-6db2-44f3-b4c8-3b8dcf1e871a/"
    private val mainPageUrl = "$baseUrl/%s/by/filtres/%d/created/0/$urlId"
    private val searchUrl = "$baseUrl/search/%s/$urlId"
    private val firstUrl = "$baseUrl/first/$urlId"
    private val seriesUrl = "$baseUrl/season/by/serie/%s/$urlId"

    override val mainPage = mainPageOf(
        firstUrl to "Home",
        mainPageUrl.format("movie", 0) to "Movies",
        mainPageUrl.format("serie", 0) to "Series",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        println("request: $request")
        when (request.data) {
            firstUrl -> {
                val res = app.get(request.data).parsed<FirstApiResponse>()
                val homePageLists = res.genres.map {
                    val searchResponse = it.posters?.map { movieItem ->
                        movieItem.toSearchResponse()
                    }
                    HomePageList(it.title ?: "", searchResponse!!)
                }
                val homePageResponse = newHomePageResponse(homePageLists, false)
                println("homePageResponse: $homePageResponse")
                return homePageResponse
            }

            else -> {
                val res = parseJson<List<MovieItem>>(app.get(request.data).toString())
                val searchResponses = res.map { it.toSearchResponse() }
                val homePageResponse = newHomePageResponse(
                    HomePageList(request.name, searchResponses, false), hasNext = false
                )
                println("homePageResponse: $homePageResponse")
                return homePageResponse
            }
        }
    }

    override suspend fun quickSearch(query: String) = search(query)

    override suspend fun search(query: String): List<SearchResponse>? {
        val res = app.get(searchUrl.format(query)).parsedSafe<ApiResponse>()
        return res?.posters?.map {
            it.toSearchResponse()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val movieItem: MovieItem = tryParseJson<MovieItem>(url)!!
        return when (val type = getType(movieItem.type)) {
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
                val res = tryParseJson<List<ApiResponse>>(
                    app.get(seriesUrl.format(movieItem.id)).toString()
                )
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
                newTvSeriesLoadResponse(
                    movieItem.title,
                    url,
                    TvType.TvSeries,
                    episodes?.flatten() ?: emptyList()
                ) {
                    this.posterUrl = movieItem.cover
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

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("veex", "loadLinks data: $data")
        val movieItem: MovieItem = tryParseJson<MovieItem>(data)!!
        val sources = movieItem.sources
        val source = sources?.first()
        if (source != null) {
            callback.invoke(newExtractorLink("HD", "HD", source.url) {
                this.quality = Qualities.P1080.value
                this.type = ExtractorLinkType.M3U8
            })
            runAllAsync(
                {
                    invokeSubtitles(source.url, subtitleCallback)
                }
            )
            return true
        }
        return false
    }

    suspend fun invokeSubtitles(
        url: String,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val baseUrl = url.substringBeforeLast("/")
        val mediaInfoUrl = baseUrl.plus("/mediainfo.json")
        val response = app.get(mediaInfoUrl).toString()
        val subtitles: List<MediaTrack> = parseJson<List<MediaTrack>>(response).filter {
            it.type.lowercase() == "text" && it.Language.isNotEmpty()
        }
        subtitles.forEach {
            subtitleCallback(
                SubtitleFile(
                    it.Language,
                    "$baseUrl/sub-titles/${it.Language}-${it.ID}.srt"
                )
            )
        }
    }

    private fun MovieItem.toSearchResponse(): SearchResponse {
        val title = "$title ($year)"
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
}