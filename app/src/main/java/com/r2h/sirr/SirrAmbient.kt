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

/** Keep one premium wallpaper and a restrained atmosphere across every screen.
 * The wallpaper is an on-device resource, never fetched from the network.
 * If the image hasn't been installed yet, the existing dark fallback remains.
 */
private fun sceneHue(scene: String): Color = when (scene) {
    "splash", "gate", "home" -> Color(0xFFFF74BC)
    "couple", "whispers" -> Color(0xFFFF77BC)
    "party", "quiz" -> Color(0xFFB990F5)
    "dice", "scratch" -> Color(0xFFF2AEAD)
    else -> Color(0xFFD78EC9)
}

@Composable
internal fun SirrAmbientBackground(scene: String, allowMotion: Boolean) {
    val context = LocalContext.current
    val animationsAllowed = remember(context, allowMotion) {
        allowMotion && runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }.getOrDefault(true)
    }
    // One continuous phase for all routes: navigation won't restart the effect.
    if (animationsAllowed) {
        val transition = rememberInfiniteTransition(label = "r2h-wallpaper-glow")
        val phase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(36000, easing = LinearEasing)
            ),
            label = "r2h-slow-glow"
        )
        R2HWallpaperScene(scene, phase)
    } else {
        R2HWallpaperScene(scene, .25f)
    }
}

@Composable
private fun R2HWallpaperScene(scene: String, phase: Float) {
    val context = LocalContext.current
    // A dynamic lookup lets Android compile before the user's wallpaper is copied in.
    // Once drawable-nodpi/r2h_neon_wallpaper.webp exists, it is used automatically.
    val wallpaperId = remember(context) {
        context.resources.getIdentifier(
            "r2h_neon_wallpaper", "drawable", context.packageName
        )
    }
    Box(Modifier.fillMaxSize()) {
        if (wallpaperId != 0) {
            Image(
                painter = painterResource(wallpaperId),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(
                    Color(0xFF210B24), Color(0xFF0B0612), Color(0xFF210A22)
                )))
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val unit = size.minDimension
            val cycle = phase * 2f * PI.toFloat()
            // Darkens only the reading area, preserving the golden top R2H lettering.
            drawRect(Brush.verticalGradient(listOf(
                Color(0xFF09020C).copy(alpha = .04f),
                Color(0xFF0A0611).copy(alpha = .17f),
                Color(0xFF090410).copy(alpha = .25f),
                Color(0xFF06030A).copy(alpha = .32f)
            )))
            val accent = sceneHue(scene)
            // Very low-opacity lighting along the margins, not over controls.
            for (side in 0..1) {
                val px = if (side == 0) width * .04f else width * .96f
                val py = height * (
                    .46f + .11f * sin(cycle + side * 1.8f)
                )
                val center = Offset(px, py)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = .11f),
                            accent.copy(alpha = .035f),
                            Color.Transparent
                        ), center = center, radius = unit * .49f
                    ),
                    radius = unit * .49f, center = center
                )
            }
            // Sparse twinkling edge stars: no animated scaling of the image or logo.
            for (i in 0 until 30) {
                val left = i % 2 == 0
                val px = width * (if (left)
                    .015f + (i * 17 % 11) * .008f
                else .985f - (i * 13 % 11) * .008f)
                val py = height * (.10f + ((i * 47 + 19) % 83) / 100f)
                val twinkle = (sin(cycle + i * .77f) + 1f) * .5f
                drawCircle(
                    color = Color(0xFFFFDBEC).copy(alpha = .13f + twinkle * .27f),
                    radius = (if (i % 6 == 0) 1.9f else 1.05f).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
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
                Image(
                    painter = painterResource(R.drawable.r2h_play_foreground),
                    contentDescription = "R2H-Play",
                    modifier = Modifier.size(192.dp)
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
                    Text("R2H-Play",
                        color = Color(0xFFFFDACA), fontSize = 43.sp,
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
