package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.ceil

@Composable
internal fun ScratchScreenV2() {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    var stages by remember { mutableIntStateOf(1) }
    val texts = remember { mutableStateListOf("", "", "") }
    var extras by remember { mutableStateOf(false) }
    val secretGroups = remember {
        listOf(
            listOf(
                "في حاجة في نظرتك بتخليني أنسى أنا كنت هقول إيه.",
                "نفسي أقولك إمتى بدأت أحس إن بينا كيمياء مختلفة.",
                "لو السؤال مريح ليك، اختار اللحظة اللي نفسك نعيشها سوا.",
            ),
            listOf(
                "فاكر آخر مرة اتقابلت فيها عيوننا وسكتنا؟",
                "السكوت ده كان وراه كلام كتير لسه ما اتقالش.",
                "اتحداك تكمّل الجملة: «كنت عايز أقولك وقتها…».",
            ),
            listOf(
                "في رسالة غزل في دماغي ومش عارف أبدأها.",
                "أول سطر فيها عن تفصيلة صغيرة فيك بتشدني.",
                "لو حابب، اختر أنت آخر سطر ونخليها سر بينا.",
            ),
            listOf(
                "أوقات ببقى عايز أقرب ومش عايز أستعجل.",
                "أكتر حاجة بتريحني إننا نقدر نسأل بعض بصراحة.",
                "قولّي: تحب نتكلم الأول ولا نقعد ساكتين شوية؟",
            ),
            listOf(
                "لو الليلة دي فيلم رومانسي، أنت بطله معايا.",
                "أنا مخبي عنوان المشهد اللي لسه ما اتصورش.",
                "اختر عنوانًا جريئًا ولطيفًا، وأنا أقولك عنواني.",
            ),
            listOf(
                "في كلمة واحدة منك بتغيّر مزاج يومي كله.",
                "نفسي تعرف إنها بتيجي في بالي حتى وإنت بعيد.",
                "جرّب تخمّن الكلمة؛ ليك ثلاث محاولات وبس.",
            ),
            listOf(
                "في حاجة في ضحكتك بتخليني أتوه عن الكلام.",
                "كل مرة بسمعها بفتكر موقف بينا ما نسيتهوش.",
                "لو تحب، احكي إنت كمان عن ذكرى بتضحكك لحد دلوقتي.",
            ),
            listOf(
                "نفسي أقولك اعتراف صغير من غير ما أعمل مقدمة.",
                "كنت فاكر إني مش بتكسف، لحد ما اتكلمت معاك.",
                "اتحداك تقول موقف اتكسفت فيه من مجاملة صادقة.",
            ),
            listOf(
                "لو اتقابلنا النهارده لأول مرة، هتلاحظ إيه؟",
                "أنا غالبًا هفتكر طريقتك في الكلام قبل أي حاجة.",
                "اختار أول جملة تعارف تقولها لي من غير تردد.",
            ),
            listOf(
                "في أغنية كل ما بتيجي بفتكر قعدتنا.",
                "الأغنية دي مش مشهورة، بس ليها معنى عندي.",
                "لو تحب، اختار أغنية نخليها علامة سرية بينا.",
            ),
            listOf(
                "نفسي نعمل مفاجأة بسيطة من غير تخطيط كتير.",
                "الفكرة مش في المكان؛ الفكرة إننا نكون مرتاحين.",
                "اختار: خروجة مفاجئة ولا قعدة هادية نتكلم فيها؟",
            ),
            listOf(
                "أنا بلاحظ تفاصيل صغيرة يمكن أنت مش واخد بالك منها.",
                "أكتر تفصيلة بتحيرني هي اللحظة اللي بتسكت فيها.",
                "لو حابب، قولّي بيكون في بالك إيه وقتها.",
            ),
            listOf(
                "في كلام حلو كنت ناوي أقوله واتراجعت.",
                "خفت يتفهم غلط، مع إنه كان صادق جدًا.",
                "اختار نسمعه دلوقتي ولا أكتبهولك في رسالة؟",
            ),
            listOf(
                "نفسي نسأل بعض سؤال ما اتسألش قبل كده.",
                "مش سؤال محرج؛ سؤال يخلينا نفهم بعض أكتر.",
                "اتحداك تسألني حاجة عن الاشتياق من غير ما تتكسف.",
            ),
            listOf(
                "لما بنبعد شوية، بتفضل في دماغي تفصيلة واحدة.",
                "مش صورة ولا صوت؛ إحساس إن القعدة معاك كانت مريحة.",
                "قولّي إيه أكتر حاجة بتفتكرها بعد ما نمشي.",
            ),
            listOf(
                "تخيل إن معاك مفتاح لصندوق رسائل قديمة بينا.",
                "هتلاقي فيه اعتراف اتكتب واتمسح قبل ما يتبعت.",
                "اختر: تحب تعرف بدايته ولا سبب إني ما بعتوش؟",
            ),
            listOf(
                "أنا ساعات بضحك علشان أخبي إنك مكسوفني.",
                "أصعب حاجة عندي إني أقول مجاملة وأنا باصص في عينك.",
                "لو مريح ليك، خلينا نتبادل مجاملة واحدة وبس.",
            ),
            listOf(
                "كل واحد فينا عنده طريقة بيطلب بيها الاهتمام.",
                "أنا غالبًا بلمّح بدل ما أقولها بشكل مباشر.",
                "اتحداك تقولّي إزاي أحب أسألك لو محتاج قرب.",
            ),
            listOf(
                "في سر صغير عن أول مرة حسيت فيها بالثقة معاك.",
                "السر مش حدث كبير؛ كان موقف عادي لكنه فرّق.",
                "لو تحب، احكي عن أول مرة حسيت إنك مفهوم.",
            ),
            listOf(
                "دي آخر ورقة في الصندوق، وفيها اختيارك أنت.",
                "أنا قلت اللي عندي؛ والباقي مش لازم يتقال دلوقتي.",
                "اختار اعتراف لطيف أو سؤال جديد أو نختم بسر نحتفظ بيه.",
            ),
        )
    }
    var photoId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(true) }
    var stage by remember { mutableIntStateOf(0) }
    var reset by remember { mutableIntStateOf(0) }
    var ttlHours by remember { mutableIntStateOf(3) }
    var expiresAt by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf("") }
    var stageRevealed by remember { mutableStateOf(false) }
    DisposableEffect(photoId) { val activeId = photoId; onDispose { vault.delete(activeId) } }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val newId = vault.importPhoto(uri)
            vault.delete(photoId); photoId = newId; error = ""
        }.onFailure { error = "فشل إدراج الصورة" }
    }
    fun close() {
        editing = true; stage = 0; stageRevealed = false; reset++; expiresAt = 0
        texts.indices.forEach { texts[it] = "" } // Destroy private session messages on close/expiry.
        vault.delete(photoId); photoId = null
    }
    LaunchedEffect(editing, expiresAt) {
        if (!editing && expiresAt > 0) {
            val wait = expiresAt - System.currentTimeMillis()
            if (wait > 0) kotlinx.coroutines.delay(wait)
            close()
        }
    }
    Page("اكشف اللي مستخبي", "مفيش طريقة تعرف اللي جاي… غير لما تكشفه بنفسك.") {
        if (editing) {
            SirrIntro(
                overline = "مفاجأة لا تتكشف إلا بإيدك",
                title = "الغطا مخبّي إيه؟",
                description = "جهّزوا سر، خبّوه تحت الغطا، وسيبوا المفاجأة تتكشف خطوة خطوة.",
                accent = Color(0xFF93604B), motif = "scratch"
            )
            SirrSection("01", "حضّروا المفاجأة", "اكتبوا سر أو اختاروا سر جاهز")
            OutlinedButton(
                onClick = {
                    val group = secretGroups.random()
                    repeat(3) { i -> texts[i] = group[i] }
                    // Keep the user's selected layer count (1, 2 or 3).
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("✦  اختارلي سر جاهز") }
            SirrSection("02", "كام غطا هيتكشف؟", "من مفاجأة واحدة لتلات مفاجآت ورا بعض")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..3).forEach { n ->
                    SirrOption("$n", stages == n, onClick = { stages = n },
                        modifier = Modifier.weight(1f),
                        subtitle = if (n == 1) "سر واحد" else "$n أسرار")
                }
            }
            repeat(stages) { i ->
                OutlinedTextField(texts[i], { texts[i] = it.take(280) }, label = { Text("السر المستخبي ${i+1}") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            OutlinedButton(onClick={extras=!extras},modifier=Modifier.fillMaxWidth()){
                Text(if(extras)"اقفل الاختيارات الإضافية" else "ممكن تزود صورة أو تغيّر وقت العرض")
            }
            if(extras) {
                OutlinedButton(onClick={pickPhoto.launch("image/*")}){Text("حط صورة في الآخر")}
                if(photoId!=null)TextButton(onClick={vault.delete(photoId);photoId=null}){Text("امسح الصورة")}
                Text("المفاجأة تفضل مفتوحة قد إيه؟")
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    listOf(3,6,24).forEach{hours->
                        FilterChip(ttlHours==hours,onClick={ttlHours=hours},label={Text("$hours ساعات")})
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            SirrAction(
                text = "خبّي المفاجأة  ✦",
                enabled = texts.take(stages).all { it.isNotBlank() },
                onClick = {
                    editing = false; stage = 0; stageRevealed = false; reset++
                    expiresAt = System.currentTimeMillis() + ttlHours * 3_600_000L
                },
                explanation = if (texts.take(stages).any { it.isBlank() })
                    "كمّل الكلام المستخبي علشان تبدأ"
                else "جاهزة… سلّم الموبايل للشخص اللي هيكشفها"
            )
        } else {
            SirrSection("0${stage + 1}", "الغطا ${stage + 1} من $stages",
                if (stageRevealed) "المفاجأة اتكشفت!" else
                    "مرّر صباعك على الكارت لحد ما يبان السر")
            key(reset, stage) {
                ScratchPanel(texts[stage]) { stageRevealed = true }
            }
            if (stage < stages-1) {
                SirrAction(
                    text = "اكشف اللي بعدها  ✦",
                    enabled = stageRevealed,
                    onClick = { stage++; stageRevealed = false; reset++ },
                    explanation = if (!stageRevealed)
                        "اكشف جزء كفاية من الغطا الأول" else null
                )
            } else {
                val photo = remember(photoId) { vault.readPhoto(photoId) }
                if (photo != null && stageRevealed) Image(photo, contentDescription = "المفاجأة الأخيرة", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
                if (stageRevealed) {
                    SirrAction(
                        text = "كده السر اتكشف  ✦",
                        onClick = { close() },
                        explanation = "هتقفل الجلسة وتمسح الصورة الخاصة بيها"
                    )
                }
            }
            OutlinedButton(onClick = { stageRevealed = false; reset++ }, modifier = Modifier.fillMaxWidth()) { Text("خبّيها تاني") }
            TextButton(onClick = { close() }) { Text("اقفل وامسح الصورة") }
        }
    }
}

@Composable
internal fun ScratchPanel(hiddenText: String, onRevealed: () -> Unit) {
    val effects = rememberGameEffects()
    val brushRadiusPx = with(LocalDensity.current) { 17.dp.toPx() }
    // Nested observable lists append in O(1), avoiding quadratic copy-on-each-pointer-event.
    val strokes = remember { mutableStateListOf<androidx.compose.runtime.snapshots.SnapshotStateList<Offset>>() }
    var savedPoints by remember { mutableIntStateOf(0) }
    val cells = remember { BooleanArray(192) }
    var coveredCells by remember { mutableIntStateOf(0) }
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    var alreadyRevealed by remember { mutableStateOf(false) }
    // Coverage measures the same brush-sized trail that is visibly cleared.
    // 16×12 cells are sampled along the line and inside the brush radius.
    val coverage = (coveredCells / 192f).coerceIn(0f, 1f)
    val revealed = coverage >= .58f
    LaunchedEffect(revealed) {
        if (revealed && !alreadyRevealed) {
            alreadyRevealed = true
            effects.cue(GameCue.REVEAL)
            onRevealed()
        }
    }
    fun mark(center: Offset) {
        val w = dimensions.width.toFloat()
        val h = dimensions.height.toFloat()
        if (w < 1f || h < 1f) return
        // The brush width and hit testing both scale in density-independent
        // physical pixels; clamp the edge so swiping outside has no effect.
        if (center.x < 0f || center.y < 0f || center.x > w || center.y > h) return
        val radius = brushRadiusPx
        val startX = ((center.x - radius) / w * 16).toInt().coerceIn(0, 15)
        val endX = ((center.x + radius) / w * 16).toInt().coerceIn(0, 15)
        val startY = ((center.y - radius) / h * 12).toInt().coerceIn(0, 11)
        val endY = ((center.y + radius) / h * 12).toInt().coerceIn(0, 11)
        for (y in startY..endY) for (x in startX..endX) {
            val midX = (x + .5f) * w / 16f
            val midY = (y + .5f) * h / 12f
            val dx = midX - center.x
            val dy = midY - center.y
            val index = y * 16 + x
            if (dx * dx + dy * dy <= radius * radius && !cells[index]) {
                cells[index] = true
                coveredCells++
            }
        }
    }
    fun begin(p: Offset) {
        if (savedPoints < 2400) { strokes.add(mutableStateListOf(p)); savedPoints++ }
        mark(p)
    }
    fun append(p: Offset) {
        val previous = strokes.lastOrNull()?.lastOrNull()
        if (previous == null) { begin(p); return }
        val dx = p.x - previous.x
        val dy = p.y - previous.y
        val distance = hypot(dx, dy)
        if (distance < 1f) return
        val count = ceil(distance / 7f).toInt().coerceIn(1, 180)
        for (i in 1..count) {
            val t = i.toFloat() / count
            mark(Offset(previous.x + dx * t, previous.y + dy * t))
        }
        if (savedPoints < 2400 && strokes.isNotEmpty()) {
            strokes.last().add(p)
            savedPoints++
        }
    }
    Text(if(revealed) "السر بان! ✦" else "امسح بإيدك لحد ما يظهر السر — ${(coverage * 100).toInt()}٪",
        color=MaterialTheme.colorScheme.onSurfaceVariant)
    Box(Modifier.fillMaxWidth().height(265.dp).clip(RoundedCornerShape(27.dp)),contentAlignment=Alignment.Center) {
        Surface(color=Color(0xFF321B2B),modifier=Modifier.fillMaxSize()) {
            Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                Text(hiddenText, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            }
        }
        if (!revealed) Canvas(Modifier.fillMaxSize()
            .onSizeChanged { dimensions = it }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(dimensions) {
                detectDragGestures(
                    onDragStart = { begin(it) },
                    onDrag = { change, _ ->
                        append(change.position)
                        change.consume()
                    }
                )
            }
            .pointerInput(dimensions) { detectTapGestures { begin(it) } }
        ) {
            drawRect(Brush.linearGradient(listOf(Color(0xFFDCB98D),Color(0xFF8C5169),Color(0xFF50283A))))
            drawCircle(Color.White.copy(alpha=.1f),radius=size.minDimension*.3f,
                center=Offset(size.width*.83f,size.height*.27f))
            for (i in 0..19) {
                val x = (i*41 % 97) / 97f * size.width
                val y = (i*73 % 97) / 97f * size.height
                drawCircle(
                    color = Color(0xFFFFEDCF).copy(alpha = if(i%2==0) .38f else .2f),
                    radius = (if(i%3==0) 2.3f else 1.3f).dp.toPx(),
                    center = Offset(x, y)
                )
            }
            // Keep the visual clearing as connected lines rather than isolated dots.
            strokes.forEach { stroke ->
                if (stroke.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        if (stroke.size == 1) {
                            lineTo(stroke.first().x + .1f, stroke.first().y)
                        } else {
                            for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
                        }
                    }
                    drawPath(path, color = Color.Transparent, style = Stroke(
                        width = 34.dp.toPx(), cap = StrokeCap.Round
                    ), blendMode = BlendMode.Clear)
                }
            }
        }
    }
    if (revealed) Text("بان السر!", color = MaterialTheme.colorScheme.primary)
}
