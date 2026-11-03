package com.mika.dspot.expect.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mika.domain.data.model.loginsync.LoginSyncHostState
import com.mika.dspot.ui.theme.typo
import com.mika.dspot.viewModel.LoginSyncHostViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.cancel
import dspot.composeapp.generated.resources.login_sync_android_title
import dspot.composeapp.generated.resources.login_sync_desktop_instructions
import dspot.composeapp.generated.resources.login_sync_desktop_title
import dspot.composeapp.generated.resources.login_sync_expired
import dspot.composeapp.generated.resources.login_sync_new_code
import dspot.composeapp.generated.resources.login_sync_no_network
import dspot.composeapp.generated.resources.login_sync_phone_connected
import dspot.composeapp.generated.resources.login_sync_pick_address
import dspot.composeapp.generated.resources.login_sync_receive_failed
import dspot.composeapp.generated.resources.login_sync_received
import dspot.composeapp.generated.resources.login_sync_start_failed
import dspot.composeapp.generated.resources.ok
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Total seconds a QR code is valid before the server expires it.
private const val QR_TTL_SECONDS = 120

@Composable
actual fun LoginSyncDialog(onDismiss: () -> Unit) {
    val viewModel: LoginSyncHostViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.open() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }

    val canRestart =
        state is LoginSyncHostState.Expired ||
            state is LoginSyncHostState.NoNetwork ||
            state is LoginSyncHostState.Failed ||
            state is LoginSyncHostState.Rejected

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.login_sync_desktop_title),
                style = typo().titleSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val s = state) {
                    LoginSyncHostState.Starting -> {
                        CircularProgressIndicator()
                    }

                    is LoginSyncHostState.Waiting -> {
                        // QR code + expiry countdown
                        QrCodeWithCountdown(
                            text    = s.invite,
                            onExpired = { viewModel.open() },
                        )
                        Text(
                            text = stringResource(
                                Res.string.login_sync_desktop_instructions,
                                stringResource(Res.string.login_sync_android_title),
                            ),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.login_sync_pick_address),
                            style = typo().bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        // Address selector — chip-style cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            s.addresses.forEach { address ->
                                val isSelected = address == s.selected
                                val cardColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cardColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { viewModel.select(address) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = address.ip,
                                            style = typo().bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        Text(
                                            text = address.interfaceName,
                                            style = typo().bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSelected) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(MaterialTheme.colorScheme.primary),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LoginSyncHostState.Connected -> {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(Res.string.login_sync_phone_connected),
                            textAlign = TextAlign.Center,
                        )
                    }

                    is LoginSyncHostState.Done -> {
                        Text(
                            text = stringResource(
                                Res.string.login_sync_received,
                                s.services.joinToString { it.label },
                            ),
                        )
                    }

                    LoginSyncHostState.Expired -> {
                        Text(
                            text = "⏱ " + stringResource(Res.string.login_sync_expired),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    LoginSyncHostState.NoNetwork -> {
                        Text(
                            text = "⚠️ " + stringResource(Res.string.login_sync_no_network),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is LoginSyncHostState.Failed -> {
                        Text(
                            text = "❌ " + stringResource(Res.string.login_sync_start_failed, s.message),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is LoginSyncHostState.Rejected -> {
                        Text(
                            text = "⛔ " + stringResource(Res.string.login_sync_receive_failed, s.reason),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(
                        if (state is LoginSyncHostState.Done) Res.string.ok else Res.string.cancel,
                    ),
                )
            }
        },
        dismissButton = if (canRestart) {
            {
                TextButton(onClick = viewModel::open) {
                    Text(text = stringResource(Res.string.login_sync_new_code))
                }
            }
        } else {
            null
        },
    )
}

// ─── QR code + countdown ring ─────────────────────────────────────────────────

/**
 * Shows a dark-on-white QR code with a circular expiry countdown ring drawn
 * around it and a seconds-remaining label underneath.
 *
 * @param text       The string to encode.
 * @param onExpired  Called when the countdown reaches zero (auto-refresh trigger).
 */
@Composable
private fun QrCodeWithCountdown(
    text: String,
    onExpired: () -> Unit,
) {
    var secondsLeft by remember(text) { mutableIntStateOf(QR_TTL_SECONDS) }
    val currentOnExpired by rememberUpdatedState(onExpired)

    LaunchedEffect(text) {
        secondsLeft = QR_TTL_SECONDS
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        currentOnExpired()
    }

    val fraction = secondsLeft.toFloat() / QR_TTL_SECONDS.toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 400),
        label = "qr_countdown",
    )

    val ringColor = when {
        fraction > 0.5f -> Color(0xFF4CAF50)  // green
        fraction > 0.25f -> Color(0xFFFF9800) // amber
        else -> Color(0xFFF44336)             // red
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // QR + ring overlay
        Box(contentAlignment = Alignment.Center) {
            // Countdown ring drawn behind (actually in front, same z-order layer)
            Canvas(modifier = Modifier.size(264.dp)) {
                val stroke  = 6.dp.toPx()
                val inset   = stroke / 2
                drawArc(
                    color      = ringColor.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke),
                    style      = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap   = StrokeCap.Round,
                    ),
                )
                drawArc(
                    color      = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter  = false,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke),
                    style      = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap   = StrokeCap.Round,
                    ),
                )
            }
            QrCode(text, Modifier.size(240.dp))
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text  = "${secondsLeft}s",
            style = typo().labelSmall,
            color = ringColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─── Plain QR code canvas ─────────────────────────────────────────────────────

/** Dark modules on white whatever the theme: scanners expect that polarity. */
@Composable
private fun QrCode(
    text: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(text) {
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    }
    Canvas(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                // Half a pixel of overlap hides the seams between fractional-width modules.
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
