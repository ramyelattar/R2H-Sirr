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
        R2HNeonAction(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
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
            color = Color.Transparent,
            modifier = Modifier.r2hGlass(
                accent = Color(0xFFFFA9DA), corner = 13.dp, lift = 5.dp
            )
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
        modifier = modifier.heightIn(min = 60.dp)
            .r2hGlass(
                accent = if (selected) Color(0xFFFFA6D1) else Color(0xFFA994C4),
                corner = 17.dp, selected = selected, lift = if (selected) 10.dp else 5.dp
            ),
        shape = RoundedCornerShape(17.dp),
        color = Color.Transparent
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
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
            .r2hGlass(
                accent = if (checked) Color(0xFFFFA8D2) else Color(0xFFB094C4),
                corner = 18.dp, selected = checked, lift = 7.dp
            )
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
    val art = when (motif) {
        "couple" -> R2HGameArt.COUPLE
        "party" -> R2HGameArt.PARTY
        "dice" -> R2HGameArt.DICE
        "quiz" -> R2HGameArt.QUIZ
        "whispers" -> R2HGameArt.WHISPERS
        "scratch" -> R2HGameArt.SECRET
        else -> R2HGameArt.SECRET
    }
    Surface(
        modifier = Modifier.fillMaxWidth()
            .r2hGlass(accent = accent, corner = 24.dp, lift = 17.dp),
        shape = RoundedCornerShape(24.dp), color = Color.Transparent
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    overline, color = Color(0xFFFFD8B5),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    title, color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    description, color = Color(0xFFFFDBEA),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            R2HArtwork(art, accent, Modifier.size(91.dp))
        }
    }
}
