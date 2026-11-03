package com.r2h.sirr

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    val top = if (selected) Color(0xFF4B264B).copy(alpha = .91f)
              else Color(0xFF36213F).copy(alpha = .85f)
    val middle = if (selected) Color(0xFF271833).copy(alpha = .92f)
                 else Color(0xFF1B1429).copy(alpha = .82f)
    val bottom = Color(0xFF090815).copy(alpha = if (selected) .93f else .88f)
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
                Color.White.copy(alpha = if (selected) .73f else .52f),
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


/**
 * The one intentional high-energy surface against the dark cinematic glass.
 * Opaque enough for white Arabic labels and never washed out by the wallpaper.
 */
@Composable
internal fun R2HNeonAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(if (compact) 15.dp else 19.dp)
    val gradient = if (enabled) {
        listOf(Color(0xFFF22F9E), Color(0xFFD83AB3), Color(0xFF913CC7))
    } else {
        listOf(Color(0xFF4A324A), Color(0xFF49324B))
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = if (compact) 45.dp else 55.dp)
            .shadow(
                elevation = if (enabled) 11.dp else 2.dp,
                shape = shape, ambientColor = Color(0xFFFC4FC2),
                spotColor = Color(0xFFFF64CF)
            )
            .clip(shape)
            .background(Brush.horizontalGradient(gradient))
            .border(
                1.dp,
                if (enabled) Color(0xFFFFD2F0) else Color(0xFF6F6073),
                shape
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFFDED0DD)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            fontSize = if (compact) 14.sp else 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
