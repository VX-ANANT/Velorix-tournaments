package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

@Composable
fun CircleLoader(modifier: Modifier = Modifier, pathColor: Color = Color(0xFF2F3545), dotColor: Color = Color(0xFF5628EE)) {
    val infiniteTransition = rememberInfiniteTransition(label = "CircleLoader")
    val offset by infiniteTransition.animateFloat(
        initialValue = 75f,
        targetValue = 275f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                75f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                125f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                175f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                225f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                275f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    val dotX by infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -18f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                18f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotX"
    )
    val dotY by infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -18f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -36f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotY"
    )

    Canvas(modifier = modifier.size(44.dp)) {
        val scaleX = size.width / 80f
        val scaleY = size.height / 80f
        scale(scaleX, scaleY, pivot = Offset.Zero) {
            val path = Path().apply {
                addOval(Rect(8f, 8f, 72f, 72f))
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = 10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(150f, 50f), offset)
                )
            )
        }
        
        // Draw Dot in 44x44 space
        val dotCenterX = 19.dp.toPx() + 3.dp.toPx() + dotX.dp.toPx()
        val dotCenterY = 37.dp.toPx() + 3.dp.toPx() + dotY.dp.toPx()
        drawCircle(
            color = dotColor,
            radius = 3.dp.toPx(),
            center = Offset(dotCenterX, dotCenterY)
        )
    }
}

@Composable
fun RectLoader(modifier: Modifier = Modifier, pathColor: Color = Color(0xFF2F3545), dotColor: Color = Color(0xFF5628EE)) {
    val infiniteTransition = rememberInfiniteTransition(label = "RectLoader")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 256f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                64f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                128f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                192f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                256f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    val dotX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                18f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotX"
    )
    val dotY by infiniteTransition.animateFloat(
        initialValue = -36f, targetValue = -36f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -36f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 750 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 1500 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 2250 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -36f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotY"
    )

    Canvas(modifier = modifier.size(44.dp)) {
        val scaleX = size.width / 80f
        val scaleY = size.height / 80f
        scale(scaleX, scaleY, pivot = Offset.Zero) {
            val path = Path().apply {
                moveTo(8f, 8f)
                lineTo(8f, 72f)
                lineTo(72f, 72f)
                lineTo(72f, 8f)
                lineTo(8f, 8f)
                close()
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = 10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(192f, 64f), offset)
                )
            )
        }
        
        // Draw Dot in 44x44 space
        val dotCenterX = 19.dp.toPx() + 3.dp.toPx() + dotX.dp.toPx()
        val dotCenterY = 37.dp.toPx() + 3.dp.toPx() + dotY.dp.toPx()
        drawCircle(
            color = dotColor,
            radius = 3.dp.toPx(),
            center = Offset(dotCenterX, dotCenterY)
        )
    }
}

@Composable
fun TriangleLoader(modifier: Modifier = Modifier, pathColor: Color = Color(0xFF2F3545), dotColor: Color = Color(0xFF5628EE)) {
    val infiniteTransition = rememberInfiniteTransition(label = "TriangleLoader")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 221f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                74f at 1000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                147f at 2000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                221f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    val dotX by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -10f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 1000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                10f at 2000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -10f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotX"
    )
    val dotY by infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -18f at 0 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                0f at 1000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 2000 with CubicBezierEasing(0.785f, 0.135f, 0.15f, 0.86f)
                -18f at 3000
            },
            repeatMode = RepeatMode.Restart
        ), label = "dotY"
    )

    Canvas(modifier = modifier.size(48.dp, 44.dp)) {
        val scaleX = size.width / 86f
        val scaleY = size.height / 80f
        scale(scaleX, scaleY, pivot = Offset.Zero) {
            val path = Path().apply {
                moveTo(43f, 8f)
                lineTo(79f, 72f)
                lineTo(7f, 72f)
                close()
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = 10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(145f, 76f), offset)
                )
            )
        }
        
        // .loader.triangle:before left=21px, top=37px
        val dotCenterX = 21.dp.toPx() + 3.dp.toPx() + dotX.dp.toPx()
        val dotCenterY = 37.dp.toPx() + 3.dp.toPx() + dotY.dp.toPx()
        drawCircle(
            color = dotColor,
            radius = 3.dp.toPx(),
            center = Offset(dotCenterX, dotCenterY)
        )
    }
}

@Composable
fun AnimatedLoaders(
    modifier: Modifier = Modifier, 
    pathColor: Color = Color(0xFF2F3545), 
    dotColor: Color = Color(0xFF5628EE)
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleLoader(pathColor = pathColor, dotColor = dotColor)
        Spacer(Modifier.width(16.dp))
        TriangleLoader(pathColor = pathColor, dotColor = dotColor)
        Spacer(Modifier.width(16.dp))
        RectLoader(pathColor = pathColor, dotColor = dotColor)
    }
}
