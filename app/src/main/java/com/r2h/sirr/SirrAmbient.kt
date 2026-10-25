package com.r2h.sirr

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Each scene has its own palette, particle orbit and atmospheric light, not a static wallpaper. */
private fun sceneHue(scene: String): Color = when (scene) {
    "splash", "gate" -> Color(0xFFD4498F)
    "home" -> Color(0xFFBD3D78)
    "couple" -> Color(0xFFC34B8E)
    "party" -> Color(0xFF8A65CA)
    "dice" -> Color(0xFFE36E7F)
    "quiz" -> Color(0xFF897CD7)
    "scratch" -> Color(0xFFCF8C61)
    "whispers" -> Color(0xFFCA629C)
    "remote" -> Color(0xFF7283C0)
    "settings", "privacy" -> Color(0xFF9D729F)
    else -> Color(0xFFAE5787)
}

/** Respects Android's global animation scale and the app's own animation preference. */
@Composable
internal fun SirrAmbientBackground(scene: String, allowMotion: Boolean) {
    val context = LocalContext.current
    val animationsAllowed = remember(allowMotion) {
        allowMotion && runCatching {
            Settings.Global.getFloat(context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
    if (animationsAllowed) {
        val movement = rememberInfiniteTransition(label = "sirr-atmosphere")
        val phase by movement.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(32000, easing = LinearEasing)),
            label = "sirr-orbit"
        )
        SirrAmbientCanvas(scene, phase)
    } else {
        SirrAmbientCanvas(scene, 0.27f)
    }
}

@Composable
private fun SirrAmbientCanvas(scene: String, phase: Float) {
    val accent = sceneHue(scene)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF100A17), Color(0xFF08070C), Color(0xFF0C0810))))
        val width = size.width
        val height = size.height
        val unit = size.minDimension
        val theta = phase * 2f * PI.toFloat()
        val a = Offset(width * (.77f + sin(theta) * .14f),
            height * (.22f + cos(theta * .62f) * .11f))
        val b = Offset(width * (.19f + cos(theta * .67f) * .13f),
            height * (.73f + sin(theta) * .10f))
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = .18f), accent.copy(alpha = .065f),
                    Color.Transparent),
                center = a, radius = unit * 1.05f),
            radius = unit * 1.05f, center = a
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF7051AA).copy(alpha = .115f),
                    Color(0xFF452441).copy(alpha = .045f), Color.Transparent),
                center = b, radius = unit * .92f),
            radius = unit * .92f, center = b
        )
        // Soft, slow smoke ribbons. Drawn into the scene, never obscuring controls.
        val lift = sin(theta * .66f) * height * .055f
        val ribbon = Path().apply {
            moveTo(-width * .24f, height * .28f + lift)
            cubicTo(width * .22f, height * .16f + lift,
                width * .34f, height * .56f + lift,
                width * .78f, height * .36f + lift)
            cubicTo(width * .99f, height * .26f + lift,
                width * 1.14f, height * .28f + lift,
                width * 1.20f, height * .35f + lift)
            lineTo(width * 1.20f, height * .44f + lift)
            cubicTo(width * .85f, height * .33f + lift,
                width * .41f, height * .69f + lift,
                -width * .24f, height * .40f + lift)
            close()
        }
        drawPath(ribbon, Brush.linearGradient(listOf(
            Color.Transparent, accent.copy(alpha = .095f),
            Color(0xFFB067D4).copy(alpha = .125f), Color.Transparent)))
        for (i in 0 until 23) {
            val x = ((i * 73 + 11) % 101) / 101f
            val y = ((i * 47 + 29) % 97) / 97f
            val drift = (phase * (if (i % 2 == 0) .17f else -.11f))
            val px = ((x + drift + 1f) % 1f) * width
            val py = (y + sin(theta + i * .67f) * .018f) * height
            val dot = if (i % 5 == 0) 2.1.dp.toPx() else 1.1.dp.toPx()
            drawCircle(
                Color(0xFFFFC8E5).copy(alpha = .13f + .11f *
                    ((sin(theta + i * .42f) + 1f) / 2f)),
                radius = dot, center = Offset(px, py)
            )
        }
        val center = Offset(width * .83f, height * .40f)
        val orbit = unit * .48f
        for (i in 1..3) {
            drawCircle(accent.copy(alpha = .040f / i),
                radius = orbit * (i * .83f),
                center = center, style = Stroke(width = 1.dp.toPx()))
        }
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF09070B).copy(alpha = .06f),
            Color.Transparent,
            Color(0xFF09070B).copy(alpha = .38f))))
    }
}

/** This is a Compose entrance, distinct from Android's short operating-system splash. */
@Composable
internal fun SirrCinematicSplash(onFinished: () -> Unit) {
    val movement = rememberInfiniteTransition(label = "sirr-entrance")
    val pulse by movement.animateFloat(
        initialValue = .90f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(1150, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "splash-heartbeat"
    )
    var show by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(2280, easing = LinearEasing),
        label = "splash-progress"
    )
    LaunchedEffect(Unit) {
        show = true
        delay(2450L)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF07050B).copy(alpha = .72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier.size(222.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * .40f * pulse
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFD34893).copy(alpha = .24f),
                                Color(0xFF873F86).copy(alpha = .06f),
                                Color.Transparent),
                            center = center, radius = radius * 1.42f),
                        radius = radius * 1.42f, center = center
                    )
                    for (i in 1..3) {
                        drawCircle(Color(0xFFFFB4DA).copy(alpha = .48f / i),
                            radius = radius * (.58f + i * .25f),
                            center = center, style = Stroke(width = 1.3.dp.toPx()))
                    }
                }
                Image(
                    painter = painterResource(R.drawable.sirr_launcher_foreground),
                    contentDescription = "سِرّ، مفتاح الأسرار",
                    modifier = Modifier.size(192.dp)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = show,
                enter = androidx.compose.animation.fadeIn(tween(750)) +
                    androidx.compose.animation.scaleIn(initialScale = .86f,
                        animationSpec = tween(950))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سِرّ",
                        color = Color(0xFFFFDACA), fontSize = 47.sp,
                        fontWeight = FontWeight.Bold)
                    Text("مش كل الأسرار بتتقال بصوت عالي.",
                        color = Color(0xFFF6CEDF),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(.56f)
                    .height(3.dp).clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFFF89BC),
                trackColor = Color(0xFF35213C)
            )
            Text("كل حكاية ليها بداية…",
                color = Color(0xFFE8C2D8),
                style = MaterialTheme.typography.labelLarge)
        }
        Text("R2H  •  PRIVATE BY DESIGN",
            color = Color(0xFF947D94),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 38.dp))
    }
}
