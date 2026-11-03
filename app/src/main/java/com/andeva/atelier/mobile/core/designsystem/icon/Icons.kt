package com.andeva.atelier.mobile.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val person: ImageVector
    get() {
        if (_person != null) {
            return _person!!
        }
        _person =
            ImageVector.Builder(
                name = "person",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
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
                        moveTo(9.18f, 10.83f)
                        quadTo(8f, 9.65f, 8f, 8f)
                        reflectiveQuadTo(9.18f, 5.18f)
                        reflectiveQuadTo(12f, 4f)
                        reflectiveQuadToRelative(2.83f, 1.18f)
                        reflectiveQuadTo(16f, 8f)
                        reflectiveQuadToRelative(-1.17f, 2.82f)
                        reflectiveQuadTo(12f, 12f)
                        reflectiveQuadTo(9.18f, 10.83f)
                        close()
                        moveTo(4f, 20f)
                        verticalLineTo(17.2f)
                        quadTo(4f, 16.35f, 4.44f, 15.64f)
                        quadTo(4.88f, 14.93f, 5.6f, 14.55f)
                        quadTo(7.15f, 13.77f, 8.75f, 13.39f)
                        reflectiveQuadTo(12f, 13f)
                        reflectiveQuadToRelative(3.25f, 0.39f)
                        reflectiveQuadToRelative(3.15f, 1.16f)
                        quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
                        reflectiveQuadTo(20f, 17.2f)
                        verticalLineTo(20f)
                        horizontalLineTo(4f)
                        close()
                        moveTo(6f, 18f)
                        horizontalLineTo(18f)
                        verticalLineTo(17.2f)
                        quadToRelative(0f, -0.27f, -0.14f, -0.5f)
                        quadTo(17.73f, 16.48f, 17.5f, 16.35f)
                        quadTo(16.15f, 15.68f, 14.78f, 15.34f)
                        reflectiveQuadTo(12f, 15f)
                        reflectiveQuadTo(9.23f, 15.34f)
                        reflectiveQuadTo(6.5f, 16.35f)
                        quadTo(6.28f, 16.48f, 6.14f, 16.7f)
                        quadTo(6f, 16.93f, 6f, 17.2f)
                        verticalLineTo(18f)
                        close()
                        moveTo(13.41f, 9.41f)
                        quadTo(14f, 8.82f, 14f, 8f)
                        reflectiveQuadTo(13.41f, 6.59f)
                        reflectiveQuadTo(12f, 6f)
                        reflectiveQuadTo(10.59f, 6.59f)
                        quadTo(10f, 7.18f, 10f, 8f)
                        reflectiveQuadToRelative(0.59f, 1.41f)
                        reflectiveQuadTo(12f, 10f)
                        reflectiveQuadTo(13.41f, 9.41f)
                        close()
                        moveTo(12f, 8f)
                        close()
                        moveToRelative(0f, 10f)
                        close()
                    }
                }
                .build()
        return _person!!
    }

private var _person: ImageVector? = null


