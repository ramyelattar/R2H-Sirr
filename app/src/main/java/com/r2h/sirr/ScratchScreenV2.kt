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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.r2h.sirr.core.SecretStory
import com.r2h.sirr.core.SecretStoryCatalog
import com.r2h.sirr.core.SecretTier
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
    var suggestionTier by remember { mutableStateOf(SecretTier.SOFT) }
    var selectedStory by remember { mutableStateOf<SecretStory?>(null) }
    var handoff by remember { mutableStateOf(false) }
    var viewerReady by remember { mutableStateOf(false) }
    fun applyPreset(story: SecretStory, count: Int) {
        val chosen = SecretStoryCatalog.scratchLayers(story, count)
        texts.indices.forEach { i -> texts[i] = chosen.getOrElse(i) { "" } }
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
        handoff = false; viewerReady = false; selectedStory = null; error = ""
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
            Text("مستوى الأسرار الجاهزة", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SecretTier.entries.forEach { tier ->
                    FilterChip(
                        selected = suggestionTier == tier,
                        onClick = {
                            if (selectedStory != null && suggestionTier != tier) {
                                texts.indices.forEach { texts[it] = "" }
                            }
                            suggestionTier = tier
                            selectedStory = null
                        },
                        label = { Text(tier.arabic) }
                    )
                }
            }
            if (suggestionTier == SecretTier.ADULT) Text(
                "المستوى الحميم فيه كلام مباشر عن الجنس والرغبة، للكبار 18+ فقط. مفيش نشاط مطلوب تنفيذه.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    val options = SecretStoryCatalog.scratch.filter { it.tier == suggestionTier }
                    val candidates = options.filterNot { it.id == selectedStory?.id }
                    val chosen = (if (candidates.isEmpty()) options else candidates).random()
                    selectedStory = chosen
                    applyPreset(chosen, stages)
                    error = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("✦  اختارلي حكاية سرية جاهزة") }
            SirrSection("02", "كام غطا هيتكشف؟", "من مفاجأة واحدة لتلات مفاجآت ورا بعض")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (1..3).forEach { n ->
                    SirrOption("$n", stages == n, onClick = {
                        stages = n
                        selectedStory?.let { applyPreset(it, n) }
                    },
                        modifier = Modifier.weight(1f),
                        subtitle = if (n == 1) "سر واحد" else "$n أسرار")
                }
            }
            repeat(stages) { i ->
                OutlinedTextField(
                    texts[i],
                    {
                        texts[i] = it.take(280)
                        selectedStory = null
                    },
                    label = { Text("السر المستخبي ${i+1}") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedButton(onClick={extras=!extras},modifier=Modifier.fillMaxWidth()){
                Text(if(extras)"اقفل الاختيارات الإضافية" else "ممكن تزود صورة أو تغيّر وقت العرض")
            }
            if(extras) {
                OutlinedButton(onClick={pickPhoto.launch("image/*")}){Text("حط صورة في الآخر")}
                if(photoId!=null)TextButton(onClick={vault.delete(photoId);photoId=null}){Text("امسح الصورة")}
                Text("حد عرض الجلسة وهي مفتوحة على الموبايل (لا يُستعاد السر بعد الخروج):")
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
                    editing = false; handoff = true; viewerReady = false
                    stage = 0; stageRevealed = false; reset++
                    expiresAt = System.currentTimeMillis() + ttlHours * 3_600_000L
                },
                explanation = if (texts.take(stages).any { it.isBlank() })
                    "كمّل الكلام المستخبي علشان تبدأ"
                else "جاهزة… سلّم الموبايل للشخص اللي هيكشفها"
            )
        } else if (handoff) {
            SirrIntro(
                overline = "المفاجأة مقفولة",
                title = "جاهز تكشف السر؟",
                description = "سلّم الموبايل للشخص المقصود. الحكاية ممكن تكون شخصية أو حميمة، ومن حقه يقفل من غير ما يكشفها.",
                accent = Color(0xFF93604B),
                motif = "scratch"
            )
            SirrConsent(
                name = "أنا جاهز أشوف السر، وعارف إني أقدر أوقف في أي وقت",
                checked = viewerReady,
                onChange = { viewerReady = it }
            )
            SirrAction(
                text = "ابدأ كشف أول كارت",
                enabled = viewerReady,
                onClick = { handoff = false }
            )
            OutlinedButton(onClick = { close() }, modifier = Modifier.fillMaxWidth()) {
                Text("مش حابب أشوفها — امسح الأسرار")
            }
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
            TextButton(onClick = { close() }) { Text("اقفل وامسح الأسرار والصورة") }
        }
    }
}

