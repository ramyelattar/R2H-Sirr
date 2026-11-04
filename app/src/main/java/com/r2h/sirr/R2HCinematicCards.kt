package com.r2h.sirr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Individual compositions and metallic artwork. These are real clickable UI
// cards, not a flat icon placed over a recycled glass rectangle.
private fun R2HGameArt.sequence(): String = when (this) {
    R2HGameArt.COUPLE -> "01"
    R2HGameArt.PARTY -> "02"
    R2HGameArt.DICE -> "03"
    R2HGameArt.QUIZ -> "04"
    R2HGameArt.WHISPERS -> "05"
    R2HGameArt.SECRET -> "06"
    R2HGameArt.ROOM -> "07"
    R2HGameArt.MUSIC -> "08"
}

private fun R2HGameArt.caption(): String = when (this) {
    R2HGameArt.COUPLE -> "قُرب"
    R2HGameArt.PARTY -> "لَمّة"
    R2HGameArt.DICE -> "مفاجأة"
    R2HGameArt.QUIZ -> "اكتشاف"
    R2HGameArt.WHISPERS -> "رسالة"
    R2HGameArt.SECRET -> "سرّ"
    R2HGameArt.ROOM -> "اتصال"
    R2HGameArt.MUSIC -> "أجواء"
}

private fun R2HGameArt.artFirst(): Boolean =
    this == R2HGameArt.COUPLE || this == R2HGameArt.WHISPERS ||
        this == R2HGameArt.QUIZ

/** Thin engravings and a diffuse spot, not another opaque background panel. */
@Composable
private fun CardOrnament(art: R2HGameArt, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val onLeft = art.artFirst()
        val cx = if (onLeft) w * .20f else w * .79f
        val cy = h * .27f
        val radius = size.minDimension * .58f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    accent.copy(alpha = .14f),
                    accent.copy(alpha = .045f),
                    Color.Transparent
                ),
                center = Offset(cx, cy), radius = radius
            ), center = Offset(cx, cy), radius = radius
        )
        when (art) {
            R2HGameArt.COUPLE, R2HGameArt.WHISPERS -> {
                for (i in 0..2) {
                    drawCircle(
                        accent.copy(alpha = .14f / (i + 1)),
                        radius = size.minDimension * (.29f + i * .16f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            R2HGameArt.PARTY, R2HGameArt.DICE -> {
                for (i in 0..3) {
                    val d = i * w * .085f
                    drawLine(
                        color = accent.copy(alpha = .11f),
                        start = Offset(w * .53f + d, h * .05f),
                        end = Offset(w * .87f + d, h * .37f),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
            R2HGameArt.QUIZ, R2HGameArt.SECRET -> {
                for (i in 1..3) {
                    drawCircle(
                        accent.copy(alpha = .12f / i),
                        radius = size.minDimension * (.26f + i * .12f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                }
            }
            else -> Unit
        }
        // A restrained diagonal highlight along the bottom-right edge.
        drawLine(
            color = Color.White.copy(alpha = .13f),
            start = Offset(w * .47f, h - 1.6.dp.toPx()),
            end = Offset(w * .92f, h - 1.6.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun ArtIndex(art: R2HGameArt, accent: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            art.sequence(), color = accent, fontSize = 12.sp,
            lineHeight = 16.sp, fontWeight = FontWeight.Bold
        )
        Text(
            art.caption(), color = Color(0xFFFFE2EC),
            fontSize = 11.sp, lineHeight = 16.sp
        )
    }
}

@Composable
internal fun R2HModeCard(
    title: String,
    description: String,
    art: R2HGameArt,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = shape,
        modifier = modifier.heightIn(min = 172.dp)
            .r2hGlass(accent = tint, corner = 24.dp, lift = 16.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = .105f),
                        Color(0xFF171024).copy(alpha = .31f),
                        Color(0xFF080715).copy(alpha = .45f)
                    )
                )
            )
        ) {
            CardOrnament(art, tint)
            Column(
                Modifier.fillMaxWidth().padding(
                    start = 13.dp, end = 13.dp, top = 9.dp, bottom = 15.dp
                ), verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().height(82.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (art.artFirst()) {
                        R2HArtwork(art, tint, Modifier.size(83.dp))
                        ArtIndex(art, tint)
                    } else {
                        ArtIndex(art, tint)
                        R2HArtwork(art, tint, Modifier.size(83.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    title, color = Color.White,
                    fontSize = 18.sp, lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    description, color = Color(0xFFF2DEEA),
                    fontSize = 14.sp, lineHeight = 20.sp,
                    maxLines = 2
                )
            }
        }
    }
}

/** Featured game follows its own wide composition and strong neon call to action. */
@Composable
internal fun R2HFeaturedHomeCard(onClick: () -> Unit) {
    val accent = Color(0xFFFF8BCE)
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp)
            .r2hGlass(accent = accent, corner = 28.dp, selected = true, lift = 22.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                val glow = Offset(size.width * .77f, size.height * .46f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = .15f), accent.copy(alpha = .045f),
                            Color.Transparent
                        ),
                        center = glow, radius = size.minDimension * .54f
                    ), radius = size.minDimension * .54f, center = glow
                )
            }
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "اللعبة الأساسية", color = Color(0xFFFFD5B3),
                        fontSize = 12.sp, lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "مين هيكشف\nالسر الأول؟", color = Color.White,
                        fontSize = 22.sp, lineHeight = 29.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                    R2HNeonAction(
                        text = "ابدأوا اللعبة",
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
                R2HArtwork(
                    R2HGameArt.SECRET, Color(0xFFFFD0A2),
                    Modifier.size(118.dp)
                )
            }
        }
    }
}

/** Compact premium utility card—same material, independently composed. */
@Composable
internal fun FeatureRow(
    title: String,
    detail: String,
    art: R2HGameArt,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(21.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 89.dp)
            .r2hGlass(accent = tint, corner = 21.dp, lift = 12.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            CardOrnament(art, tint)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                R2HArtwork(art, tint, Modifier.size(65.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        title, color = Color.White,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                    Text(
                        detail, color = Color(0xFFF2DEEA),
                        fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
                R2HCardChevron(Modifier.size(21.dp))
            }
        }
    }
}
