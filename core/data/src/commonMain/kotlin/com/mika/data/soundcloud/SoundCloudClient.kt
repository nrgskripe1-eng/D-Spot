package com.mika.data.soundcloud

import com.mika.domain.data.model.browse.album.Track
import com.mika.domain.data.model.browse.artist.ArtistBrowse
import com.mika.domain.data.model.browse.artist.ResultSong
import com.mika.domain.data.model.browse.artist.Songs
import com.mika.domain.data.model.browse.playlist.Author
import com.mika.domain.data.model.browse.playlist.PlaylistBrowse
import com.mika.domain.data.model.home.Content
import com.mika.domain.data.model.home.HomeItem
import com.mika.domain.data.model.searchResult.artists.ArtistsResult
import com.mika.domain.data.model.searchResult.playlists.PlaylistsResult
import com.mika.domain.data.model.searchResult.songs.Album
import com.mika.domain.data.model.searchResult.songs.Artist
import com.mika.domain.data.model.searchResult.songs.SongsResult
import com.mika.domain.data.model.searchResult.songs.Thumbnail
import com.mika.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val SC_ID_PREFIX = "sc_"

@Serializable
internal data class SoundCloudSearchTracksResponse(
    val collection: List<SoundCloudTrackDto> = emptyList(),
)

@Serializable
internal data class SoundCloudSearchUsersResponse(
    val collection: List<SoundCloudUserDto> = emptyList(),
)

@Serializable
internal data class SoundCloudSearchPlaylistsResponse(
    val collection: List<SoundCloudPlaylistDto> = emptyList(),
)

@Serializable
internal data class SoundCloudTrackDto(
    val id: Long,
    val title: String = "",
    val duration: Long = 0L,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val user: SoundCloudUserDto? = null,
    val media: SoundCloudMediaDto? = null,
    @SerialName("permalink_url") val permalinkUrl: String? = null,
)

@Serializable
internal data class SoundCloudUserDto(
    val id: Long,
    val username: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("permalink_url") val permalinkUrl: String? = null,
    val description: String? = null,
    @SerialName("followers_count") val followersCount: Long = 0L,
)

@Serializable
internal data class SoundCloudPlaylistDto(
    val id: Long,
    val title: String = "",
    @SerialName("track_count") val trackCount: Int = 0,
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val user: SoundCloudUserDto? = null,
    val description: String? = null,
    val duration: Long = 0L,
    val tracks: List<SoundCloudTrackDto> = emptyList(),
)

@Serializable
internal data class SoundCloudMediaDto(
    val transcodings: List<SoundCloudTranscodingDto> = emptyList(),
)

@Serializable
internal data class SoundCloudTranscodingDto(
    val url: String = "",
    val format: SoundCloudFormatDto? = null,
)

@Serializable
internal data class SoundCloudFormatDto(
    val protocol: String = "",
    @SerialName("mime_type") val mimeType: String = "",
)

@Serializable
internal data class SoundCloudStreamResponse(
    val url: String? = null,
)

class SoundCloudClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val clientIdMutex = Mutex()
    private var cachedClientId: String? = null

    private suspend fun getClientId(forceRefresh: Boolean = false): String? =
        clientIdMutex.withLock {
            if (!forceRefresh && cachedClientId != null) {
                return@withLock cachedClientId
            }
            val fetched = fetchClientIdFromWeb()
            if (fetched != null) {
                cachedClientId = fetched
            }
            cachedClientId
        }

    private suspend fun fetchClientIdFromWeb(): String? =
        runCatching {
            val html = client.get("https://soundcloud.com").bodyAsText()
            val scriptRegex = Regex("""https://a-v2\.sndcdn\.com/assets/[^"']+\.js""")
            val scriptUrls = scriptRegex.findAll(html).map { it.value }.toList().reversed()
            val clientIdRegex = Regex("""client_id\s*:\s*["']([a-zA-Z0-9]{32})["']""")
            for (scriptUrl in scriptUrls.take(6)) {
                val js = runCatching { client.get(scriptUrl).bodyAsText() }.getOrNull() ?: continue
                val match = clientIdRegex.find(js)
                if (match != null) {
                    val id = match.groupValues[1]
                    Logger.d("SoundCloud", "Fetched fresh client_id")
                    return@runCatching id
                }
            }
            null
        }.onFailure {
            Logger.e("SoundCloud", "Failed to fetch client_id: ${it.message}")
        }.getOrNull()

    private suspend inline fun <reified T> getWithClientId(
        url: String,
        crossinline params: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T? {
        var cid = getClientId() ?: return null
        val firstResp = runCatching {
            client.get(url) {
                parameter("client_id", cid)
                params()
            }
        }.getOrNull() ?: return null

        if (firstResp.status == HttpStatusCode.Unauthorized || firstResp.status == HttpStatusCode.Forbidden) {
            cid = getClientId(forceRefresh = true) ?: return null
            val retryResp = runCatching {
                client.get(url) {
                    parameter("client_id", cid)
                    params()
                }
            }.getOrNull() ?: return null
            return if (retryResp.status == HttpStatusCode.OK) retryResp.body<T>() else null
        }

        return if (firstResp.status == HttpStatusCode.OK) firstResp.body<T>() else null
    }

    suspend fun searchTracks(query: String, limit: Int = 15): List<SongsResult> {
        val response = getWithClientId<SoundCloudSearchTracksResponse>(
            "https://api-v2.soundcloud.com/search/tracks",
        ) {
            parameter("q", query)
            parameter("limit", limit)
        } ?: return emptyList()

        return response.collection.mapNotNull { track ->
            if (track.title.isBlank()) return@mapNotNull null
            val durationSec = (track.duration / 1000L).toInt()
            val mins = durationSec / 60
            val secs = durationSec % 60
            val durationStr = "$mins:${secs.toString().padStart(2, '0')}"
            val thumbUrl = (track.artworkUrl ?: track.user?.avatarUrl)?.replace("-large", "-t500x500")

            SongsResult(
                album = null,
                artists = track.user?.let {
                    listOf(Artist(id = "$SC_ID_PREFIX${it.id}", name = it.username))
                },
                category = "Songs",
                duration = durationStr,
                durationSeconds = durationSec,
                feedbackTokens = null,
                isExplicit = false,
                resultType = "song",
                thumbnails = thumbUrl?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) },
                title = track.title,
                videoId = "$SC_ID_PREFIX${track.id}",
                videoType = "SONG",
                year = "",
            )
        }
    }

    suspend fun searchArtists(query: String, limit: Int = 8): List<ArtistsResult> {
        val response = getWithClientId<SoundCloudSearchUsersResponse>(
            "https://api-v2.soundcloud.com/search/users",
        ) {
            parameter("q", query)
            parameter("limit", limit)
        } ?: return emptyList()

        return response.collection.mapNotNull { user ->
            if (user.username.isBlank()) return@mapNotNull null
            val avatar = user.avatarUrl?.replace("-large", "-t500x500")
            ArtistsResult(
                artist = user.username,
                browseId = "$SC_ID_PREFIX${user.id}",
                category = "Artists",
                radioId = "",
                resultType = "artist",
                shuffleId = "",
                thumbnails = avatar?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList(),
            )
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 8): List<PlaylistsResult> {
        val response = getWithClientId<SoundCloudSearchPlaylistsResponse>(
            "https://api-v2.soundcloud.com/search/playlists",
        ) {
            parameter("q", query)
            parameter("limit", limit)
        } ?: return emptyList()

        return response.collection.mapNotNull { pl ->
            if (pl.title.isBlank()) return@mapNotNull null
            val thumb = (pl.artworkUrl ?: pl.user?.avatarUrl)?.replace("-large", "-t500x500")
            PlaylistsResult(
                author = pl.user?.username ?: "",
                browseId = "$SC_ID_PREFIX${pl.id}",
                category = "Playlists",
                itemCount = "${pl.trackCount} songs",
                resultType = "playlist",
                thumbnails = thumb?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList(),
                title = pl.title,
            )
        }
    }

    suspend fun getStreamUrl(prefixedTrackId: String): String? {
        val rawId = prefixedTrackId.removePrefix(SC_ID_PREFIX)
        val track = getWithClientId<SoundCloudTrackDto>(
            "https://api-v2.soundcloud.com/tracks/$rawId",
        ) ?: return null

        val transcodings = track.media?.transcodings ?: return null
        val chosen =
            transcodings.firstOrNull { it.format?.protocol == "progressive" }
                ?: transcodings.firstOrNull { it.format?.protocol == "hls" }
                ?: transcodings.firstOrNull()
                ?: return null

        val streamResp = getWithClientId<SoundCloudStreamResponse>(chosen.url)
        return streamResp?.url
    }

    suspend fun getArtistBrowse(prefixedUserId: String): ArtistBrowse? {
        val rawId = prefixedUserId.removePrefix(SC_ID_PREFIX)
        val user = getWithClientId<SoundCloudUserDto>(
            "https://api-v2.soundcloud.com/users/$rawId",
        ) ?: return null

        val tracksResp = getWithClientId<SoundCloudSearchTracksResponse>(
            "https://api-v2.soundcloud.com/users/$rawId/tracks",
        ) {
            parameter("limit", 30)
        }
        val tracks = tracksResp?.collection.orEmpty()
        val avatar = user.avatarUrl?.replace("-large", "-t500x500")
        val userThumbs = avatar?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList()

        val resultSongs = tracks.mapNotNull { track ->
            if (track.title.isBlank()) return@mapNotNull null
            val durationSec = (track.duration / 1000L).toInt()
            val thumbUrl = (track.artworkUrl ?: user.avatarUrl)?.replace("-large", "-t500x500")
            val thumbs = thumbUrl?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList()
            ResultSong(
                videoId = "$SC_ID_PREFIX${track.id}",
                title = track.title,
                artists = listOf(Artist(id = prefixedUserId, name = track.user?.username ?: user.username)),
                durationSeconds = durationSec,
                album = Album(id = "", name = ""),
                likeStatus = "INDIFFERENT",
                thumbnails = thumbs,
                isAvailable = true,
                isExplicit = false,
                videoType = "SONG",
            )
        }

        return ArtistBrowse(
            albums = null,
            channelId = prefixedUserId,
            description = user.description,
            name = user.username,
            radioId = null,
            related = null,
            shuffleId = null,
            singles = null,
            songs = Songs(browseId = null, results = resultSongs),
            video = null,
            featuredOn = null,
            videoList = null,
            subscribed = false,
            subscribers = if (user.followersCount > 0) "${user.followersCount} followers" else null,
            thumbnails = userThumbs,
            views = null,
        )
    }

    suspend fun getPlaylistBrowse(prefixedPlaylistId: String): PlaylistBrowse? {
        if (prefixedPlaylistId == "sc_home_unreleased" || prefixedPlaylistId == "sc_home_remixes") {
            val isUnreleased = prefixedPlaylistId == "sc_home_unreleased"
            val title = if (isUnreleased) "Unreleased, Demos & Freestyles" else "Trending Remixes & Club Edits"
            val query = if (isUnreleased) "unreleased freestyle demo" else "slowed remix edit"
            val songs = searchTracks(query, limit = 25)
            val tracks = songs.map { song ->
                Track(
                    album = song.album,
                    artists = song.artists,
                    duration = song.duration ?: "0:00",
                    durationSeconds = song.durationSeconds ?: 0,
                    isAvailable = true,
                    isExplicit = false,
                    likeStatus = "INDIFFERENT",
                    thumbnails = song.thumbnails ?: emptyList(),
                    title = song.title ?: "",
                    videoId = song.videoId,
                    videoType = "SONG",
                    category = null,
                    feedbackTokens = null,
                    resultType = null,
                    year = null,
                )
            }
            val totalSec: Int = tracks.sumOf { it.durationSeconds ?: 0 }
            return PlaylistBrowse(
                author = Author(id = "", name = "D-Spot"),
                description = null,
                duration = "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}",
                durationSeconds = totalSec,
                id = prefixedPlaylistId,
                privacy = "PUBLIC",
                thumbnails = tracks.firstOrNull()?.thumbnails ?: emptyList(),
                title = title,
                trackCount = tracks.size,
                tracks = tracks,
                year = "",
            )
        }
        val rawId = prefixedPlaylistId.removePrefix(SC_ID_PREFIX)
        val pl = getWithClientId<SoundCloudPlaylistDto>(
            "https://api-v2.soundcloud.com/playlists/$rawId",
        ) ?: return null

        val plThumb = (pl.artworkUrl ?: pl.user?.avatarUrl)?.replace("-large", "-t500x500")
        val plThumbs = plThumb?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList()
        val totalSec = (pl.duration / 1000L).toInt()
        val mins = totalSec / 60
        val secs = totalSec % 60

        val tracks = pl.tracks.mapNotNull { track ->
            if (track.title.isBlank()) return@mapNotNull null
            val durationSec = (track.duration / 1000L).toInt()
            val thumbUrl = (track.artworkUrl ?: track.user?.avatarUrl ?: plThumb)?.replace("-large", "-t500x500")
            val thumbs = thumbUrl?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList()
            Track(
                album = null,
                artists = track.user?.let { listOf(Artist(id = "$SC_ID_PREFIX${it.id}", name = it.username)) },
                duration = "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}",
                durationSeconds = durationSec,
                isAvailable = true,
                isExplicit = false,
                likeStatus = "INDIFFERENT",
                thumbnails = thumbs,
                title = track.title,
                videoId = "$SC_ID_PREFIX${track.id}",
                videoType = "SONG",
                category = null,
                feedbackTokens = null,
                resultType = null,
                year = null,
            )
        }

        return PlaylistBrowse(
            author = Author(
                id = pl.user?.let { "$SC_ID_PREFIX${it.id}" } ?: "",
                name = pl.user?.username ?: "",
            ),
            description = pl.description,
            duration = "$mins:${secs.toString().padStart(2, '0')}",
            durationSeconds = totalSec,
            id = prefixedPlaylistId,
            privacy = "PUBLIC",
            thumbnails = plThumbs,
            title = pl.title,
            trackCount = tracks.size,
            tracks = tracks,
            year = "",
        )
    }

    suspend fun getHomeSections(): List<HomeItem> {
        val sections = mutableListOf<HomeItem>()
        val queries = listOf(
            "Unreleased, Demos & Freestyles" to "unreleased freestyle demo",
            "Trending Remixes & Club Edits" to "slowed remix edit",
        )
        for ((sectionTitle, query) in queries) {
            val tracks = runCatching { searchTracks(query, limit = 12) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                val contents = tracks.map { song ->
                    Content(
                        album = song.album,
                        artists = song.artists,
                        description = null,
                        isExplicit = song.isExplicit,
                        playlistId = null,
                        browseId = null,
                        thumbnails = song.thumbnails ?: emptyList(),
                        title = song.title ?: "",
                        videoId = song.videoId,
                        views = null,
                        durationSeconds = song.durationSeconds,
                        videoType = "SONG",
                    )
                }
                sections.add(
                    HomeItem(
                        contents = contents,
                        title = sectionTitle,
                    ),
                )
            }
        }
        return sections
    }

    suspend fun getRelatedTracks(prefixedTrackId: String, limit: Int = 15): List<Track> {
        val rawId = prefixedTrackId.removePrefix(SC_ID_PREFIX)
        val response = getWithClientId<SoundCloudSearchTracksResponse>(
            "https://api-v2.soundcloud.com/tracks/$rawId/related",
        ) {
            parameter("limit", limit)
        } ?: return emptyList()

        return response.collection.mapNotNull { track ->
            if (track.title.isBlank()) return@mapNotNull null
            val durationSec = (track.duration / 1000L).toInt()
            val thumbUrl = (track.artworkUrl ?: track.user?.avatarUrl)?.replace("-large", "-t500x500")
            val thumbs = thumbUrl?.let { listOf(Thumbnail(height = 500, url = it, width = 500)) } ?: emptyList()
            Track(
                album = null,
                artists = track.user?.let { listOf(Artist(id = "$SC_ID_PREFIX${it.id}", name = it.username)) },
                duration = "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}",
                durationSeconds = durationSec,
                isAvailable = true,
                isExplicit = false,
                likeStatus = "INDIFFERENT",
                thumbnails = thumbs,
                title = track.title,
                videoId = "$SC_ID_PREFIX${track.id}",
                videoType = "SONG",
                category = null,
                feedbackTokens = null,
                resultType = null,
                year = null,
            )
        }
    }

    sealed class ResolveResult {
        data class Track(val videoId: String) : ResolveResult()
        data class Playlist(val playlistId: String) : ResolveResult()
        data class Artist(val channelId: String) : ResolveResult()
    }

    suspend fun resolveUrl(url: String): ResolveResult? {
        val cid = getClientId() ?: return null
        val resp = runCatching {
            client.get("https://api-v2.soundcloud.com/resolve") {
                parameter("url", url)
                parameter("client_id", cid)
            }
        }.getOrNull() ?: return null

        if (resp.status != HttpStatusCode.OK) return null
        val bodyText = runCatching { resp.bodyAsText() }.getOrNull() ?: return null
        val jsonObj = runCatching { json.parseToJsonElement(bodyText) as? kotlinx.serialization.json.JsonObject }.getOrNull() ?: return null
        val kind = (jsonObj["kind"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return null
        val id = (jsonObj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() ?: return null

        return when (kind) {
            "track" -> ResolveResult.Track("$SC_ID_PREFIX$id")
            "playlist" -> ResolveResult.Playlist("$SC_ID_PREFIX$id")
            "user" -> ResolveResult.Artist("$SC_ID_PREFIX$id")
            else -> null
        }
    }
}

