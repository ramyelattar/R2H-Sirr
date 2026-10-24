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
    val accent = when(motif) {
        DiscoveryMotif.DICE -> Color(0xFFEBC296)
        DiscoveryMotif.QUIZ -> Color(0xFFDDABCB)
        DiscoveryMotif.SCRATCH -> Color(0xFFE0C2A1)
        DiscoveryMotif.WHISPER -> Color(0xFFF1A5B6)
    }
    val base = when(motif) {
        DiscoveryMotif.DICE -> Color(0xFF412836)
        DiscoveryMotif.QUIZ -> Color(0xFF322741)
        DiscoveryMotif.SCRATCH -> Color(0xFF3B2831)
        DiscoveryMotif.WHISPER -> Color(0xFF452335)
    }
    Surface(
        onClick = onClick, modifier = modifier.semantics { contentDescription = "$title. $subtitle" },
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, accent.copy(alpha=.25f)),
        color = base
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min=198.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val cx=size.width*.74f
                val cy=size.height*.27f
                val r=min(size.width,size.height)*.26f
                drawCircle(accent.copy(alpha=.06f),r*1.7f,Offset(cx,cy))
                drawCircle(accent.copy(alpha=.13f),r*1.12f,Offset(cx,cy),
                    style=Stroke(width=1.2.dp.toPx()))
                when(motif){
                    DiscoveryMotif.DICE -> {
                        val p=Path().apply {
                            moveTo(cx-r*.75f,cy-r*.45f)
                            lineTo(cx+r*.42f,cy-r*.60f)
                            lineTo(cx+r*.83f,cy-r*.05f)
                            lineTo(cx+r*.60f,cy+r*.82f)
                            lineTo(cx-r*.55f,cy+r*.61f)
                            close()
                        }
                        drawPath(p, Brush.linearGradient(listOf(accent.copy(alpha=.39f),Color(0xFF5F354A))))
                        listOf(-.27f to -.18f,.12f to .0f,.17f to .43f).forEach{(x,y)->
                            drawCircle(Color(0xFFFFE7C8),3.0.dp.toPx(),Offset(cx+r*x,cy+r*y))
                        }
                    }
                    DiscoveryMotif.QUIZ -> {
                        for(i in 0..2) {
                            drawCircle(accent.copy(alpha=.30f-i*.05f),r*(.72f+i*.30f),
                                Offset(cx,cy),style=Stroke(width=1.8.dp.toPx()))
                        }
                        drawCircle(accent.copy(alpha=.75f),4.5.dp.toPx(),
                            Offset(cx+r*.38f,cy-r*.39f))
                        drawCircle(accent.copy(alpha=.75f),3.2.dp.toPx(),
                            Offset(cx-r*.50f,cy+r*.25f))
                    }
                    DiscoveryMotif.SCRATCH -> {
                        val top=cy-r*.77f
                        val left=cx-r*.93f
                        drawRoundRect(accent.copy(alpha=.18f),
                            topLeft=Offset(left,top),
                            size=androidx.compose.ui.geometry.Size(r*1.75f,r*1.39f),
                            cornerRadius=CornerRadius(14.dp.toPx()))
                        for(i in 0..6) {
                            val y=top+r*(.18f+i*.16f)
                            drawLine(accent.copy(alpha=.54f-(i%2)*.18f),
                                Offset(left+r*.18f,y),
                                Offset(left+r*(1.30f+(i%3)*.11f),y-r*.07f),
                                strokeWidth=2.3.dp.toPx())
                        }
                    }
                    DiscoveryMotif.WHISPER -> {
                        val p=Path().apply {
                            moveTo(cx-r*.90f,cy-r*.47f)
                            lineTo(cx+r*.85f,cy-r*.47f)
                            lineTo(cx+r*.85f,cy+r*.55f)
                            lineTo(cx-r*.90f,cy+r*.55f)
                            close()
                        }
                        drawPath(p,accent.copy(alpha=.27f))
                        drawLine(accent,Offset(cx-r*.90f,cy-r*.47f),
                            Offset(cx-r*.06f,cy+r*.17f),1.5.dp.toPx())
                        drawLine(accent,Offset(cx+r*.85f,cy-r*.47f),
                            Offset(cx-r*.06f,cy+r*.17f),1.5.dp.toPx())
                        drawCircle(accent.copy(alpha=.87f),4.dp.toPx(),Offset(cx+r*.68f,cy-r*.68f))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().heightIn(min=198.dp).padding(18.dp),
                verticalArrangement=Arrangement.Bottom
            ){
                Text(title,color=Color.White,style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Bold,maxLines=2)
                Spacer(Modifier.height(3.dp))
                Text(subtitle,color=Color(0xFFE5CED6),style=MaterialTheme.typography.bodySmall,
                    maxLines=2)
                Spacer(Modifier.height(10.dp))
                Text("اكتشف  ←",color=accent,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
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
    val accent = hues[(number - 1).coerceIn(0, hues.lastIndex)]
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = "الموضوع $number: $title. $subtitle" },
        color = Color(0xFF251923),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha=.30f))
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min=122.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width*.16f,size.height*.39f)
                val base = size.minDimension*.17f
                drawCircle(accent.copy(alpha=.08f),base*2.5f,center)
                for (i in 1..3) {
                    drawCircle(accent.copy(alpha=.46f / i),base*(.65f+.44f*i),center,
                        style=Stroke(width=1.8.dp.toPx()))
                }
                val end = Offset(size.width*.92f,size.height*.77f)
                drawCircle(accent.copy(alpha=.38f),2.5.dp.toPx(),end)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=22.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(57.dp),contentAlignment=Alignment.Center){
                    Text(number.toString().padStart(2,'0'), color=accent,
                        fontSize=27.sp, fontWeight=FontWeight.Bold)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text(title,color=Color.White,style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold)
                    Text(subtitle,color=Color(0xFFD8C3CD),style=MaterialTheme.typography.bodySmall)
                }
                Text("‹",color=accent,fontSize=28.sp)
            }
        }
    }
}