@Composable
internal fun ScratchPanel(hiddenText: String, onRevealed: () -> Unit) {
    val effects = rememberGameEffects()
    val brushRadiusPx = with(LocalDensity.current) { 17.dp.toPx() }
    // A bounded mask uses constant memory no matter how long the user scratches.
    // Coverage and the visible erased cells share the same 32 x 24 mask.
    val columns = 32
    val rows = 24
    val cells = remember { BooleanArray(columns * rows) }
    var coveredCells by remember { mutableIntStateOf(0) }
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    var alreadyRevealed by remember { mutableStateOf(false) }
    var revealWithoutTouch by remember { mutableStateOf(false) }
    val coverage = coveredCells / (columns * rows).toFloat()
    val revealed = revealWithoutTouch || coverage >= 0.58f

    LaunchedEffect(revealed) {
        if (revealed && !alreadyRevealed) {
            alreadyRevealed = true
            effects.cue(GameCue.REVEAL)
            onRevealed()
        }
    }

    fun mark(center: Offset) {
        if (revealed) return
        val w = dimensions.width.toFloat()
        val h = dimensions.height.toFloat()
        if (w < 1f || h < 1f || center.x < 0f || center.y < 0f ||
            center.x > w || center.y > h) return
        val cellWidth = w / columns
        val cellHeight = h / rows
        val radius = brushRadiusPx
        val startX = ((center.x - radius) / cellWidth).toInt().coerceIn(0, columns - 1)
        val endX = ((center.x + radius) / cellWidth).toInt().coerceIn(0, columns - 1)
        val startY = ((center.y - radius) / cellHeight).toInt().coerceIn(0, rows - 1)
        val endY = ((center.y + radius) / cellHeight).toInt().coerceIn(0, rows - 1)
        for (y in startY..endY) for (x in startX..endX) {
            val dx = (x + 0.5f) * cellWidth - center.x
            val dy = (y + 0.5f) * cellHeight - center.y
            val index = y * columns + x
            if (dx * dx + dy * dy <= radius * radius && !cells[index]) {
                cells[index] = true
                coveredCells++
            }
        }
    }

    fun markBetween(start: Offset, end: Offset) {
        val distance = hypot(end.x - start.x, end.y - start.y)
        val steps = ceil(distance / 7f).toInt().coerceIn(1, 256)
        for (i in 0..steps) {
            val ratio = i.toFloat() / steps
            mark(Offset(
                start.x + (end.x - start.x) * ratio,
                start.y + (end.y - start.y) * ratio
            ))
        }
    }

    Text(
        if (revealed) "السر اتكشف!" else
            "امسح على الغطا — ${(coverage * 100).toInt()}٪ اتكشف",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Box(
        Modifier.fillMaxWidth().height(265.dp).clip(RoundedCornerShape(27.dp)),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = Color(0xFF321B2B), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                // Prevent TalkBack and accessibility services from reading covered secrets.
                Text(
                    hiddenText,
                    modifier = if (revealed) Modifier else Modifier.clearAndSetSemantics {},
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        if (!revealed) Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { dimensions = it }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(dimensions) {
                    var previous: Offset? = null
                    detectDragGestures(
                        onDragStart = {
                            previous = it
                            mark(it)
                        },
                        onDrag = { change, _ ->
                            val old = previous
                            if (old == null) mark(change.position)
                            else markBetween(old, change.position)
                            previous = change.position
                            change.consume()
                        },
                        onDragEnd = { previous = null },
                        onDragCancel = { previous = null }
                    )
                }
                .pointerInput(dimensions) { detectTapGestures { mark(it) } }
        ) {
            drawRect(Brush.linearGradient(
                listOf(Color(0xFFDCB98D), Color(0xFF8C5169), Color(0xFF50283A))
            ))
            drawCircle(
                Color.White.copy(alpha = 0.1f),
                radius = size.minDimension * 0.3f,
                center = Offset(size.width * 0.83f, size.height * 0.27f)
            )
            for (i in 0..19) {
                val x = (i * 41 % 97) / 97f * size.width
                val y = (i * 73 % 97) / 97f * size.height
                drawCircle(
                    color = Color(0xFFFFEDCF).copy(alpha = if (i % 2 == 0) 0.38f else 0.2f),
                    radius = (if (i % 3 == 0) 2.3f else 1.3f).dp.toPx(),
                    center = Offset(x, y)
                )
            }
            val cellWidth = size.width / columns
            val cellHeight = size.height / rows
            cells.forEachIndexed { i, erased ->
                if (erased) drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(
                        (i % columns) * cellWidth,
                        (i / columns) * cellHeight
                    ),
                    size = Size(cellWidth + 0.5f, cellHeight + 0.5f),
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
    if (!revealed) {
        Text(
            "مش لازم تقدر تستخدم السحب علشان تعرف السر.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = { revealWithoutTouch = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("اظهر السر بدون كشط") }
    }
}
