package com.mika.dspot.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.manager.DataStoreManager
import com.mika.dspot.ui.icon.Person
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.viewModel.SharedViewModel
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun AccountTabIcon(
    sharedViewModel: SharedViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val isYouTubeLoggedIn by sharedViewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val accountThumbUrl by sharedViewModel.getAccountThumbUrl().collectAsStateWithLifecycle("")

    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isYouTubeLoggedIn == DataStoreManager.TRUE && !accountThumbUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(accountThumbUrl)
                    .crossfade(300)
                    .build(),
                placeholder = rememberVectorPainter(SimpIcons.Person),
                error = rememberVectorPainter(SimpIcons.Person),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = SimpIcons.Person,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview
@Composable
fun AccountTabIconPreview() {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = SimpIcons.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}
