package com.r2h.sirr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable translucent raised surface. Keeps the actual wallpaper visible through
 * every game card without a costly, per-frame bitmap blur or raster cache.
 */
internal fun Modifier.r2hGlass(
    accent: Color = Color(0xFFFFA6D9),
    corner: Dp = 24.dp,
    selected: Boolean = false,
    lift: Dp = 12.dp
): Modifier {
    val shape = RoundedCornerShape(corner)
    val top = if (selected) Color(0xFF5E2859).copy(alpha = .74f)
              else Color(0xFF44264F).copy(alpha = .55f)
    val middle = if (selected) Color(0xFF311A42).copy(alpha = .78f)
                 else Color(0xFF21192F).copy(alpha = .52f)
    val bottom = Color(0xFF090815).copy(alpha = if (selected) .79f else .61f)
    return this
        .shadow(
            elevation = lift, shape = shape, clip = false,
            ambientColor = accent.copy(alpha = .23f),
            spotColor = accent.copy(alpha = .40f)
        )
        .clip(shape)
        .background(
            Brush.linearGradient(listOf(
                top, middle, bottom
            ))
        )
        .border(
            width = if (selected) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (selected) .65f else .45f),
                accent.copy(alpha = .72f),
                accent.copy(alpha = .23f)
            )),
            shape = shape
        )
        .drawWithContent {
            drawContent()
            val inset = 17.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = if (selected) .30f else .18f),
                start = Offset(inset, 1.3.dp.toPx()),
                end = Offset(size.width - inset, 1.3.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
}
