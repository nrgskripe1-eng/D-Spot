package com.mika.dspot.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Chat: ImageVector
  get() {
    if (_Chat != null) {
      return _Chat!!
    }
    _Chat =
      ImageVector.Builder(
          name = "Chat",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
          autoMirror = true,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(20f, 2f)
            lineTo(4f, 2f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(2f, 22f)
            lineToRelative(4f, -4f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            lineTo(22f, 4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(18f, 14f)
            lineTo(6f, 14f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(2f)
            close()
            moveTo(18f, 11f)
            lineTo(6f, 11f)
            lineTo(6f, 9f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(2f)
            close()
            moveTo(18f, 8f)
            lineTo(6f, 8f)
            lineTo(6f, 6f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(2f)
            close()
          }
        }
        .build()
    return _Chat!!
  }

private var _Chat: ImageVector? = null

