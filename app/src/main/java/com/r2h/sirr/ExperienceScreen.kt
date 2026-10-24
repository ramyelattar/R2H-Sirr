package com.r2h.sirr

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.r2h.sirr.core.*

@Composable
internal fun ExperienceScreen(group: Boolean) {
    var roundCount by remember(group) { mutableIntStateOf(if (group) 10 else 6) }
    var variety by remember { mutableStateOf(ChallengeVariety.ONE_LEVEL) }
    var avoidRepeat by remember { mutableStateOf(true) }
    var seats by remember { mutableIntStateOf(2) }
    var intensity by remember { mutableStateOf(Intensity.SOFT) }
    val names = remember { mutableStateListOf("إنت", "اللي معاك", "صاحبكم ٣", "صاحبكم ٤") }
    val consent = remember { mutableStateListOf(false, false, false, false) }
    var game by remember { mutableStateOf<LocalGame?>(null) }
    var reply by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    var mediaOpen by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }
    val effects = rememberGameEffects()
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    val storageName = if (group) "custom-party" else "custom-couple"
    val historyName = if (group) "recent-party" else "recent-couple"
    var customText by remember(group) { mutableStateOf(vault.readPrivate(storageName).orEmpty()) }
    var customStatus by remember { mutableStateOf("") }
    val customEntries = remember(customText) { customText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList() }
    val customValid = customEntries.size <= 60 && customEntries.all { it.length in 6..200 } &&
        customEntries.distinct().size == customEntries.size

    Page(
        if (group) "اللّمة ليها حكاية" else "بينك وبين اللي معاك",
        if (group) "اختاروا المود، واللعبة هتبدأ." else "شوية أسئلة ومفاجآت… براحتكم ومن غير ضغط."
    ) {
        if (game == null) {
            Text("مود الليلة", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Intensity.entries.filter { it != Intensity.CUSTOM }.forEach { level ->
                    FilterChip(selected = intensity == level, onClick = { intensity = level },
                        label = { Text(level.arabic) })
                }
            }
            Text("قد إيه عندكم وقت؟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6, 10, 12, 18).forEach { count ->
                    FilterChip(selected = roundCount == count, onClick = { roundCount = count },
                        label = { Text("${count} جولات") })
                }
            }
            if (group) {
                Text("عدد اللي بيلعبوا: ${seats}", style = MaterialTheme.typography.titleMedium)
                Slider(value = seats.toFloat(), onValueChange = { seats = it.toInt().coerceIn(2,4) },
                    valueRange = 2f..4f, steps = 1)
            }
            OutlinedButton(
                onClick = { advanced = !advanced },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (advanced) "اقفل الاختيارات الإضافية" else "عايز تغيّر الأسامي أو تزود التحديات؟")
            }
            if (advanced) {
                HorizontalDivider()
                Text("خلّي المفاجآت أكتر", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ChallengeVariety.entries.forEach { option ->
                    FilterChip(selected = variety == option, onClick = { variety = option },
                        label = { Text(option.arabic) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(avoidRepeat, { avoidRepeat = it })
                    Text("بلاش نفس تحديات المرة اللي فاتت")
                }
                OutlinedButton(onClick = { intensity = Intensity.CUSTOM }, modifier = Modifier.fillMaxWidth()) {
                    Text("اكتب تحديات من عندك")
                }
                if (intensity == Intensity.CUSTOM) {
                    OutlinedTextField(
                        value = customText, onValueChange = { customText = it.take(12_000) },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8,
                        label = { Text("كل تحدي في سطر") }
                    )
                    Text("${customEntries.size} تحدي — من 6 لـ 200 حرف لكل واحد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!customValid) Text("في تحديات متكررة أو أطول من المسموح", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = customValid, onClick = {
                            runCatching { vault.writePrivate(storageName, customText) }
                                .onSuccess { customStatus = "اتحفظوا على موبايلك بشكل مشفّر" }
                                .onFailure { customStatus = "معرفناش نحفظ التحديات" }
                        }) { Text("احفظهم") }
                        TextButton(onClick = { vault.deletePrivate(storageName); customText = ""; customStatus = "اتمسحوا" }) {
                            Text("امسحهم")
                        }
                    }
                    if (customStatus.isNotEmpty()) Text(customStatus)
                }
                Text("الأسماء (اختياري)", style = MaterialTheme.typography.titleMedium)
                repeat(seats) { i ->
                    OutlinedTextField(
                        value = names[i], onValueChange = { names[i] = it.take(32) },
                        label = { Text("اللاعب ${i + 1}") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                EffectPreferencesPanel(effects)
            }
            HorizontalDivider()
            Text("كله بموافقتكم", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("كل واحد يوافق بنفسه قبل ما تبدأوا. وتقدروا تعدّوا أي تحدي أو توقفوا في أي وقت.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            repeat(seats) { i ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent[i], onCheckedChange = { consent[i] = it })
                    Text("${names[i]} موافق يلعب", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = {
                    formError = ""
                    val recent = if (avoidRepeat) vault.readPrivate(historyName)
                        ?.lineSequence()?.filter { it.isNotBlank() }?.toSet().orEmpty() else emptySet()
                    runCatching {
                        SessionFactory.create(
                            names.take(seats), intensity, group,
                            customPrompts = if (intensity == Intensity.CUSTOM) customEntries else emptyList(),
                            roundCount = roundCount, variety = variety, recentlySeen = recent
                        )
                    }.onSuccess { newGame ->
                        game = newGame
                        runCatching { vault.writePrivate(historyName, newGame.turns.joinToString("\n") { it.prompt.text }) }
                        reply = ""
                    }.onFailure { formError = "حصلت مشكلة في تجهيز الجولة. جرّب اختيار مود تاني." }
                },
                enabled = (intensity != Intensity.CUSTOM || (customValid && customEntries.isNotEmpty())) &&
                    (0 until seats).all { consent[it] && names[it].isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp)
            ) { Text("يلا نبدأ اللعبة", fontWeight = FontWeight.Bold) }
            if (formError.isNotEmpty()) Text(formError, color = MaterialTheme.colorScheme.error)
        } else {
            val current = game!!
            if (!current.finished) {
                val turn = current.turns[current.round]
                LaunchedEffect(current.round) { effects.cue(GameCue.ROUND) }
                LinearProgressIndicator(
                    progress = { current.round.toFloat() / current.turns.size },
                    modifier = Modifier.fillMaxWidth()
                )
                RoundChapter(current.round,current.turns.size)
                Text("الدور على ${current.playerNames[turn.actorIndex]}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                CinematicChallenge(turn.prompt.text, effects)
                OutlinedButton(
                    onClick = { mediaOpen = !mediaOpen },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (mediaOpen) "اقفل الإضافات" else "عايز تكتب رد أو تضيف صورة؟") }
                if (mediaOpen) {
                    OutlinedTextField(reply, { reply = it.take(1000) },
                        label = { Text("رد خاص… لو حابب") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                    LocalResponseMedia(current.round)
                }
                Button(
                    onClick = { effects.cue(GameCue.SUCCESS); game = current.resolve(completed = true); reply = ""; mediaOpen = false },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp)
                ) { Text("خلصت التحدي ✦", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { effects.cue(GameCue.SKIP); game = current.resolve(completed = false); reply = ""; mediaOpen = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("مش دلوقتي… عَدّي") }
                TextButton(onClick = { game = current.leave(); reply = "" }) { Text("وقّف اللعبة") }
            } else {
                Text(if (current.ended) "وقفنا هنا… براحتكم" else "كده الليلة خلصت ✦",
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("المهم إن الوقت كان على مزاجكم.", style = MaterialTheme.typography.bodyLarge)
                current.playerNames.forEachIndexed { index, name ->
                    Text("${name} — ${current.scores[index]} تحدي اتعمل، ${current.skipped[index]} اتعدّى")
                }
                Button(onClick = {
                    game = null
                    consent.indices.forEach { consent[it] = false }
                }, modifier = Modifier.fillMaxWidth()) { Text("نبدأ واحدة جديدة؟") }
            }
        }
    }
}
