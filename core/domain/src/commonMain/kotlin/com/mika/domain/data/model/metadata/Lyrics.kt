package com.mika.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val error: Boolean = false,
    val lines: List<Line>?,
    val syncType: String?,
    val simpMusicLyrics: DSpotLyrics? = null,
)

@Serializable
data class DSpotLyrics(
    val id: String,
    val vote: Int,
)