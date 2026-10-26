package com.r2h.sirr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Purpose-drawn vector symbols. No emoji, font glyphs or third-party icon packs. */
internal enum class R2HGameArt {
    COUPLE, PARTY, DICE, QUIZ, WHISPERS, SECRET, ROOM, MUSIC
}

@Composable
internal fun R2HArtwork(kind: R2HGameArt, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = min(size.width, size.height) / 100f
        val x0 = (size.width - 100f * unit) / 2f
        val y0 = (size.height - 100f * unit) / 2f
        fun p(x: Float, y: Float) = Offset(x0 + x * unit, y0 + y * unit)
        fun path(block: Path.() -> Unit) = Path().apply(block)
        val pale = Color(0xFFFFE8D6)
        val light = Color.White
        val bright = Brush.linearGradient(
            listOf(pale, accent, Color(0xFFAB66B9)),
            start = p(20f, 15f), end = p(83f, 88f)
        )
        fun stroke(shape: Path, thick: Float = 3.0f, alpha: Float = 1f) {
            drawPath(
                shape, brush = if (alpha == 1f) bright else Brush.linearGradient(
                    listOf(pale.copy(alpha = alpha), accent.copy(alpha = alpha)),
                    start = p(20f, 20f), end = p(85f, 85f)
                ), style = Stroke(
                    width = thick * unit, cap = StrokeCap.Round, join = StrokeJoin.Round
                )
            )
        }
        fun dot(x: Float, y: Float, radius: Float = 2.8f, color: Color = pale) {
            drawCircle(color, radius * unit, p(x, y))
        }
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = .25f), accent.copy(alpha = .10f), Color.Transparent),
                center = p(50f, 49f), radius = 49f * unit
            ), radius = 49f * unit, center = p(50f, 49f)
        )
        drawCircle(
            accent.copy(alpha = .48f), radius = 38f * unit,
            center = p(50f, 49f), style = Stroke(width = .9f * unit)
        )
        drawCircle(
            pale.copy(alpha = .20f), radius = 32f * unit,
            center = p(50f, 49f), style = Stroke(width = .7f * unit)
        )
        when (kind) {
            R2HGameArt.COUPLE -> {
                drawCircle(bright, 8f * unit, p(35f, 37f))
                drawCircle(bright, 8f * unit, p(65f, 37f))
                stroke(path {
                    moveTo(p(17f, 69f).x, p(17f, 69f).y)
                    cubicTo(p(18f, 53f).x, p(18f, 53f).y, p(34f, 50f).x, p(34f, 50f).y, p(44f, 59f).x, p(44f, 59f).y)
                    moveTo(p(83f, 69f).x, p(83f, 69f).y)
                    cubicTo(p(82f, 53f).x, p(82f, 53f).y, p(66f, 50f).x, p(66f, 50f).y, p(56f, 59f).x, p(56f, 59f).y)
                }, 4.4f)
                val heart = path {
                    moveTo(p(50f, 66f).x, p(50f, 66f).y)
                    cubicTo(p(32f, 54f).x, p(32f, 54f).y, p(40f, 45f).x, p(40f, 45f).y, p(50f, 53f).x, p(50f, 53f).y)
                    cubicTo(p(60f, 45f).x, p(60f, 45f).y, p(68f, 54f).x, p(68f, 54f).y, p(50f, 66f).x, p(50f, 66f).y)
                    close()
                }
                drawPath(heart, bright)
                dot(50f, 76f, 1.5f)
            }
            R2HGameArt.PARTY -> {
                listOf(34f to 42f, 50f to 32f, 66f to 42f).forEachIndexed { i, pos ->
                    drawCircle(bright, (if (i == 1) 7f else 6f) * unit, p(pos.first, pos.second))
                }
                listOf(34f to 63f, 50f to 59f, 66f to 63f).forEachIndexed { i, pos ->
                    val w = if (i == 1) 16f else 13f
                    drawRoundRect(
                        brush = bright, topLeft = p(pos.first - w / 2f, pos.second - 7f),
                        size = Size(w * unit, 18f * unit), cornerRadius = CornerRadius(8f * unit)
                    )
                }
                stroke(path {
                    moveTo(p(23f, 76f).x, p(23f, 76f).y)
                    quadraticTo(p(50f, 90f).x, p(50f, 90f).y, p(77f, 76f).x, p(77f, 76f).y)
                }, 2.3f, .8f)
                dot(21f, 35f, 2f); dot(79f, 35f, 2f)
            }
            R2HGameArt.DICE -> {
                val top = path {
                    moveTo(p(50f, 19f).x, p(50f, 19f).y)
                    lineTo(p(78f, 35f).x, p(78f, 35f).y)
                    lineTo(p(50f, 50f).x, p(50f, 50f).y)
                    lineTo(p(22f, 35f).x, p(22f, 35f).y)
                    close()
                }
                val left = path {
                    moveTo(p(22f, 35f).x, p(22f, 35f).y)
                    lineTo(p(50f, 50f).x, p(50f, 50f).y)
                    lineTo(p(50f, 82f).x, p(50f, 82f).y)
                    lineTo(p(22f, 66f).x, p(22f, 66f).y)
                    close()
                }
                val right = path {
                    moveTo(p(78f, 35f).x, p(78f, 35f).y)
                    lineTo(p(50f, 50f).x, p(50f, 50f).y)
                    lineTo(p(50f, 82f).x, p(50f, 82f).y)
                    lineTo(p(78f, 66f).x, p(78f, 66f).y)
                    close()
                }
                drawPath(top, Brush.linearGradient(listOf(pale, accent),p(36f, 20f),p(73f, 46f)))
                drawPath(left, Color(0xFFAC57A2))
                drawPath(right, Brush.linearGradient(listOf(accent, Color(0xFF532D74)),p(60f,40f),p(78f,80f)))
                stroke(top, 1.5f); stroke(left, 1.5f); stroke(right, 1.5f)
                dot(50f, 35f, 2.3f, Color(0xFF572C61))
                dot(34f, 52f, 2.4f); dot(41f, 66f, 2.4f)
                dot(62f, 51f, 2.4f); dot(70f, 60f, 2.4f); dot(60f, 69f, 2.4f)
            }
            R2HGameArt.QUIZ -> {
                stroke(path {
                    moveTo(p(35f, 39f).x, p(35f, 39f).y)
                    cubicTo(p(35f, 20f).x, p(35f, 20f).y, p(68f, 20f).x, p(68f, 20f).y, p(68f, 43f).x, p(68f, 43f).y)
                    cubicTo(p(68f, 56f).x, p(68f, 56f).y, p(50f, 54f).x, p(50f, 54f).y, p(50f, 65f).x, p(50f, 65f).y)
                }, 5.6f)
                dot(50f, 77f, 4.1f)
                dot(79f, 25f, 1.9f); dot(22f, 60f, 1.7f)
            }
            R2HGameArt.WHISPERS -> {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(accent, Color(0xFF8B4A9C))),
                    topLeft = p(20f, 36f), size = Size(60f * unit, 39f * unit),
                    cornerRadius = CornerRadius(6f * unit)
                )
                stroke(path {
                    moveTo(p(22f, 39f).x, p(22f, 39f).y)
                    lineTo(p(50f, 59f).x, p(50f, 59f).y)
                    lineTo(p(78f, 39f).x, p(78f, 39f).y)
                }, 2.8f)
                stroke(path {
                    moveTo(p(23f, 72f).x, p(23f, 72f).y)
                    lineTo(p(41f, 55f).x, p(41f, 55f).y)
                    moveTo(p(77f, 72f).x, p(77f, 72f).y)
                    lineTo(p(59f, 55f).x, p(59f, 55f).y)
                }, 2f)
                drawCircle(Color(0xFFFFD6E6), 6f * unit, p(50f, 59f))
                drawCircle(accent, 2.7f * unit, p(50f, 59f))
            }
            R2HGameArt.SECRET -> {
                val seal = path {
                    moveTo(p(50f, 24f).x, p(50f, 24f).y)
                    cubicTo(p(34f, 24f).x, p(34f, 24f).y, p(28f, 35f).x, p(28f, 35f).y, p(28f, 46f).x, p(28f, 46f).y)
                    cubicTo(p(28f, 57f).x, p(28f, 57f).y, p(36f, 61f).x, p(36f, 61f).y, p(40f, 64f).x, p(40f, 64f).y)
                    lineTo(p(39f, 80f).x, p(39f, 80f).y)
                    lineTo(p(61f, 80f).x, p(61f, 80f).y)
                    lineTo(p(60f, 64f).x, p(60f, 64f).y)
                    cubicTo(p(64f, 61f).x, p(64f, 61f).y, p(72f, 57f).x, p(72f, 57f).y, p(72f, 46f).x, p(72f, 46f).y)
                    cubicTo(p(72f, 35f).x, p(72f, 35f).y, p(66f, 24f).x, p(66f, 24f).y, p(50f, 24f).x, p(50f, 24f).y)
                    close()
                }
                drawPath(seal, brush = bright)
                drawCircle(Color(0xFF45214B), 12f * unit, p(50f, 45f))
                drawCircle(pale, 4.5f * unit, p(50f, 44f))
                drawPath(path {
                    moveTo(p(47f, 47f).x, p(47f, 47f).y)
                    lineTo(p(43f, 59f).x, p(43f, 59f).y)
                    lineTo(p(57f, 59f).x, p(57f, 59f).y)
                    lineTo(p(53f, 47f).x, p(53f, 47f).y)
                    close()
                }, pale)
            }
            R2HGameArt.ROOM -> {
                stroke(path {
                    moveTo(p(23f, 67f).x, p(23f, 67f).y)
                    lineTo(p(23f, 31f).x, p(23f, 31f).y)
                    lineTo(p(51f, 20f).x, p(51f, 20f).y)
                    lineTo(p(77f, 31f).x, p(77f, 31f).y)
                    lineTo(p(77f, 67f).x, p(77f, 67f).y)
                    lineTo(p(51f, 79f).x, p(51f, 79f).y)
                    close()
                    moveTo(p(51f, 79f).x, p(51f, 79f).y)
                    lineTo(p(51f, 45f).x, p(51f, 45f).y)
                    lineTo(p(77f, 31f).x, p(77f, 31f).y)
                }, 3.2f)
                dot(62f, 60f, 2.3f)
            }
            R2HGameArt.MUSIC -> {
                stroke(path {
                    moveTo(p(43f, 30f).x, p(43f, 30f).y)
                    lineTo(p(70f, 23f).x, p(70f, 23f).y)
                    lineTo(p(70f, 65f).x, p(70f, 65f).y)
                    moveTo(p(43f, 30f).x, p(43f, 30f).y)
                    lineTo(p(43f, 74f).x, p(43f, 74f).y)
                }, 4.5f)
                drawCircle(bright, 7f * unit, p(35f, 74f))
                drawCircle(bright, 7f * unit, p(62f, 65f))
                stroke(path {
                    moveTo(p(43f, 40f).x, p(43f, 40f).y)
                    lineTo(p(70f, 33f).x, p(70f, 33f).y)
                }, 2.2f)
            }
        }
        dot(20f, 21f, 1.4f, light.copy(alpha = .75f))
        dot(81f, 76f, 1.1f, pale.copy(alpha = .8f))
    }
}

/** Consistent engraved navigation chevron, not an arbitrary emoji arrow. */
@Composable
internal fun R2HCardChevron(modifier: Modifier = Modifier, color: Color = Color(0xFFFFDFC8)) {
    Canvas(modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val a = Offset(15f * sx, 5f * sy)
        val b = Offset(8f * sx, 12f * sy)
        val c = Offset(15f * sx, 19f * sy)
        drawLine(color, a, b, strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, b, c, strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
    }
}