@Suppress("CheckReturnValue")
public val assignment: ImageVector
    get() {
        if (_assignment != null) {
            return _assignment!!
        }
        _assignment =
            ImageVector.Builder(
                name = "assignment",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
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
                        moveTo(5f, 21f)
                        quadTo(4.18f, 21f, 3.59f, 20.41f)
                        reflectiveQuadTo(3f, 19f)
                        verticalLineTo(5f)
                        quadTo(3f, 4.17f, 3.59f, 3.59f)
                        reflectiveQuadTo(5f, 3f)
                        horizontalLineTo(9.2f)
                        quadTo(9.53f, 2.1f, 10.29f, 1.55f)
                        reflectiveQuadTo(12f, 1f)
                        reflectiveQuadToRelative(1.71f, 0.55f)
                        reflectiveQuadTo(14.8f, 3f)
                        horizontalLineTo(19f)
                        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                        reflectiveQuadTo(21f, 5f)
                        verticalLineTo(19f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(19f, 21f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(5f, 19f)
                        horizontalLineTo(19f)
                        verticalLineTo(5f)
                        horizontalLineTo(5f)
                        verticalLineTo(19f)
                        close()
                        moveTo(7f, 17f)
                        horizontalLineToRelative(7f)
                        verticalLineTo(15f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(7f, 13f)
                        horizontalLineTo(17f)
                        verticalLineTo(11f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(7f, 9f)
                        horizontalLineTo(17f)
                        verticalLineTo(7f)
                        horizontalLineTo(7f)
                        verticalLineTo(9f)
                        close()
                        moveTo(12.54f, 4.04f)
                        quadTo(12.75f, 3.82f, 12.75f, 3.5f)
                        quadToRelative(0f, -0.33f, -0.21f, -0.54f)
                        reflectiveQuadTo(12f, 2.75f)
                        reflectiveQuadTo(11.46f, 2.96f)
                        reflectiveQuadTo(11.25f, 3.5f)
                        quadToRelative(0f, 0.32f, 0.21f, 0.54f)
                        reflectiveQuadTo(12f, 4.25f)
                        reflectiveQuadTo(12.54f, 4.04f)
                        close()
                        moveTo(5f, 19f)
                        verticalLineTo(5f)
                        verticalLineTo(19f)
                        close()
                    }
                }
                .build()
        return _assignment!!
    }

private var _assignment: ImageVector? = null


@Suppress("CheckReturnValue")
public val bluetooth_searching: ImageVector
    get() {
        if (_bluetooth_searching != null) {
            return _bluetooth_searching!!
        }
        _bluetooth_searching =
            ImageVector.Builder(
                name = "bluetooth_searching",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
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
                        moveTo(9f, 22f)
                        verticalLineTo(14.4f)
                        lineTo(4.4f, 19f)
                        lineTo(3f, 17.6f)
                        lineTo(8.6f, 12f)
                        lineTo(3f, 6.4f)
                        lineTo(4.4f, 5f)
                        lineTo(9f, 9.6f)
                        verticalLineTo(2f)
                        horizontalLineToRelative(1f)
                        lineToRelative(5.7f, 5.7f)
                        lineTo(11.4f, 12f)
                        lineToRelative(4.3f, 4.3f)
                        lineTo(10f, 22f)
                        horizontalLineTo(9f)
                        close()
                        moveTo(11f, 9.6f)
                        lineTo(12.9f, 7.7f)
                        lineTo(11f, 5.85f)
                        verticalLineTo(9.6f)
                        close()
                        moveToRelative(0f, 8.55f)
                        lineTo(12.9f, 16.3f)
                        lineTo(11f, 14.4f)
                        verticalLineToRelative(3.75f)
                        close()
                        moveToRelative(5.55f, -3.8f)
                        lineTo(14.25f, 12f)
                        lineToRelative(2.3f, -2.3f)
                        quadToRelative(0.23f, 0.55f, 0.36f, 1.13f)
                        reflectiveQuadTo(17.05f, 12f)
                        reflectiveQuadToRelative(-0.14f, 1.19f)
                        quadToRelative(-0.14f, 0.59f, -0.36f, 1.16f)
                        close()
                        moveTo(19.5f, 17.2f)
                        lineTo(18.25f, 16f)
                        quadToRelative(0.5f, -0.93f, 0.78f, -1.94f)
                        reflectiveQuadTo(19.3f, 12f)
                        reflectiveQuadTo(19.03f, 9.94f)
                        quadTo(18.75f, 8.92f, 18.25f, 8f)
                        lineTo(19.5f, 6.75f)
                        quadToRelative(0.73f, 1.2f, 1.11f, 2.52f)
                        reflectiveQuadTo(21f, 12f)
                        reflectiveQuadToRelative(-0.39f, 2.71f)
                        quadTo(20.23f, 16.02f, 19.5f, 17.2f)
                        close()
                    }
                }
                .build()
        return _bluetooth_searching!!
    }

private var _bluetooth_searching: ImageVector? = null

@Suppress("CheckReturnValue")
public val home: ImageVector
    get() {
        if (_home != null) {
            return _home!!
        }
        _home =
            ImageVector.Builder(
                name = "home",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
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
                        moveTo(6f, 19f)
                        horizontalLineTo(9f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(6f)
                        verticalLineToRelative(6f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(10f)
                        lineTo(12f, 5.5f)
                        lineTo(6f, 10f)
                        verticalLineToRelative(9f)
                        close()
                        moveTo(4f, 21f)
                        verticalLineTo(9f)
                        lineTo(12f, 3f)
                        lineToRelative(8f, 6f)
                        verticalLineTo(21f)
                        horizontalLineTo(13f)
                        verticalLineTo(15f)
                        horizontalLineTo(11f)
                        verticalLineToRelative(6f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(8f, -8.75f)
                        close()
                    }
                }
                .build()
        return _home!!
    }

private var _home: ImageVector? = null