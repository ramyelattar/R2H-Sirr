package com.r2h.sirr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
    var reply by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    var mediaOpen by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }
    val effects = rememberGameEffects()
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    val gameName = if (group) "game-party" else "game-couple"
    val replyName = if (group) "replies-party" else "replies-couple"
    // Sensitive randomized challenges are stored encrypted, never in saved-instance-state.
    var game by remember(group) { mutableStateOf(LocalSessionStore.load(vault, gameName, group)) }
    // Save each game transition synchronously; a delayed Compose effect can be lost
    // if Android destroys the Activity immediately after a user action.
    var replyStatus by remember { mutableStateOf("") }
    LaunchedEffect(game?.round) {
        reply = game?.takeIf { !it.finished }?.let {
            LocalSessionStore.readReply(vault, replyName, it.round)
        }.orEmpty()
        replyStatus = ""
    }
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
            SirrIntro(
                overline = if (group) "من اتنين لأربعة" else "حكاية لشخصين",
                title = if (group) "كل واحد ليه دوره." else "خلي الليلة مختلفة.",
                description = if (group)
                    "اختاروا المود وعدد الجولات. اللعبة هتوزّع الأدوار عليكم واحدة واحدة."
                else "مود واحد، جولات قصيرة، وتحدي يتكشف لما تبقوا جاهزين.",
                accent = if (group) Color(0xFF605080) else Color(0xFFA04A74),
                motif = if (group) "party" else "couple"
            )
            SirrSection("01", "اختاروا المود", "من كلام خفيف لجرأة على مزاجكم")
            Intensity.entries.filter { it != Intensity.CUSTOM }.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { level ->
                        SirrOption(title = level.arabic, selected = intensity == level,
                            onClick = { intensity = level }, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            SirrSection("02", "اختاروا عدد الجولات", "الاختيار ده ينفع يتكرر في ليلة تانية")
            listOf(6, 10, 12, 18).chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { count ->
                        SirrOption(title = "$count جولات", selected = roundCount == count,
                            onClick = { roundCount = count }, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (group) {
                Text("كام واحد هيلعب؟", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (2..4).forEach { count ->
                        SirrOption(title = "$count لاعبين", selected = seats == count,
                            onClick = { seats = count }, modifier = Modifier.weight(1f))
                    }
                }
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
            HorizontalDivider(color = Color(0xFF5C4153))
            SirrSection("03", "الكل موافق؟",
                "كل واحد يوافق بنفسه. تخطي التحدي أو وقف اللعبة متاح في أي وقت.")
            repeat(seats) { i ->
                SirrConsent(name = names[i], checked = consent[i],
                    onChange = { consent[i] = it })
            }
            val waiting = (0 until seats).filter { !consent[it] }
            val invalidNames = (0 until seats).any { names[it].isBlank() }
            val invalidCustom = intensity == Intensity.CUSTOM &&
                (!customValid || customEntries.isEmpty())
            val startHint = when {
                invalidNames -> "اكتبوا اسم لكل لاعب من الاختيارات الإضافية"
                invalidCustom -> "كمّل التحديات المخصوصة الأول"
                waiting.isNotEmpty() -> "مستنيين موافقة ${waiting.size} من اللاعبين"
                else -> "جاهزين؟ السر الأول مستنيكم."
            }
            SirrAction(
                text = "افتح أول سر  ✦",
                explanation = startHint,
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
                        runCatching {
                            LocalSessionStore.save(vault, gameName, newGame)
                            vault.deletePrivate(replyName)
                        }.onSuccess {
                            game = newGame
                            runCatching { vault.writePrivate(historyName, newGame.turns.joinToString("\n") { it.prompt.text }) }
                            reply = ""
                        }.onFailure { formError = "تعذر حفظ الجولة بأمان؛ لم تبدأ اللعبة. تأكد إن مساحة التخزين كافية." }
                    }.onFailure { formError = "حصلت مشكلة في تجهيز الجولة. جرّب اختيار مود تاني." }
                },
                enabled = !invalidNames && !invalidCustom && waiting.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (formError.isNotEmpty()) Text(formError, color = MaterialTheme.colorScheme.error)
        } else {
            val current = game!!
            if (!current.finished) {
                val turn = current.turns[current.round]
                var isRevealed by remember(current.round) { mutableStateOf(false) }
                LaunchedEffect(current.round) { effects.cue(GameCue.ROUND) }
                SirrSection("${current.round + 1}", "جولة جديدة · السر مستنيكم",
                    "لو مش مناسب، عدّوه من غير أي حسابات.")
                LinearProgressIndicator(
                    progress = { current.round.toFloat() / current.turns.size },
                    modifier = Modifier.fillMaxWidth()
                )
                RoundChapter(current.round,current.turns.size)
                Text("الدور على ${current.playerNames[turn.actorIndex]}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!isRevealed) {
                    SealedChallengeCard(onReveal = {
                        isRevealed = true
                        effects.cue(GameCue.REVEAL)
                    })
                }
                AnimatedVisibility(
                    visible = isRevealed,
                    enter = if (effects.animate)
                        fadeIn(animationSpec = tween(320)) +
                            scaleIn(initialScale = .94f, animationSpec = tween(420))
                    else EnterTransition.None
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        OutlinedButton(onClick = {
                            runCatching { LocalSessionStore.writeReply(vault, replyName, current.round, reply) }
                                .onSuccess { replyStatus = "ردك اتحفظ مشفّر على الموبايل" }
                                .onFailure { replyStatus = "معرفناش نحفظ الرد؛ حاول تاني قبل ما تكمل" }
                        }, enabled = reply.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            Text("احفظ ردك عندك")
                        }
                        if (replyStatus.isNotBlank()) Text(replyStatus)
                    }
                    }
                }
                fun finishRound(completed:Boolean) {
                    if (reply.isNotBlank() && runCatching {
                        LocalSessionStore.writeReply(vault, replyName, current.round, reply)
                    }.isFailure) {
                        replyStatus = "فشل حفظ الرد؛ النص لسه موجود علشان متفقدوش"
                        return
                    }
                    val next = current.resolve(completed = completed)
                    runCatching { LocalSessionStore.save(vault, gameName, next) }
                        .onSuccess {
                            effects.cue(if (completed) GameCue.SUCCESS else GameCue.SKIP)
                            game = next
                            reply = ""; mediaOpen = false; replyStatus = ""
                        }.onFailure {
                            replyStatus = "الجولة متقدمتش لأن حفظها فشل؛ ردك لسه موجود"
                        }
                }
                if (isRevealed) {
                    SirrAction(
                        text = "التحدي اتعمل  ✦",
                        onClick = { finishRound(true) }
                    )
                }
                OutlinedButton(
                    onClick = { finishRound(false) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("مش مناسب؟ عَدّي من غير إحراج") }
                TextButton(onClick = {
                    runCatching { LocalSessionStore.save(vault, gameName, current.leave()) }
                        .onSuccess { game = current.leave(); reply = "" }
                        .onFailure { replyStatus = "تعذر حفظ إيقاف اللعبة، حاول تاني" }
                }) { Text("وقّف اللعبة") }
                if (replyStatus.isNotBlank() && !mediaOpen) {
                    Text(replyStatus, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(if (current.ended) "وقفنا هنا… براحتكم" else "كده الليلة خلصت ✦",
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("المهم إن الوقت كان على مزاجكم.", style = MaterialTheme.typography.bodyLarge)
                current.playerNames.forEachIndexed { index, name ->
                    Text("${name} — ${current.scores[index]} تحدي اتعمل، ${current.skipped[index]} اتعدّى")
                }
                val pastReplies = remember(current, replyName) {
                    current.turns.indices.mapNotNull { index ->
                        LocalSessionStore.readReply(vault, replyName, index)?.let { index to it }
                    }
                }
                if (pastReplies.isNotEmpty()) {
                    Text("ردودكم الخاصة — محفوظة على الموبايل بس",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    pastReplies.forEach { (index, text) ->
                        Text("الجولة ${index + 1}: $text", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(onClick = {
                    runCatching {
                        vault.deletePrivate(gameName)
                        vault.deletePrivate(replyName)
                    }.onSuccess {
                        game = null
                        consent.indices.forEach { consent[it] = false }
                        formError = ""
                    }.onFailure { formError = "تعذر مسح الجولة القديمة؛ حاول تاني" }
                }, modifier = Modifier.fillMaxWidth()) { Text("نبدأ واحدة جديدة؟") }
                if (formError.isNotBlank()) Text(formError, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
