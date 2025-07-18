package com.paoneking

data class ApiResponse(
    val posters: List<MovieItem>?,
    val episodes: List<MovieItem>?,
    val id: Int?,
    val title: String?,
)

data class FirstApiResponse(
    val slides: List<Slide>,
    val genres: List<ApiResponse>
)

data class Slide(
    val id: Int,
    val title: String?,
    val type: String?,
    val image: String?,
    val poster: MovieItem
)

data class MovieItem(
    val id: Int,
    val type: String?,
    val title: String,
    val label: String?,
    val sublabel: String?,
    val description: String?,
    val year: Int?,
    val imdb: Double?,
    val comment: Boolean?,
    val rating: Int?,
    val duration: String?,
    val downloadas: String?,
    val playas: String?,
    val classification: String?,
    val image: String?,
    val cover: String?,
    val genres: List<Genre>?,
    val trailer: Trailer?,
    val sources: List<Source>?
)

data class Genre(
    val id: Int,
    val title: String
)

data class Trailer(
    val id: Int,
    val type: String,
    val url: String
)

data class Source(
    val id: Int,
    val title: String,
    val quality: String?,
    val size: String?,
    val kind: String,
    val premium: String?,
    val external: Boolean,
    val type: String,
    val url: String
)

