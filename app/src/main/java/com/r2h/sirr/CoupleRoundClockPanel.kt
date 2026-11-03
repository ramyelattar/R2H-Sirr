package com.r2h.sirr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.LocalGame

/**
 * Count-down controls for the active local couple turn.
 * Every transition is persisted synchronously before the screen changes.
 */
@Composable
internal fun CoupleRoundClockPanel(
    game: LocalGame,
    bothConsent: Boolean,
    persist: (LocalGame) -> Boolean,
    onCompleted: () -> Unit
) {
    if (game.competition == null || game.finished) return
    val clock = game.competition.roundClock
    var tickNow by remember(game.round) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(game.round, clock.runningSinceMillis) {
        tickNow = System.currentTimeMillis()
        if (clock.running) {
            while (true) {
                delay(1_000L)
                tickNow = System.currentTimeMillis()
            }
        }
    }
    val duration = game.turns[game.round].prompt.durationSeconds
    val remaining = if (clock.started) clock.remainingSeconds(tickNow) else duration
    val minutes = remaining / 60
    val seconds = remaining % 60
    val formatted = "%02d:%02d".format(java.util.Locale.US, minutes, seconds)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("مؤقت التحدي", style = MaterialTheme.typography.titleMedium)
        Text(formatted, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("الوقت الفعلي للتحدي: ${duration / 60} دقيقة و${duration % 60} ثانية. الوقف المؤقت مش بيتحسب.")
        when {
            !clock.started -> Button(
                enabled = bothConsent,
                onClick = { persist(game.startRoundClock(System.currentTimeMillis())) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ابدأ وقت التحدي") }
            remaining == 0 -> Text("وقت التحدي خلص. بعد التأكد من التنفيذ وموافقة الطرفين، سجّلوا النتيجة.")
            clock.running -> OutlinedButton(
                onClick = { persist(game.pauseRoundClock(System.currentTimeMillis())) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("وقف المؤقت مؤقتًا") }
            else -> Button(
                enabled = bothConsent,
                onClick = { persist(game.startRoundClock(System.currentTimeMillis())) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("كمّل المؤقت") }
        }
        if (!bothConsent) Text("لابد من موافقة الطرفين قبل تشغيل المؤقت أو استكماله.")
        SirrAction(
            text = "التحدي اتعمل — +10 نقاط",
            enabled = bothConsent && clock.started && remaining == 0,
            explanation = when {
                !bothConsent -> "محتاج موافقة الطرفين"
                remaining != 0 -> "كمّلوا وقت التحدي قبل احتساب النقاط"
                else -> null
            },
            onClick = onCompleted
        )
    }
}
