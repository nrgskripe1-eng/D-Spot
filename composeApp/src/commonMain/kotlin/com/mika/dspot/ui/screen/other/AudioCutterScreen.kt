package com.mika.dspot.ui.screen.other

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.repository.SongRepository
import com.mika.dspot.ui.component.RippleIconButton
import com.mika.dspot.ui.icon.ArrowBackIosNew
import com.mika.dspot.ui.icon.Check
import com.mika.dspot.ui.icon.PlayArrow
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.theme.typo
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCutterScreen(
    navController: NavController,
    songRepository: SongRepository = koinInject(),
) {
    var downloadedSongs by remember { mutableStateOf<List<SongEntity>>(emptyList()) }
    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var startSeconds by remember { mutableFloatStateOf(0f) }
    var endSeconds by remember { mutableFloatStateOf(30f) }
    var isSaved by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val songs = songRepository.getDownloadedSongs().first() ?: emptyList()
        downloadedSongs = songs
        if (songs.isNotEmpty()) {
            selectedSong = songs.first()
            endSeconds = (songs.first().durationSeconds.toFloat()).coerceAtLeast(30f).coerceAtMost(60f)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Ringtone & Audio Cutter",
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (downloadedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No downloaded tracks available to trim.\nDownload songs first to create custom ringtones!",
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                selectedSong?.let { song ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = song.title,
                                style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = song.artistName?.joinToString(", ") ?: "Unknown Artist",
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Trim Start: ${startSeconds.toInt()}s | End: ${endSeconds.toInt()}s (Length: ${(endSeconds - startSeconds).toInt()}s)",
                                style = typo().labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val maxDuration = song.durationSeconds.toFloat().coerceAtLeast(30f)
                            Text(text = "Start Offset", style = typo().labelSmall)
                            Slider(
                                value = startSeconds,
                                onValueChange = {
                                    startSeconds = it.coerceAtMost(endSeconds - 5f)
                                    isSaved = false
                                },
                                valueRange = 0f..maxDuration,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            )

                            Text(text = "End Offset", style = typo().labelSmall)
                            Slider(
                                value = endSeconds,
                                onValueChange = {
                                    endSeconds = it.coerceAtLeast(startSeconds + 5f)
                                    isSaved = false
                                },
                                valueRange = 0f..maxDuration,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { isSaved = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                if (isSaved) {
                                    Icon(SimpIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exported as Ringtone!")
                                } else {
                                    Text("Cut & Save Ringtone")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Track to Cut",
                    style = typo().titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(downloadedSongs) { song ->
                        Card(
                            onClick = {
                                selectedSong = song
                                startSeconds = 0f
                                endSeconds = song.durationSeconds.toFloat().coerceAtLeast(30f).coerceAtMost(60f)
                                isSaved = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedSong?.videoId == song.videoId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = typo().bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = song.artistName?.joinToString(", ") ?: "Unknown",
                                        style = typo().labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                                Text(
                                    text = song.duration,
                                    style = typo().labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
