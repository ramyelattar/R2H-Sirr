package com.r2h.sirr

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// M10: one shared visual language for all game entry/decision surfaces.
private val M10Cream = Color(0xFFFFD2A4)
private val M10Ink = Color(0xFF23121B)
private val M10Faint = Color(0xFFE7CCD7)
private val M10Panel = Color(0xFF201621)

@Composable
internal fun SirrAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    explanation: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFA76B9),
                contentColor = Color(0xFF150915),
                disabledContainerColor = Color(0xFF473640),
                disabledContentColor = Color(0xFFFFE1DC)
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(text, fontSize = 17.sp, lineHeight = 24.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        if (!explanation.isNullOrBlank()) {
            Text(explanation, color = M10Faint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SirrSection(
    number: String,
    title: String,
    hint: String? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = Color(0xFF523046),
            border = BorderStroke(1.dp, Color(0xFF9F6882))
        ) {
            Text(number, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                color = M10Cream, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!hint.isNullOrBlank()) {
                Text(hint, color = M10Faint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SirrOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color(0xFF7D255C) else Color(0xE91C1227),
        border = BorderStroke(if (selected) 1.7.dp else 1.dp,
            if (selected) Color(0xFFFFA6D1) else Color(0xFF74536A))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, color = Color.White,
                fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = M10Faint, fontSize = 12.sp,
                    lineHeight = 18.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun SirrConsent(
    name: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (checked) Color(0xFF30283A) else M10Panel,
        border = BorderStroke(1.dp,
            if (checked) Color(0xFFB68AA5) else Color(0xFF513B4B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onChange)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(if (checked) "وافق على المشاركة" else "لسه مستنيين موافقته",
                    color = M10Faint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A distinctive illustrated scene, not the same pink rectangle on every page. */
@Composable
internal fun SirrIntro(
    overline: String,
    title: String,
    description: String,
    accent: Color = Color(0xFF9F4F79),
    motif: String = ""
) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(
                Color(0xFE1A101F), accent.copy(alpha = .29f), Color(0xFD100B19))),
                RoundedCornerShape(23.dp))
            .border(BorderStroke(1.dp, accent.copy(alpha = .56f)),
                RoundedCornerShape(23.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width * .18f, size.height * .43f)
            val r = size.minDimension * .24f
            drawCircle(accent.copy(alpha = .13f), r * 1.4f, c)
            when(motif) {
                "couple" -> {
                    val heart = Path().apply {
                        moveTo(c.x,c.y+r*.82f)
                        cubicTo(c.x-r*1.35f,c.y+r*.02f,c.x-r*.95f,c.y-r*.94f,c.x,c.y-r*.27f)
                        cubicTo(c.x+r*.95f,c.y-r*.94f,c.x+r*1.35f,c.y+r*.02f,c.x,c.y+r*.82f)
                        close()
                    }
                    drawPath(heart, Color(0xFFFFA2C8).copy(alpha=.65f))
                }
                "party" -> {
                    for(i in 0..2) {
                        val cx=c.x+(i-1)*r*.53f
                        drawCircle(Color(0xFFE4C0FF).copy(alpha=.82f),
                            r*.19f, Offset(cx,c.y-r*.34f+(i%2)*r*.08f))
                        drawRoundRect(Color(0xFF9C7CDE).copy(alpha=.71f),
                            topLeft=Offset(cx-r*.19f,c.y), size=Size(r*.38f,r*.65f),
                            cornerRadius=CornerRadius(r*.15f))
                    }
                }
                "dice" -> {
                    drawRoundRect(
                        Color(0xFFE9A383).copy(alpha=.49f),
                        topLeft=Offset(c.x-r*.68f,c.y-r*.68f),
                        size=Size(r*1.35f,r*1.35f),
                        cornerRadius=CornerRadius(r*.26f))
                    for ((dx,dy) in listOf(-.32f to -.32f, .3f to .3f, -.3f to .32f)) {
                        drawCircle(Color(0xFFFFEACA).copy(alpha=.92f),
                            radius=r*.07f,center=Offset(c.x+r*dx,c.y+r*dy))
                    }
                }
                "quiz" -> {
                    for(i in 1..3) {
                        drawCircle(Color(0xFFB5B3FA).copy(alpha=.57f/i),
                            radius=r*(.4f+i*.28f),center=c,
                            style=Stroke(width=2.dp.toPx()))
                    }
                    drawCircle(Color(0xFFFDDAFA),r*.085f,
                        Offset(c.x+r*.44f,c.y-r*.35f))
                }
                "whispers" -> {
                    val x=c.x-r*.88f; val y=c.y-r*.48f
                    drawRoundRect(Color(0xFFFA9BCA).copy(alpha=.66f),
                        Offset(x,y),Size(r*1.75f,r*1.1f),
                        CornerRadius(r*.13f))
                    val flap=Path().apply {
                        moveTo(x,y)
                        lineTo(c.x,c.y+r*.17f)
                        lineTo(x+r*1.75f,y)
                    }
                    drawPath(flap,Color(0xFF32162C),style=Stroke(width=2.dp.toPx()))
                }
                "scratch" -> {
                    for(i in 0..2) {
                        val shift=i*r*.13f
                        drawRoundRect(
                            Color(0xFFFFD3A2).copy(alpha=.64f-(i*.16f)),
                            Offset(c.x-r*.63f+shift,c.y-r*.77f+shift),
                            Size(r*1.25f,r*1.48f),
                            CornerRadius(r*.11f),
                            style=Stroke(width=3.dp.toPx()))
                    }
                }
                else -> {
                    for(i in 1..3) {
                        drawCircle(Color(0xFFFFD8B0).copy(alpha=.28f/i),
                            r*(.43f+i*.22f),c,
                            style=Stroke(width=2.dp.toPx()))
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal=19.dp, vertical=20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(overline.uppercase(), color=Color(0xFFFFD1A6),
                style=MaterialTheme.typography.labelMedium,
                fontWeight=FontWeight.Bold)
            Text(title, color=Color.White,
                style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.ExtraBold)
            Text(description, color=Color(0xFFF1D8E7),
                style=MaterialTheme.typography.bodyMedium)
        }
    }
}
