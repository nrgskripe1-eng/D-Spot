package com.mika.kotlinytmusicscraper.pages

import com.mika.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)