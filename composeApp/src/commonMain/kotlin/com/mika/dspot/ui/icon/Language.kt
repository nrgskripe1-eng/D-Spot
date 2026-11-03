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
val SimpIcons.Language: ImageVector
  get() {
    if (_Language != null) {
      return _Language!!
    }
    _Language =
      ImageVector.Builder(
          name = "Language",
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
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(12f, 22f)
            quadToRelative(-2.075f, 0f, -3.9f, -0.788f)
            reflectiveQuadToRelative(-3.175f, -2.137f)
            quadTo(3.575f, 17.925f, 2.788f, 16.1f)
            quadTo(2f, 14.275f, 2f, 12f)
            reflectiveQuadToRelative(0.788f, -3.9f)
            reflectiveQuadToRelative(2.137f, -3.175f)
            quadTo(6.275f, 3.575f, 8.1f, 2.788f)
            quadTo(9.925f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.788f)
            reflectiveQuadToRelative(3.175f, 2.137f)
            quadTo(20.425f, 6.075f, 21.213f, 7.9f)
            quadTo(22f, 9.725f, 22f, 12f)
            reflectiveQuadToRelative(-0.788f, 3.9f)
            reflectiveQuadToRelative(-2.137f, 3.175f)
            quadTo(17.725f, 20.425f, 15.9f, 21.213f)
            quadTo(14.075f, 22f, 12f, 22f)
            close()
            moveTo(12f, 20f)
            quadToRelative(0.45f, -0.65f, 0.8f, -1.325f)
            quadToRelative(0.35f, -0.675f, 0.625f, -1.375f)
            horizontalLineTo(10.575f)
            quadToRelative(0.275f, 0.7f, 0.625f, 1.375f)
            quadTo(11.55f, 19.35f, 12f, 20f)
            close()
            moveTo(9.75f, 19.65f)
            quadToRelative(-0.325f, -0.625f, -0.6f, -1.3f)
            quadToRelative(-0.275f, -0.675f, -0.475f, -1.35f)
            horizontalLineTo(6.1f)
            quadToRelative(0.7f, 1.125f, 1.75f, 1.938f)
            quadToRelative(1.05f, 0.812f, 1.9f, 0.712f)
            close()
            moveTo(14.25f, 19.65f)
            quadToRelative(0.9f, 0.1f, 1.95f, -0.712f)
            quadToRelative(1.05f, -0.813f, 1.75f, -1.938f)
            horizontalLineTo(15.325f)
            quadToRelative(-0.2f, 0.675f, -0.475f, 1.35f)
            quadToRelative(-0.275f, 0.675f, -0.6f, 1.3f)
            close()
            moveTo(5.25f, 15.3f)
            horizontalLineToRelative(2.4f)
            quadToRelative(-0.075f, -0.325f, -0.1f, -0.65f)
            quadToRelative(-0.025f, -0.325f, -0.025f, -0.65f)
            reflectiveQuadToRelative(0.025f, -0.65f)
            quadToRelative(0.025f, -0.325f, 0.1f, -0.65f)
            horizontalLineTo(5.25f)
            quadToRelative(-0.125f, 0.325f, -0.188f, 0.65f)
            quadTo(5f, 13.675f, 5f, 14f)
            reflectiveQuadToRelative(0.063f, 0.65f)
            quadToRelative(0.062f, 0.325f, 0.187f, 0.65f)
            close()
            moveTo(8.7f, 15.3f)
            horizontalLineToRelative(6.6f)
            quadToRelative(0.075f, -0.325f, 0.1f, -0.65f)
            quadToRelative(0.025f, -0.325f, 0.025f, -0.65f)
            reflectiveQuadToRelative(-0.025f, -0.65f)
            quadToRelative(-0.025f, -0.325f, -0.1f, -0.65f)
            horizontalLineTo(8.7f)
            quadToRelative(-0.075f, 0.325f, -0.1f, 0.65f)
            quadTo(8.575f, 13.675f, 8.575f, 14f)
            reflectiveQuadToRelative(0.025f, 0.65f)
            quadToRelative(0.025f, 0.325f, 0.1f, 0.65f)
            close()
            moveTo(18.75f, 15.3f)
            quadToRelative(0.125f, -0.325f, 0.188f, -0.65f)
            quadTo(19f, 14.325f, 19f, 14f)
            reflectiveQuadToRelative(-0.063f, -0.65f)
            quadToRelative(-0.062f, -0.325f, -0.187f, -0.65f)
            horizontalLineToRelative(-2.4f)
            quadToRelative(0.075f, 0.325f, 0.1f, 0.65f)
            quadToRelative(0.025f, 0.325f, 0.025f, 0.65f)
            reflectiveQuadToRelative(-0.025f, 0.65f)
            quadToRelative(-0.025f, 0.325f, -0.1f, 0.65f)
            close()
            moveTo(15.325f, 11.3f)
            horizontalLineToRelative(2.575f)
            quadToRelative(-0.7f, -1.125f, -1.75f, -1.937f)
            quadToRelative(-1.05f, -0.813f, -1.9f, -0.713f)
            quadToRelative(0.325f, 0.625f, 0.6f, 1.3f)
            quadToRelative(0.275f, 0.675f, 0.475f, 1.35f)
            close()
            moveTo(10.575f, 11.3f)
            horizontalLineToRelative(2.85f)
            quadToRelative(-0.275f, -0.7f, -0.625f, -1.375f)
            quadTo(12.45f, 9.25f, 12f, 8.6f)
            quadToRelative(-0.45f, 0.65f, -0.8f, 1.325f)
            quadToRelative(-0.35f, 0.675f, -0.625f, 1.375f)
            close()
            moveTo(6.1f, 11.3f)
            horizontalLineToRelative(2.575f)
            quadToRelative(0.2f, -0.675f, 0.475f, -1.35f)
            quadToRelative(0.275f, -0.675f, 0.6f, -1.3f)
            quadToRelative(-0.9f, -0.1f, -1.95f, 0.713f)
            quadToRelative(-1.05f, 0.812f, -1.7f, 1.937f)
            close()
          }
        }
        .build()
    return _Language!!
  }

private var _Language: ImageVector? = null
