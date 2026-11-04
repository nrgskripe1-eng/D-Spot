package com.mika.domain.data.type

data class ChartItem(
    val name: String,
    val ytPlaylistId: String,
    val cover: String? = null,
) : PlaylistType {
    override fun playlistType(): PlaylistType.Type = PlaylistType.Type.YOUTUBE_PLAYLIST
}
