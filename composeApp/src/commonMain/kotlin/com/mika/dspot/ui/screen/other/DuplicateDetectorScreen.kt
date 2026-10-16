package com.mika.dspot.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mika.domain.repository.LocalPlaylistRepository
import com.mika.domain.repository.SongRepository
import com.mika.dspot.ui.component.RippleIconButton
import com.mika.dspot.ui.icon.ArrowBackIosNew
import com.mika.dspot.ui.icon.Check
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.theme.typo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class DuplicateGroup(
    val title: String,
    val artist: String,
    val playlistName: String,
    val playlistId: Long,
    val duplicateCount: Int,
    val videoId: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateDetectorScreen(
    navController: NavController,
    localPlaylistRepository: LocalPlaylistRepository = koinInject(),
    songRepository: SongRepository = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(true) }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var cleanedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val playlists = localPlaylistRepository.getAllLocalPlaylists().first()
        val foundDuplicates = mutableListOf<DuplicateGroup>()

        for (playlist in playlists) {
            val tracks = playlist.tracks ?: emptyList()
            val counts = tracks.groupingBy { it }.eachCount()
            for ((vid, count) in counts) {
                if (count > 1) {
                    val song = songRepository.getSongById(vid).first()
                    foundDuplicates.add(
                        DuplicateGroup(
                            title = song?.title ?: vid,
                            artist = song?.artistName?.joinToString(", ") ?: "Unknown Artist",
                            playlistName = playlist.title,
                            playlistId = playlist.id,
                            duplicateCount = count,
                            videoId = vid,
                        )
                    )
                }
            }
        }
        duplicateGroups = foundDuplicates
        isScanning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Duplicate Detector",
                    style = typo().titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            navigationIcon = {
                RippleIconButton(
                    imageVector = SimpIcons.ArrowBackIosNew,
                    tint = MaterialTheme.colorScheme.onBackground,
                    onClick = { navController.navigateUp() },
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )

        if (isScanning) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning your playlists for duplicates...",
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (duplicateGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SimpIcons.Check,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Clean Playlist Library!",
                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No duplicate songs found in any of your playlists.",
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Found ${duplicateGroups.size} duplicated song instances",
                        style = typo().labelLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                items(duplicateGroups) { group ->
                    val groupKey = "${group.playlistId}_${group.videoId}"
                    val isCleaned = cleanedGroups.contains(groupKey)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.title,
                                    style = typo().titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${group.artist} • In: ${group.playlistName}",
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${group.duplicateCount} copies found",
                                    style = typo().labelSmall.copy(color = Color(0xFFEF4444)),
                                )
                            }

                            if (isCleaned) {
                                Text(
                                    text = "Cleaned",
                                    style = typo().labelMedium.copy(color = Color(0xFF22C55E)),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            } else {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val resource = localPlaylistRepository.getLocalPlaylist(group.playlistId).first()
                                            val currentTracks: List<String> = resource.data?.tracks ?: emptyList()
                                            // Deduplicate: keep only 1 instance of videoId
                                            var kept = false
                                            val newTracks = currentTracks.filter { vid: String ->
                                                if (vid == group.videoId) {
                                                    if (!kept) {
                                                        kept = true
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                } else {
                                                    true
                                                }
                                            }
                                            localPlaylistRepository.updateLocalPlaylistTracks(newTracks, group.playlistId)
                                            cleanedGroups = cleanedGroups + groupKey
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text("Remove Extra", style = typo().labelSmall.copy(color = Color.White))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
