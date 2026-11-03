package com.mika.dspot.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.mika.dspot.extension.DesktopWindowChrome
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.Remove
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.icon.UnfoldLess
import com.mika.dspot.ui.icon.UnfoldMore
import com.mika.dspot.ui.theme.typo
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.circle_app_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.MouseInfo
import java.awt.Window

/**
 * Custom modern title bar for D-Spot desktop.
 * Replaces the generic Windows title bar with a clean D-Spot chrome.
 * Supports window dragging, double-tap to maximize, and minimal window controls.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    title: String,
    windowState: WindowState,
    window: Window,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black,
    titleColor: Color = Color.White,
) {
    var isMaximized by remember { mutableStateOf(windowState.placement == WindowPlacement.Maximized) }
    var dragStartX by remember { mutableStateOf(0) }
    var dragStartY by remember { mutableStateOf(0) }

    val isMacOS = remember {
        System.getProperty("os.name", "").contains("Mac", ignoreCase = true)
    }

    LaunchedEffect(windowState.placement) {
        isMaximized = windowState.placement == WindowPlacement.Maximized
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopWindowChrome.TITLE_BAR_HEIGHT_DP.dp)
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                            } else {
                                windowState.placement = WindowPlacement.Maximized
                            }
                        },
                    )
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            dragStartX = mouseLocation.x - window.x
                            dragStartY = mouseLocation.y - window.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                                dragStartX = (windowState.size.width.value / 2).toInt()
                                dragStartY = 20
                            }
                            window.setLocation(
                                mouseLocation.x - dragStartX,
                                mouseLocation.y - dragStartY,
                            )
                        },
                    )
                },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // If macOS, show controls on left
            if (isMacOS) {
                Row(
                    modifier = Modifier.padding(start = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MacWindowControlButton(
                        onClick = onCloseRequest,
                        backgroundColor = Color(0xFFFF605C),
                        hoverColor = Color(0xFFE54942),
                        icon = WindowControlIcon.Close,
                    )
                    MacWindowControlButton(
                        onClick = { windowState.isMinimized = true },
                        backgroundColor = Color(0xFFFFBD44),
                        hoverColor = Color(0xFFE5A93D),
                        icon = WindowControlIcon.Minimize,
                    )
                    MacWindowControlButton(
                        onClick = {
                            windowState.placement =
                                if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                        },
                        backgroundColor = Color(0xFF00CA4E),
                        hoverColor = Color(0xFF00B344),
                        icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            // Windows / Linux: Modern right-aligned controls
            if (!isMacOS) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DSpotWindowsControlButton(
                        icon = WindowControlIcon.Minimize,
                        onClick = { windowState.isMinimized = true },
                        contentColor = titleColor,
                    )
                    DSpotWindowsControlButton(
                        icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
                        onClick = {
                            windowState.placement =
                                if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                        },
                        contentColor = titleColor,
                    )
                    DSpotWindowsControlButton(
                        icon = WindowControlIcon.Close,
                        onClick = onCloseRequest,
                        contentColor = titleColor,
                        isClose = true,
                    )
                }
            }
        }

    }
}

private enum class WindowControlIcon {
    Minimize,
    Maximize,
    Restore,
    Close,
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DSpotWindowsControlButton(
    icon: WindowControlIcon,
    onClick: () -> Unit,
    contentColor: Color,
    isClose: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue =
            when {
                isHovered && isClose -> Color(0xFFE81123)
                isHovered -> contentColor.copy(alpha = 0.12f)
                else -> Color.Transparent
            },
        animationSpec = tween(120),
        label = "winBtnBg",
    )

    val tint by animateColorAsState(
        targetValue =
            when {
                isHovered && isClose -> Color.White
                isHovered -> contentColor
                else -> contentColor.copy(alpha = 0.7f)
            },
        animationSpec = tween(120),
        label = "winBtnTint",
    )

    Box(
        modifier =
            Modifier
                .width(46.dp)
                .fillMaxHeight()
                .background(bg)
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
        contentAlignment = Alignment.Center,
    ) {
        when (icon) {
            WindowControlIcon.Minimize -> {
                Icon(
                    imageVector = SimpIcons.Remove,
                    contentDescription = "Minimize",
                    tint = tint,
                    modifier = Modifier.size(15.dp),
                )
            }
            WindowControlIcon.Maximize -> {
                Icon(
                    imageVector = SimpIcons.UnfoldMore,
                    contentDescription = "Maximize",
                    tint = tint,
                    modifier = Modifier.size(15.dp).rotate(45f),
                )
            }
            WindowControlIcon.Restore -> {
                Icon(
                    imageVector = SimpIcons.UnfoldLess,
                    contentDescription = "Restore",
                    tint = tint,
                    modifier = Modifier.size(15.dp).rotate(45f),
                )
            }
            WindowControlIcon.Close -> {
                Icon(
                    imageVector = SimpIcons.Close,
                    contentDescription = "Close",
                    tint = tint,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MacWindowControlButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    hoverColor: Color,
    icon: WindowControlIcon,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier =
            Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(if (isHovered) hoverColor else backgroundColor)
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
        contentAlignment = Alignment.Center,
    ) {
        if (isHovered) {
            when (icon) {
                WindowControlIcon.Minimize -> {
                    Icon(SimpIcons.Remove, tint = Color.DarkGray, contentDescription = "Minimize", modifier = Modifier.size(8.dp))
                }
                WindowControlIcon.Maximize -> {
                    Icon(SimpIcons.UnfoldMore, tint = Color.DarkGray, contentDescription = "Maximize", modifier = Modifier.size(8.dp).rotate(45f))
                }
                WindowControlIcon.Restore -> {
                    Icon(SimpIcons.UnfoldLess, tint = Color.DarkGray, contentDescription = "Restore", modifier = Modifier.size(8.dp).rotate(45f))
                }
                WindowControlIcon.Close -> {
                    Icon(SimpIcons.Close, tint = Color.DarkGray, contentDescription = "Close", modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}
