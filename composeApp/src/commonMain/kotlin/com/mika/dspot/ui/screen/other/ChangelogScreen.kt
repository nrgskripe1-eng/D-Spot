package com.mika.dspot.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mika.dspot.extension.barBlurStyle
import com.mika.dspot.ui.component.RippleIconButton
import com.mika.dspot.ui.icon.ArrowBackIosNew
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.theme.typo
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import dspot.composeapp.generated.resources.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ── Data model ───────────────────────────────────────────────────────────────

@Serializable
private data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<ChangeItem>,
)

@Serializable
private data class ChangeItem(
    val type: String, // "new" | "improvement" | "fix"
    val text: String,
)

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    val changelog by produceState<List<ChangelogEntry>>(initialValue = emptyList()) {
        val raw = Res.readBytes("files/changelog.json").decodeToString()
        value = Json.decodeFromString<List<ChangelogEntry>>(raw).sortedByDescending { it.version }
    }

    Box(
        modifier = Modifier.fillMaxSize().hazeSource(hazeState),
    ) {
        LazyColumn(
            state = listState,
            contentPadding =
                PaddingValues(
                    top = topAppBarHeight + paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(changelog) { index, entry ->
                ChangelogCard(entry = entry, isLatest = index == 0)
            }
        }
    }

    val barTint = MaterialTheme.colorScheme.background
    AnimatedContent(
        targetState = isAtTop,
        transitionSpec = {
            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
        },
    ) { atTop ->
        Column(
            Modifier
                .then(
                    if (atTop) {
                        Modifier
                    } else {
                        Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(barTint, 0.3f))
                    },
                ).onGloballyPositioned { coordinates ->
                    topAppBarHeight = with(density) { coordinates.size.height.toDp() }
                },
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.changelog),
                        style = typo().titleMedium,
                    )
                },
                navigationIcon = {
                    RippleIconButton(
                        imageVector = SimpIcons.ArrowBackIosNew,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        }
    }
}

// ── Changelog card ───────────────────────────────────────────────────────────

@Composable
private fun ChangelogCard(
    entry: ChangelogEntry,
    isLatest: Boolean,
) {
    var expanded by rememberSaveable(entry.version) { mutableStateOf(isLatest) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else -90f,
        animationSpec = tween(250),
        label = "chevron_rotation",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row — tap to expand/collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // Version badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color =
                        if (isLatest) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                ) {
                    Text(
                        text = "v${entry.version}",
                        style = typo().labelMedium.copy(fontWeight = FontWeight.Bold),
                        color =
                            if (isLatest) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Date
                Text(
                    text = entry.date,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                // Latest pill
                if (isLatest) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "Latest",
                            style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Chevron — ArrowBackIosNew rotated to act as expand/collapse indicator
                Icon(
                    imageVector = SimpIcons.ArrowBackIosNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).rotate(rotation),
                )
            }

            // Collapsible change list
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entry.changes.forEach { change ->
                        ChangeRow(change = change)
                    }
                }
            }
        }
    }
}

// ── Single change row ────────────────────────────────────────────────────────

@Composable
private fun ChangeRow(change: ChangeItem) {
    val (chipColor, chipText, chipTextColor) =
        when (change.type) {
            "new" ->
                Triple(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    "New",
                    MaterialTheme.colorScheme.tertiary,
                )
            "improvement" ->
                Triple(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    "Improved",
                    MaterialTheme.colorScheme.primary,
                )
            else ->
                Triple(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    "Fix",
                    MaterialTheme.colorScheme.error,
                )
        }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = chipColor,
        ) {
            Text(
                text = chipText,
                style = typo().labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                color = chipTextColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = change.text,
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
