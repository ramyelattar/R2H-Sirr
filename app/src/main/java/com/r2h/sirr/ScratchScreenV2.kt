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
                "كسك سخن أوي دلوقتي وأنا لسه ما لمستوش.",
                "عايز أحط زبي جوا كسك وأحركه لحد ما تصرخي.",
                "تحدي: افتحي رجليكي وخليني أنيكك دلوقتي لحد ما أفرغ لبن جوا كسك."
            ),
            listOf(
                "طيزك بتنداس عليا كل ما تقعدي جنبي.",
                "نفسي أنيك طيزك من ورا وأحط لبن جواها.",
                "قوليلي دلوقتي: عايزة زبي في طيزك ولا في كسك الأول؟"
            ),
            listOf(
                "بشم ريحة كسك من بعيد وبتجنني.",
                "عايز ألحس كسك لحد ما تبلعي وتجيبي لبن على وشي.",
                "تحدي: اركعي وامصصي زبي لحد ما أفرغ في بؤك."
            ),
            listOf(
                "زبي واقف من ساعة ما شفتك.",
                "نفسي أدخل زبي في كسك وأنيكك بقوة لحد ما تبكى.",
                "لو سمحتيني أنيكك دلوقتي هتخليني أفرغ جوا ولا برا؟"
            ),
            listOf(
                "كسك مبلول وأنا عارف كده.",
                "عايز أنيكك من ورا وأمسك طيزك وأدوس عليها.",
                "تحدي: قعدي على زبي وتحركي لحد ما أجي جوا كسك."
            ),
            listOf(
                "نفسي أشوف لبنك ينزل وأنا بنيكك.",
                "عايز أحط صوابعي في كسك وطيزك في نفس الوقت.",
                "قوليلي: عايزة أنيكك لحد ما أفرغ لبن كتير جوا كسك."
            ),
            listOf(
                "طيزك كبيرة وبتخليني أفكر أنيكها.",
                "نفسي أدخل زبي في طيزك ببطء لحد ما يخش كله.",
                "تحدي: انقلبي على بطنك وخليني أنيك طيزك لحد ما أجي جواها."
            ),
            listOf(
                "بفكر في كسك وهو بيتفتح على زبي.",
                "عايز أنيكك وأنتِ بتصرخي وتقوليلي أعمق.",
                "لو فتحتي رجليكي دلوقتي هتخليني أفرغ لبن في كسك ولا في طيزك؟"
            ),
            listOf(
                "كسك سخن ومحتاج زبي.",
                "نفسي ألحس طيزك وكسيك لحد ما تبلعي.",
                "تحدي: امسكي زبي وحطيه في كسك بنفسك وتحركي."
            ),
            listOf(
                "عايز أشوفك وأنتِ بتبلعي لبن من كسك.",
                "نفسي أنيكك وأمسك شعرك وأدوسك.",
                "قوليلي دلوقتي: عايزة أنيك طيزك ولا كسك لحد ما أجي جوا؟"
            ),
            listOf(
                "زبي عايز يدخل كسك دلوقتي.",
                "عايز أنيكك بقوة لحد ما تبكى وتقوليلي كفاية.",
                "تحدي: قعدي على وشي وخليني ألحس كسك لحد ما تجيبي."
            ),
            listOf(
                "طيزك بتنداس عليا وبتخليني أجن.",
                "نفسي أحط زبي بين طيزك وأفرغ لبن عليها.",
                "لو سمحتيني أنيك طيزك هتخليني أجي جوا ولا برا؟"
            ),
            listOf(
                "كسك بيتفتح وأنا بفكر فيه.",
                "عايز أدخل زبي في كسك وأحركه بسرعة.",
                "تحدي: افتحي رجليكي واسمحيلي أنيكك لحد ما أفرغ لبن كتير جوا."
            ),
            listOf(
                "نفسي أشوف لبن ينزل من كسك بعد ما أنيكك.",
                "عايز أنيكك من ورا وأمسك طيزك بقوة.",
                "قوليلي: عايزة زبي في كسك ولا في طيزك دلوقتي؟"
            ),
            listOf(
                "بشم ريحة كسك وبتخليني أقف.",
                "نفسي ألحس كسك لحد ما تبلعي على لساني.",
                "تحدي: اركعي وامصصي زبي لحد ما أفرغ لبن في بؤك."
            ),
            listOf(
                "زبي واقف ومحتاج كسك.",
                "عايز أنيكك وأنتِ بتقوليلي أعمق وأسرع.",
                "لو فتحتي طيزك دلوقتي هتخليني أدخل زبي وأجي جواها؟"
            ),
            listOf(
                "كسك مبلول وأنا عايز ألحسه.",
                "نفسي أحط صوابعي في طيزك وكسيك مع بعض.",
                "تحدي: قعدي على زبي وتحركي لحد ما أجي جوا كسك. لو موافقة وبراحتك."
            ),
            listOf(
                "طيزك بتخليني أفكر أنيكها دلوقتي.",
                "عايز أدخل زبي في طيزك ببطء لحد ما يخش.",
                "قوليلي: عايزة لبن في طيزك ولا في كسك؟"
            ),
            listOf(
                "نفسي أشوفك وأنتِ بتصرخي وأنا بنيكك.",
                "عايز أنيكك بقوة وأمسك شعرك.",
                "تحدي: انقلبي وخليني أنيك طيزك لحد ما أفرغ جواها."
            ),
            listOf(
                "كسك سخن ومحتاج أنيكه دلوقتي.",
                "نفسي ألحس كسك وطيزك لحد ما تبلعي.",
                "لو سمحتيني أنيكك هتخليني أجي جوا كسك ولا برا؟"
            )
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
