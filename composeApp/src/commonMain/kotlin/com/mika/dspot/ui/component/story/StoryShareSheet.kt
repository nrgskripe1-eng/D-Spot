package com.mika.dspot.ui.component.story

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.mika.dspot.ui.component.ShareImageSheet
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.share_to_story
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet to preview and share the D-Spot Story card for Instagram Stories,
 * Facebook My Day, or other apps with interactive link attachment.
 */
@Composable
fun StoryShareSheet(
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    seedColor: Color,
    contentUrl: String?,
    onDismiss: () -> Unit,
) {
    ShareImageSheet(
        title = stringResource(Res.string.share_to_story),
        seedColor = seedColor,
        fileNamePrefix = "dspot_story",
        contentUrl = contentUrl,
        onDismiss = onDismiss,
    ) { captureModifier ->
        StoryShareCard(
            songTitle = songTitle,
            artistName = artistName,
            artwork = artwork,
            background = seedColor,
            modifier = captureModifier,
        )
    }
}

