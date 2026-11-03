package com.mika.domain.data.model.browse.artist

import com.mika.domain.data.model.searchResult.songs.Thumbnail
import com.mika.domain.data.type.HomeContentType

data class ResultSingle(
    val browseId: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val year: String,
) : HomeContentType