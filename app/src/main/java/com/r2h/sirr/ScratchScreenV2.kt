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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
internal fun ScratchScreenV2() {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    var stages by remember { mutableIntStateOf(1) }
    val texts = remember { mutableStateListOf("رسالة خاصة تظهر بعد الكشف", "مفاجأة ثانية", "وهذه هي الخاتمة") }
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
    fun close() { editing = true; stage = 0; stageRevealed = false; reset++; expiresAt = 0; vault.delete(photoId); photoId = null }
    LaunchedEffect(editing, expiresAt) {
        if (!editing && expiresAt > 0) {
            val wait = expiresAt - System.currentTimeMillis()
            if (wait > 0) kotlinx.coroutines.delay(wait)
            close()
        }
    }
    Page("اكشف اللي مستخبي", "امسح الغطا بإيدك… كل طبقة وراها حاجة مستخبية.") {
        if (editing) {
            Text("هتخبّي كام مفاجأة؟")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..3).forEach { n -> FilterChip(stages == n, onClick = { stages = n }, label = { Text("$n") }) }
            }
            repeat(stages) { i ->
                OutlinedTextField(texts[i], { texts[i] = it.take(280) }, label = { Text("السر المستخبي ${i+1}") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            OutlinedButton(onClick = { pickPhoto.launch("image/*") }) { Text("حط صورة في الآخر") }
            if (photoId != null) TextButton(onClick = { vault.delete(photoId); photoId = null }) { Text("امسح الصورة") }
            Text("انتهاء العرض:")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(3, 6, 24).forEach { hours -> FilterChip(ttlHours == hours, onClick = { ttlHours = hours }, label = { Text("$hours ساعات") }) }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Button(enabled = texts.take(stages).all { it.isNotBlank() }, onClick = {
                editing = false; stage = 0; stageRevealed = false; reset++
                expiresAt = System.currentTimeMillis() + ttlHours * 3_600_000L
            }, modifier = Modifier.fillMaxWidth()) { Text("خبّي المفاجأة") }
        } else {
            Text("الطبقة ${stage + 1} من $stages")
            key(reset, stage) {
                ScratchPanel(texts[stage]) { stageRevealed = true }
            }
            if (stage < stages-1) {
                Button(onClick = { stage++; stageRevealed = false; reset++ }, enabled = stageRevealed, modifier = Modifier.fillMaxWidth()) { Text("اكشف اللي بعدها") }
            } else {
                val photo = remember(photoId) { vault.readPhoto(photoId) }
                if (photo != null && stageRevealed) Image(photo, contentDescription = "المفاجأة الأخيرة", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
            }
            OutlinedButton(onClick = { stageRevealed = false; reset++ }, modifier = Modifier.fillMaxWidth()) { Text("خبّيها تاني") }
            TextButton(onClick = { close() }) { Text("اقفل وامسح الصورة") }
        }
    }
}

@Composable
internal fun ScratchPanel(hiddenText: String, onRevealed: () -> Unit) {
    val points = remember { mutableStateListOf<Offset>() }
    val touched = remember { mutableStateMapOf<Int, Boolean>() }
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    var alreadyRevealed by remember { mutableStateOf(false) }
    val coverage = touched.size / 120f
    val revealed = coverage >= 0.55f
    LaunchedEffect(revealed) { if (revealed && !alreadyRevealed) { alreadyRevealed = true; onRevealed() } }
    fun touch(p: Offset) {
        val w = dimensions.width.toFloat(); val h = dimensions.height.toFloat()
        if (w <= 0 || h <= 0 || p.x !in 0f..w || p.y !in 0f..h) return
        if (points.size < 2300) points.add(p)
        val col = (p.x / w * 12).toInt().coerceIn(0,11)
        val row = (p.y / h * 10).toInt().coerceIn(0,9)
        for (dx in -1..1) for (dy in -1..1) {
            val x = col + dx; val y = row + dy
            if (x in 0..11 && y in 0..9) touched[y*12+x] = true
        }
    }
    Text("تم كشف ${(coverage * 100).toInt().coerceAtMost(100)}٪ — استمر حتى ٥٥٪")
    Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF312B40), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                Text(hiddenText, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            }
        }
        if (!revealed) Canvas(Modifier.fillMaxSize()
            .onSizeChanged { dimensions = it }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { touch(it) }, onDrag = { change, _ -> touch(change.position); change.consume() }) }
            .pointerInput(Unit) { detectTapGestures { touch(it) } }
        ) {
            drawRect(Brush.linearGradient(listOf(Color(0xFFD0B58B), Color(0xFF65547C))))
            drawPoints(points = points.toList(), pointMode = PointMode.Points, color = Color.Transparent,
                strokeWidth = 48.dp.toPx(), cap = StrokeCap.Round, blendMode = BlendMode.Clear)
        }
    }
    if (revealed) Text("بان السر!", color = MaterialTheme.colorScheme.primary)
}
