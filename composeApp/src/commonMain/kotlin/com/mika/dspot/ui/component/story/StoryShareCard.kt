package com.mika.dspot.ui.component.story

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mika.dspot.ui.component.lyrics.shareCardContentColor
import com.mika.dspot.ui.component.lyrics.shareTintOn
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.icon.GraphicEq
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.mono
import org.jetbrains.compose.resources.painterResource

/**
 * Clean, modern 9:16 vertical canvas designed specifically for Instagram Stories & Facebook My Day.
 * Grounded & elegant, avoiding artificial glowing/neon gimmicks.
 */
@Composable
fun StoryShareCard(
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val content = background.shareCardContentColor()
    val secondary = content.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .width(320.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        background,
                        lerp(background, Color.Black, 0.45f),
                        lerp(background, Color.Black, 0.85f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle ambient blurred artwork in the background
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 45.dp),
                alpha = 0.28f,
            )
        }

        // Overlay vignette for depth and contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header: Clean indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = SimpIcons.GraphicEq,
                    contentDescription = null,
                    tint = content.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NOW PLAYING",
                    color = content.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }

            // Centerpiece: Realistic artwork with subtle drop shadow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (artwork != null) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false,
                            )
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = artwork,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray.copy(alpha = 0.4f)),
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

                Spacer(modifier = Modifier.height(26.dp))

                // Song Title & Artist
                Text(
                    text = songTitle,
                    color = content,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = artistName,
                    color = secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Footer: Signature D-Spot badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(content.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(content),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.mono),
                        contentDescription = null,
                        tint = background.shareTintOn(content),
                        modifier = Modifier.size(11.dp),
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "D-Spot",
                    color = content,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

