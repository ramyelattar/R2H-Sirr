package com.r2h.sirr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Four individually illustrated entry points. No remote image fetch or stock icons. */
internal enum class DiscoveryMotif { DICE, QUIZ, SCRATCH, WHISPER }

@Composable
internal fun DiscoveryTile(
    title: String, subtitle: String,
    motif: DiscoveryMotif, modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = when (motif) {
        DiscoveryMotif.DICE -> Color(0xFFEBC296)
        DiscoveryMotif.QUIZ -> Color(0xFFDDABCB)
        DiscoveryMotif.SCRATCH -> Color(0xFFE0C2A1)
        DiscoveryMotif.WHISPER -> Color(0xFFF1A5B6)
    }
    val icon = when (motif) {
        DiscoveryMotif.DICE -> R2HGameArt.DICE
        DiscoveryMotif.QUIZ -> R2HGameArt.QUIZ
        DiscoveryMotif.SCRATCH -> R2HGameArt.SECRET
        DiscoveryMotif.WHISPER -> R2HGameArt.WHISPERS
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .r2hGlass(accent = accent, corner = 26.dp, lift = 14.dp)
            .semantics { contentDescription = title + ". " + subtitle },
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 192.dp)) {
            Canvas(Modifier.matchParentSize()) {
                // Engraved side light is decorative only: no text ever overlaps art.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = .18f), Color.Transparent),
                        center = Offset(size.width * .23f, size.height * .30f),
                        radius = size.minDimension * .46f
                    ),
                    radius = size.minDimension * .46f,
                    center = Offset(size.width * .23f, size.height * .30f)
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().height(97.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    R2HArtwork(icon, accent, Modifier.size(93.dp))
                    R2HCardChevron(Modifier.padding(top = 9.dp).size(19.dp))
                }
                Text(
                    title, color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 2
                )
                Text(
                    subtitle, color = Color(0xFFFFE0EE),
                    style = MaterialTheme.typography.bodySmall, maxLines = 2
                )
            }
        }
    }
}

/** Seven non-identical illustrated doors into the Egyptian quiz chapters. */
@Composable
internal fun QuizTopicTile(
    number: Int,
    title: String,
    subtitle: String = "25 سؤال… وكل إجابة وراها حكاية",
    onClick: () -> Unit
) {
    val hues = listOf(
        Color(0xFFE5BC9A), Color(0xFFF1A9BD), Color(0xFFBDADEB),
        Color(0xFFE3B7CD), Color(0xFFE5D7A5), Color(0xFFB9D6C9),
        Color(0xFFECC5A2)
    )
    val icons = listOf(
        R2HGameArt.QUIZ, R2HGameArt.COUPLE, R2HGameArt.PARTY,
        R2HGameArt.WHISPERS, R2HGameArt.SECRET, R2HGameArt.ROOM,
        R2HGameArt.DICE
    )
    val index = (number - 1).coerceIn(0, 6)
    val accent = hues[index]
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .r2hGlass(accent = accent, corner = 22.dp, lift = 13.dp)
            .semantics {
                contentDescription = "الموضوع " + number + ": " + title + ". " + subtitle
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 99.dp)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            R2HArtwork(icons[index], accent, Modifier.size(67.dp))
            Column(
                Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    subtitle, color = Color(0xFFFFDBEB),
                    style = MaterialTheme.typography.bodySmall, maxLines = 2
                )
            }
            R2HCardChevron(Modifier.size(20.dp), accent)
        }
    }
}
