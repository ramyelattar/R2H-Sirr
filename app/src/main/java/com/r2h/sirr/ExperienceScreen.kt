package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var durationMinutes by remember { mutableIntStateOf(30) }
    var rewardText by remember { mutableStateOf("") }
    var tieChallengeText by remember { mutableStateOf("") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val names = remember(group) {
        mutableStateListOf(if (group) "إنت" else "الزوج",
            if (group) "اللي معاك" else "الزوجة", "صاحبكم ٣", "صاحبكم ٤")
    }
    val consent = remember { mutableStateListOf(false, false, false, false) }
    fun resetStartConsents() { consent.indices.forEach { consent[it] = false } }
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
    LaunchedEffect(game?.competition?.startedAtMillis, game?.finished) {
        if (game?.competition != null && game?.finished == false) {
            while (true) {
                clockNow = System.currentTimeMillis()
                kotlinx.coroutines.delay(1_000L)
            }
        }
    }
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
    val customValid = customEntries.size <= 60 && customEntries.all { it.length in 6..500 } &&
        customEntries.distinct().size == customEntries.size

    val importChallenges = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val result = StringBuilder()
                    val buffer = CharArray(4_096)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        require(result.length + count <= 30_000) { "Challenge file is too large" }
                        result.append(buffer, 0, count)
                    }
                    result.toString()
                } ?: error("Cannot read text file")
            }.onSuccess { imported ->
                if (imported.length > 30_000) {
                    customStatus = "الملف كبير جدًا — الحد 30 ألف حرف"
                } else {
                    customText = imported
                    resetStartConsents()
                    customStatus = "تم استيراد التحديات. راجعها قبل الحفظ."
                }
            }.onFailure {
                customStatus = if (it is IllegalArgumentException) "الملف أكبر من الحد — 30 ألف حرف"
                    else "تعذر استيراد الملف. اختار ملف TXT بصيغة UTF-8."
            }
        }
    }
    val exportChallenges = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(customText)
                } ?: error("Cannot write text file")
            }.onSuccess { customStatus = "تم تصدير التحديات للملف اللي اخترته" }
                .onFailure { customStatus = "تعذر تصدير التحديات" }
        }
    }

    Page(
        if (group) "اللّمة ليها حكاية" else "تحديات الزوجين",
        if (group) "اختاروا المود، واللعبة هتبدأ." else "جولات خاصة للزوجين: قرب، اختيار، مفاجآت ومنافسة بنقاط."
    ) {
        if (game == null) {
            SirrIntro(
                overline = if (group) "من اتنين لأربعة" else "للزوج والزوجة",
                title = if (group) "كل واحد ليه دوره." else "ليلتكم فيها تحدي وفائز.",
                description = if (group)
                    "اختاروا المود وعدد الجولات. اللعبة هتوزّع الأدوار عليكم واحدة واحدة."
                else "اختاروا مستوى الجرأة والمكافأة، وبعدين الجولات هتتوزع بينكم بالتبادل.",
                accent = if (group) Color(0xFF605080) else Color(0xFFA04A74),
                motif = if (group) "party" else "couple"
            )
            SirrSection("01", "اختاروا المود", "من كلام خفيف لجرأة على مزاجكم")
            Intensity.entries.filter { it != Intensity.CUSTOM }.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { level ->
                        SirrOption(title = level.arabic, selected = intensity == level,
                            onClick = { intensity = level; resetStartConsents() }, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (!group) {
                OutlinedButton(
                    onClick = { intensity = Intensity.CUSTOM; advanced = true; resetStartConsents() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("العبوا بتحدياتكم الخاصة (TXT أو كتابة)") }
            }
            Text(
                when (intensity) {
                    Intensity.SOFT -> "المود ده أخف في المحتوى. لو مش مناسب، اختاروا غيره."
                    Intensity.CUSTOM -> "تحدياتكم الخاصة بتفضل على الموبايل. راجعوا محتواها سوا قبل البدء."
                    else -> "المستويات فيها تدرّج في نوع القرب والكلام. اختاروا مستوى يناسب الطرفين."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            SirrSection("02", "اختاروا عدد الجولات", "الاختيار ده ينفع يتكرر في ليلة تانية")
            listOf(6, 10, 12, 18).chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { count ->
                        SirrOption(title = "$count جولات", selected = roundCount == count,
                            onClick = { roundCount = count; resetStartConsents() }, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (group) {
                Text("كام واحد هيلعب؟", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (2..4).forEach { count ->
                        SirrOption(title = "$count لاعبين", selected = seats == count,
                            onClick = { seats = count; resetStartConsents() }, modifier = Modifier.weight(1f))
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
                    FilterChip(selected = variety == option, onClick = { variety = option; resetStartConsents() },
                        label = { Text(option.arabic) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(avoidRepeat, { avoidRepeat = it })
                    Text("بلاش نفس تحديات المرة اللي فاتت")
                }
                OutlinedButton(onClick = { intensity = Intensity.CUSTOM; resetStartConsents() }, modifier = Modifier.fillMaxWidth()) {
                    Text("اكتب تحديات من عندك")
                }
                if (intensity == Intensity.CUSTOM) {
                    OutlinedTextField(
                        value = customText, onValueChange = { customText = it.take(30_000); resetStartConsents() },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8,
                        label = { Text("كل تحدي في سطر") }
                    )
                    Text("${customEntries.size} تحدي — من 6 لـ 500 حرف لكل واحد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!customValid) Text("في تحديات متكررة أو أطول من المسموح", color = MaterialTheme.colorScheme.error)
                    Text("كل سطر تحدي مستقل. لو عدد تحدياتك أقل من الجولات، الباقي بيتكمل من البنك الجاهز.")
                    OutlinedButton(
                        onClick = { importChallenges.launch(arrayOf("text/plain", "text/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("استورد تحدياتي من ملف TXT") }
                    OutlinedButton(
                        enabled = customEntries.isNotEmpty(),
                        onClick = { exportChallenges.launch("R2H-Play-Couple-Challenges.txt") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("صدّر تحدياتي لملف TXT") }
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
                        value = names[i], onValueChange = { names[i] = it.take(32); consent[i] = false },
                        label = { Text("اللاعب ${i + 1}") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                EffectPreferencesPanel(effects)
            }
            if (!group) {
                HorizontalDivider(color = Color(0xFF5C4153))
                SirrSection("03", "المكافأة ومدة الجلسة",
                    "اتفقوا على مكافأة الفائز قبل اللعب. المكافأة اختيارية في تنفيذها حتى بعد الفوز.")
                OutlinedTextField(
                    value = rewardText, onValueChange = { rewardText = it.take(200); resetStartConsents() },
                    label = { Text("مكافأة الفائز — اتفقوا عليها سوا") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                )
                OutlinedTextField(
                    value = tieChallengeText,
                    onValueChange = { tieChallengeText = it.take(500); resetStartConsents() },
                    label = { Text("تحدي كسر التعادل — اختياري") },
                    placeholder = { Text("لو سبتوه فاضي هنستخدم تحدي مشترك جاهز") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                )
                Text("مدة الجلسة — انتهاء الوقت تنبيه، مش خصم تلقائي")
                listOf(15, 30, 60, 0).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { minutes ->
                            SirrOption(
                                title = if (minutes == 0) "من غير وقت" else "$minutes دقيقة",
                                selected = durationMinutes == minutes,
                                onClick = { durationMinutes = minutes; resetStartConsents() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text("الجولة المكتملة +10، عدم التنفيذ -10، والاتفاق على عدم التنفيذ 0.")
                Text("الجولات القصيرة مش شرط تكمّل مدة الجلسة؛ والمدة الإجمالية تنبيه مش نهاية إجبارية.")
                Text("الكل يقدر يرفض أو يوقف أي نشاط. المكافأة لا تلزم أي طرف بفعل غير موافق عليه.")
            }
            HorizontalDivider(color = Color(0xFF5C4153))
            SirrSection(if (group) "03" else "04", "الكل موافق؟",
                "كل واحد يوافق بنفسه. تخطي التحدي أو وقف اللعبة متاح في أي وقت.")
            repeat(seats) { i ->
                SirrConsent(name = names[i], checked = consent[i],
                    onChange = { consent[i] = it })
            }
            val waiting = (0 until seats).filter { !consent[it] }
            val invalidNames = (0 until seats).any { names[it].isBlank() }
            val invalidCustom = intensity == Intensity.CUSTOM &&
                (!customValid || customEntries.isEmpty())
            val invalidReward = !group && rewardText.trim().isEmpty()
            val invalidTieText = !group && tieChallengeText.isNotBlank() &&
                tieChallengeText.trim().length < 6
            val startHint = when {
                invalidNames -> "اكتبوا اسم لكل لاعب من الاختيارات الإضافية"
                invalidCustom -> "كمّل التحديات المخصوصة الأول"
                invalidReward -> "اتفقوا واكتبوا مكافأة الفائز قبل ما تبدأوا"
                invalidTieText -> "اكتب تحدي كسر التعادل بوضوح أو سيبه فاضي"
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
                            roundCount = roundCount, variety = variety, recentlySeen = recent,
                            competition = if (!group) CoupleCompetition(
                                reward = rewardText.trim(),
                                sessionMinutes = durationMinutes,
                                startedAtMillis = System.currentTimeMillis(),
                                tieChallenge = tieChallengeText.trim().ifBlank {
                                    "كل واحد يقول لشريكه 3 حاجات جديدة بيقدّرها فيه، من غير ما يقاطع التاني."
                                }
                            ) else null
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
                enabled = !invalidNames && !invalidCustom && !invalidReward && !invalidTieText && waiting.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (formError.isNotEmpty()) Text(formError, color = MaterialTheme.colorScheme.error)
        } else {
            val current = game!!
            if (!current.finished) {
                if (current.competition != null) {
                    Text(
                        current.playerNames.indices.joinToString("    |    ") {
                            "${current.playerNames[it]}: ${current.scores[it]} نقطة"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("الجولة ${current.round + 1} من ${current.turns.size}")
                    if (current.competition.timeExpired(clockNow)) {
                        Text("وقت الجلسة المحدد خلص. تقدروا تكملوا أو تحسبوا نتيجة الجولات اللي خلصت.")
                        OutlinedButton(onClick = {
                            if (current.round > 0) {
                                runCatching {
                                    val next = current.settleEarly()
                                    LocalSessionStore.save(vault, gameName, next)
                                    next
                                }.onSuccess { game = it }
                                    .onFailure { replyStatus = "تعذر حفظ إنهاء الجلسة" }
                            }
                        }, enabled = current.round > 0, modifier = Modifier.fillMaxWidth()) {
                            Text("نحسب النتيجة دلوقتي")
                        }
                    }
                }
                val turn = current.turns[current.round]
                var isRevealed by remember(current.round) {
                    mutableStateOf(current.competition?.roundClock?.started == true)
                }
                // Opt-in is renewed for the current actor after each new reveal.
                var roundConsent by remember(current.round) { mutableStateOf(false) }
                var partnerConsent by remember(current.round) { mutableStateOf(false) }
                var skipAgreedA by remember(current.round) { mutableStateOf(false) }
                var skipAgreedB by remember(current.round) { mutableStateOf(false) }
                LaunchedEffect(current.round) { effects.cue(GameCue.ROUND) }
                SirrSection("${current.round + 1}", "جولة جديدة · السر مستنيكم",
                    if (current.competition != null)
                        "النتيجة حسب القواعد المتفق عليها، والتوقف متاح في أي وقت."
                    else "لو مش مناسب، عدّوه من غير أي حسابات.")
                LinearProgressIndicator(
                    progress = { current.round.toFloat() / current.turns.size },
                    modifier = Modifier.fillMaxWidth()
                )
                RoundChapter(current.round,current.turns.size)
                Text("الدور على ${current.playerNames[turn.actorIndex]}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (current.competition != null) {
                    Text(
                        "${turn.prompt.kind.arabic} · ${turn.prompt.durationSeconds / 60} دقيقة",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (turn.prompt.title.isNotBlank()) {
                        Text(turn.prompt.title, style = MaterialTheme.typography.titleLarge)
                    }
                }
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
                fun finishRound(outcome: CoupleOutcome) {
                    if (reply.isNotBlank() && runCatching {
                        LocalSessionStore.writeReply(vault, replyName, current.round, reply)
                    }.isFailure) {
                        replyStatus = "فشل حفظ الرد؛ النص لسه موجود علشان متفقدوش"
                        return
                    }
                    runCatching {
                        val next = if (current.competition != null) current.resolve(outcome)
                            else current.resolve(outcome == CoupleOutcome.COMPLETED)
                        LocalSessionStore.save(vault, gameName, next)
                        next
                    }.onSuccess { next ->
                        effects.cue(if (outcome == CoupleOutcome.COMPLETED) GameCue.SUCCESS else GameCue.SKIP)
                        game = next
                        reply = ""; mediaOpen = false; replyStatus = ""
                    }.onFailure {
                        replyStatus = "ما قدرناش نحفظ النتيجة. اتأكد من المؤقت ومساحة التخزين وحاول تاني."
                    }
                }
                if (isRevealed) {
                    Text(
                        "المشاركة اختيارية في كل تحدي. لو فيه شخص تاني هيشارك في أي تلامس أو نشاط، لازم تسأل وتتأكد من موافقته دلوقتي. الرفض أو تغيير الرأي يوقف النشاط فورًا.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SirrConsent(
                        name = "${current.playerNames[turn.actorIndex]} — موافق على التحدي ده",
                        checked = roundConsent,
                        onChange = { roundConsent = it }
                    )
                    if (current.competition != null) {
                        val other = 1 - turn.actorIndex
                        SirrConsent(
                            name = "${current.playerNames[other]} — موافق على المشاركة في التحدي",
                            checked = partnerConsent,
                            onChange = { partnerConsent = it }
                        )
                    }
                    val bothConsent = roundConsent &&
                        (current.competition == null || partnerConsent)
                    if (current.competition != null) {
                        CoupleRoundClockPanel(
                            game = current,
                            nowMillis = clockNow,
                            bothConsent = bothConsent
                        ) { next ->
                            runCatching {
                                LocalSessionStore.save(vault, gameName, next)
                            }.onSuccess {
                                game = next
                                clockNow = System.currentTimeMillis()
                                replyStatus = ""
                            }.onFailure { replyStatus = "ما قدرناش نحفظ حالة المؤقت" }.isSuccess
                        }
                    }
                    val clockFinished = current.competition == null ||
                        (current.competition.roundClock.started &&
                            current.competition.roundClock.remainingSeconds(clockNow) == 0)
                    SirrAction(
                        text = "التحدي اتعمل  ✦",
                        enabled = bothConsent && clockFinished,
                        explanation = when {
                            !bothConsent -> "محتاج موافقة الطرفين قبل تسجيل التنفيذ"
                            !clockFinished -> "كمّلوا وقت التحدي قبل احتساب النقاط"
                            else -> null
                        },
                        onClick = { finishRound(CoupleOutcome.COMPLETED) }
                    )
                }
                if (current.competition != null) {
                    OutlinedButton(
                        onClick = { finishRound(CoupleOutcome.NOT_COMPLETED) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("التحدي ما اتنفذش — خصم 10 نقاط") }
                    Text("اختيار صفر نقطة لازم يكون باتفاق الطرفين:")
                    SirrConsent(current.playerNames[0] + " موافق على التخطي بالاتفاق",
                        skipAgreedA, { skipAgreedA = it })
                    SirrConsent(current.playerNames[1] + " موافق على التخطي بالاتفاق",
                        skipAgreedB, { skipAgreedB = it })
                    OutlinedButton(
                        enabled = skipAgreedA && skipAgreedB,
                        onClick = { finishRound(CoupleOutcome.AGREED_SKIP) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("اتفقنا ما ننفّذوش — 0 نقطة") }
                } else {
                    OutlinedButton(
                        onClick = { finishRound(CoupleOutcome.NOT_COMPLETED) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("مش مناسب؟ عَدّي من غير إحراج") }
                }
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
                if (current.competition != null) {
                    CoupleCompetitionPanel(current) { next ->
                        runCatching {
                            LocalSessionStore.save(vault, gameName, next)
                        }.onSuccess { game = next }.isSuccess
                    }
                } else {
                    Text("المهم إن الوقت كان على مزاجكم.", style = MaterialTheme.typography.bodyLarge)
                    current.playerNames.forEachIndexed { index, name ->
                        Text("${name} — ${current.scores[index]} تحدي اتعمل، ${current.skipped[index]} اتعدّى")
                    }
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
