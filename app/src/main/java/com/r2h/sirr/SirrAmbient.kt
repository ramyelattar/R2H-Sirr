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

/**
 * A single shared background for every route, including splash, age gate,
 * home, both Dice modes, all game screens, settings and privacy.
 *
 * The user-provided video is muted and packaged locally. Honor the app's
 * cinematic preference AND the system animator scale; when disabled, show the
 * static poster from the same video instead of running a hidden decoder.
 */
@Composable
internal fun SirrAmbientBackground(scene: String, allowMotion: Boolean) {
    val context = LocalContext.current
    val motionAllowed = remember(context, allowMotion) {
        allowMotion && runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }.getOrDefault(true)
    }
    R2HVideoWallpaper(motionAllowed)
}

/** This is a Compose entrance, distinct from Android's short operating-system splash. */
@Composable
internal fun SirrCinematicSplash(
    allowMotion: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val canAnimate = remember(allowMotion) {
        allowMotion && runCatching {
            Settings.Global.getFloat(context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
    val pulse = if (canAnimate) {
        val movement = rememberInfiniteTransition(label = "sirr-entrance")
        val animatedPulse by movement.animateFloat(
            initialValue = .90f, targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                tween(1150, easing = LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
            label = "splash-heartbeat"
        )
        animatedPulse
    } else 1f
    var show by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(if (canAnimate) 2280 else 0, easing = LinearEasing),
        label = "splash-progress"
    )
    LaunchedEffect(canAnimate) {
        show = true
        delay(if (canAnimate) 2450L else 850L)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF07050B).copy(alpha = .48f)),
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
                R2HBrandLogo(
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = show,
                enter = if (canAnimate)
                    androidx.compose.animation.fadeIn(tween(750)) +
                        androidx.compose.animation.scaleIn(initialScale = .86f,
                            animationSpec = tween(950))
                else androidx.compose.animation.EnterTransition.None
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
