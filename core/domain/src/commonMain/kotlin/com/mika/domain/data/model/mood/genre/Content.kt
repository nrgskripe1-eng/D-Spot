package com.mika.domain.data.model.mood.genre

import com.mika.domain.data.model.searchResult.songs.Thumbnail
import com.mika.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val thumbnail: List<Thumbnail>?,
    val title: Title,
) : HomeContentType