package com.mika.dspot.ui.component.story

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mika.dspot.ui.component.lyrics.shareCardContentColor
import com.mika.dspot.ui.component.lyrics.shareTintOn
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.mono
import org.jetbrains.compose.resources.painterResource

/**
 * Unique D-Spot Turntable Vinyl story canvas (9:16 vertical).
 * Features:
 * - Clean Track Timestamp pill at the top (e.g. 01:24 / 03:56)
 * - Realistic vinyl disc partially sliding out of album cover with smooth 33 1/3 RPM spin
 * - Grounded dark-mode styling with subtle ambient blurred artwork
 * - Single signature D-Spot emblem at the bottom
 */
@Composable
fun StoryShareCard(
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    background: Color,
    modifier: Modifier = Modifier,
    albumName: String? = null,
    currentDurationText: String = "01:24",
    totalDurationText: String = "03:56",
    isSpinning: Boolean = true,
) {
    val content = background.shareCardContentColor()
    val secondary = content.copy(alpha = 0.65f)

    // Smooth continuous vinyl rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by if (isSpinning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rotation",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(15f) }
    }

    Box(
        modifier = modifier
            .width(320.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        background,
                        lerp(background, Color.Black, 0.55f),
                        lerp(background, Color.Black, 0.90f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient background blur from the artwork
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 48.dp),
                alpha = 0.22f,
            )
        }

        // Overlay vignette for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header: Clean Track Timestamp Pill (01:24 / 03:56)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = currentDurationText,
                        color = content,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = " / $totalDurationText",
                        color = content.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Text(
                    text = "33 ⅓ RPM",
                    color = content.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                )
            }

            // Centerpiece: Turntable Vinyl sliding out of Album Sleeve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Vinyl Disc (Behind the jacket, sliding slightly to the right)
                Box(
                    modifier = Modifier
                        .size(195.dp)
                        .offset(x = 32.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = CircleShape,
                            clip = false,
                        )
                        .clip(CircleShape)
                        .rotate(rotation)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1c1c1f),
                                    Color(0xFF0a0a0c),
                                    Color(0xFF27272a),
                                    Color(0xFF09090b),
                                    Color(0xFF18181b),
                                    Color(0xFF050507),
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    // Vinyl Grooves rings
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape)
                    )

                    // Vinyl Center Label (Disc Core)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        background,
                                        lerp(background, Color.Black, 0.6f),
                                    )
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                alpha = 0.65f,
                            )
                        }
                        // Spindle Hole
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }

                // Album Jacket (In front of vinyl disc)
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .offset(x = (-16).dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(12.dp),
                            clip = false,
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                ) {
                    if (artwork != null) {
                        Image(
                            bitmap = artwork,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.mono),
                                contentDescription = null,
                                tint = content.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
            }

            // Song Info (Title & Artist)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                Text(
                    text = songTitle,
                    color = content,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = artistName,
                    color = secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!albumName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = albumName,
                        color = secondary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Footer: Single Signature D-Spot Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(content),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.mono),
                        contentDescription = null,
                        tint = background.shareTintOn(content),
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "D-Spot",
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

