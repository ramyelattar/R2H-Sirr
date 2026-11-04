package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.TripleDiceConfiguration
import com.r2h.sirr.core.PersonalDiceFace

/** Three 3D cubes / 18 user-authored faces, with an independent legacy mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiceScreenV2() {
    val context = LocalContext.current
    val vault = remember(context) { LocalVault(context.applicationContext) }
    val effects = rememberGameEffects()
    val restore = remember(vault) { TripleDiceStore.load(vault) }
    var config by remember(vault) {
        mutableStateOf(restore.configuration ?: TripleDiceConfiguration.blank())
    }
    var saved by remember(vault) { mutableStateOf(config) }
    var unreadable by remember(vault) { mutableStateOf(restore.unreadable) }
    var migratedFromPair by remember(vault) { mutableStateOf(restore.fromOldPair) }
    var showLegacy by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editorCube by remember { mutableIntStateOf(0) }
    var editorFace by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmCloseEditor by remember { mutableStateOf(false) }
    var photoTarget by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val progress = remember { List(TripleDiceConfiguration.CUBE_COUNT) { Animatable(1f) } }
    var rollNumber by remember { mutableIntStateOf(0) }
    var rolling by remember { mutableStateOf(false) }
    var visibleIndices by remember { mutableStateOf(List(TripleDiceConfiguration.CUBE_COUNT) { 0 }) }
    var revealedFaces by remember { mutableStateOf<List<PersonalDiceFace>?>(null) }
    var pendingFaces by remember { mutableStateOf<List<PersonalDiceFace>?>(null) }

    fun clearResult() {
        revealedFaces = null
        pendingFaces = null
        error = ""
        notice = ""
    }
    fun editFace(cube: Int, index: Int, transform: (PersonalDiceFace) -> PersonalDiceFace) {
        val face = config.cubes[cube][index]
        config = config.changeFace(cube, index, transform(face))
        clearResult()
    }
    fun persist(next: TripleDiceConfiguration, done: () -> Unit = {}) {
        runCatching { TripleDiceStore.save(vault, next) }
            .onSuccess {
                config = next
                saved = next
                migratedFromPair = false
                notice = "اتحفظ تخصيص المكعبات الثلاثة محليًا ومشفّرًا"
                error = ""
                done()
            }
            .onFailure { error = "تعذر حفظ الأوجه. الإعدادات القديمة لم تُستبدل." }
    }

    val choosePhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = photoTarget
        photoTarget = null
        if (uri != null && target != null && !rolling && !unreadable) {
            var importedId: String? = null
            runCatching {
                val (cube, faceIndex) = target
                importedId = vault.importPhoto(uri)
                val next = config.changeFace(cube, faceIndex,
                    config.cubes[cube][faceIndex].copy(imageId = importedId))
                // Pair photos may still be referenced by the independent old vault.
                // Never delete them as part of updating the new triple.
                TripleDiceStore.save(vault, next)
                config = next
                saved = next
                migratedFromPair = false
                clearResult()
                notice = "اتحفظت الصورة على وجه المكعب"
            }.onFailure {
                vault.delete(importedId)
                error = "تعذر إضافة الصورة وحفظها. لم يتم استبدال الوجه القديم."
            }
        }
    }

    LaunchedEffect(rollNumber) {
        if (rollNumber == 0) return@LaunchedEffect
        val faces = pendingFaces ?: return@LaunchedEffect
        try {
            coroutineScope {
                progress.forEachIndexed { cube, animation ->
                    launch {
                        animation.snapTo(0f)
                        if (effects.animate) {
                            delay(cube * 65L)
                            val scale = 0.88f + cube * 0.06f
                            animation.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = (config.durationSeconds * 1000 * scale).toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                        } else animation.snapTo(1f)
                    }
                }
            }
            revealedFaces = faces
            effects.cue(GameCue.REVEAL)
        } finally {
            rolling = false
        }
    }

    if (showLegacy) {
        TextButton(
            onClick = { showLegacy = false },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ارجع للمكعبات الثلاثة") }
        LegacyDiceScreen()
        return
    }

    if (confirmCloseEditor) {
        AlertDialog(
            onDismissRequest = { confirmCloseEditor = false },
            title = { Text("في تغييرات مش محفوظة") },
            text = { Text("تقدر تكمل التعديل، أو تتجاهل التغييرات اللي لسه ما اتحفظتش.") },
            confirmButton = {
                TextButton(onClick = {
                    config = saved
                    clearResult()
                    showEditor = false
                    confirmCloseEditor = false
                }) { Text("تجاهل التغييرات") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloseEditor = false }) {
                    Text("كمل التعديل")
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("تمسح الأوجه اللي كتبتها؟") },
            text = { Text("هيتمسح تخصيص المكعبات الثلاثة الجديد فقط. النسخة القديمة للمكعبين ووضع النرد الكلاسيكي هيفضلوا محفوظين.") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { TripleDiceStore.resetToBlank(vault) }
                        .onSuccess {
                            val blank = TripleDiceConfiguration.blank()
                            config = blank
                            saved = blank
                            unreadable = false
                            migratedFromPair = false
                            clearResult()
                            notice = "اتمسح تخصيص الثلاثة الجديد مع الاحتفاظ ببيانات المكعبين القديمة"
                        }
                        .onFailure { error = "تعذر مسح الإعدادات. جرّب تاني." }
                    confirmClear = false
                }) { Text("امسح الأوجه") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("إلغاء") }
            }
        )
    }

    Page("مكعباتك", "٣ مكعبات، ١٨ وجه — كل التفاصيل من اختيارك.") {
        if (unreadable) {
            SirrSection("!", "إعدادات قديمة مش مقروءة",
                "مش هنكتب فوق بياناتك المحفوظة من غير اختيار واضح.")
            Text(
                "النسخة المحفوظة مش مقروءة. تقدر تبدأ تخصيص الثلاثة من جديد من غير حذف بيانات المكعبين القديمة.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth()) {
                Text("ابدأ تخصيص الثلاثة من جديد")
            }
        } else {
            Text(
                "TRIPLE DICE • PRIVATE STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC9A0DE),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0C0A12),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C303E))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TripleDiceStage3D(
                        cubes = config.cubes,
                        selected = visibleIndices,
                        progress = progress.map { it.value },
                        vault = vault,
                        modifier = Modifier.fillMaxWidth().height(314.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { cube ->
                            val material = DiceMaterial.forCube(cube)
                            Text(
                                material.label,
                                color = Color(material.accent),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            if (migratedFromPair) {
                Text(
                    "تم استرجاع تخصيص أول مكعبين من النسخة السابقة. الثالث فاضي لحد ما تخصصه، والنسخة الأصلية محفوظة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD9CAB0)
                )
            }

            val remaining = config.cubes.sumOf { cube ->
                cube.count { it.weight > 0 && !it.configured }
            }
            if (!config.playable) {
                Text(
                    if (remaining > 0)
                        "لسه في $remaining وجه محتاج نص أو صورة. الأوجه اللي وزنها صفر مش بتدخل السحب."
                    else
                        "لازم مجموع أوزان كل مكعب يبقى 100٪ علشان يبدأ الرمي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (config != saved) {
                Text(
                    "في تغييرات غير محفوظة. احفظها قبل ما تخرج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Button(
                onClick = {
                    val pick = runCatching { config.roll() }
                        .onFailure { error = "راجع الأوجه والأوزان الأول" }.getOrNull()
                    if (pick != null) {
                        visibleIndices = pick
                        pendingFaces = pick.mapIndexed { cube, face ->
                            config.cubes[cube][face]
                        }
                        revealedFaces = null
                        rolling = true
                        error = ""
                        rollNumber++
                    }
                },
                enabled = !rolling && config.playable,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF90647E),
                    contentColor = Color(0xFFFDF7FB),
                    disabledContainerColor = Color(0xFF302732),
                    disabledContentColor = Color(0xFFA99BAA)
                )
            ) {
                Text(
                    when {
                        rolling -> "المكعبات بتلف…"
                        revealedFaces != null -> "ارمِ المكعبات تاني"
                        else -> "ارمِ المكعبات"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (revealedFaces != null && !rolling) {
                Text(
                    "النتائج الثلاثة",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF8F5FA)
                )
                revealedFaces!!.forEachIndexed { index, face ->
                    val accent = Color(DiceMaterial.forCube(index).accent)
                    val photo = remember(vault, face.imageId) {
                        vault.readPhotoThumbnail(face.imageId)
                    }
                    Surface(
                        color = Color(0xFF16111D),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFF332A38)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.width(3.dp).height(51.dp)
                                    .background(accent, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "المكعب ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                                if (face.text.isNotBlank()) {
                                    Text(
                                        face.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFF8F5FA)
                                    )
                                } else if (photo != null) {
                                    Text(
                                        "وجه بصورة من إضافتك",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFC5BAC9)
                                    )
                                }
                            }
                            if (photo != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = photo,
                                    contentDescription = "صورة الوجه اللي أضفتها",
                                    modifier = Modifier.size(65.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                enabled = !rolling,
                onClick = { showEditor = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6E5B78))
            ) { Text("Face Studio — ١٨ وجه", color = Color(0xFFE2E3E8)) }

            HorizontalDivider()
            OutlinedButton(
                onClick = { showLegacy = true },
                enabled = !rolling && config == saved,
                modifier = Modifier.fillMaxWidth()
            ) { Text("افتح وضع النرد الكلاسيكي") }
            if (config != saved) Text(
                "احفظ التعديلات علشان تقدر تبدّل بين الوضعين.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.primary)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Text(
                "الصور والأوجه اللي تضيفها بتتحفظ محليًا ومشفّرة. لا يوجد رفع تلقائي أو محتوى جاهز.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    // Editing is a dedicated, focused sheet; never a twelve-row form below
    // the gameplay stage. Drafts use the independent triple vault.
    if (showEditor && !unreadable) {
        ModalBottomSheet(
            onDismissRequest = {
                if (config != saved) confirmCloseEditor = true
                else showEditor = false
            },
            containerColor = Color(0xFF120F1A),
            contentColor = Color(0xFFF8F5FA),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = 28.dp, topEnd = 28.dp
            )
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("FACE STUDIO", color = Color(0xFFC9A0DE),
                    style = MaterialTheme.typography.labelLarge)
                Text("خصص أوجه المكعبات الثلاثة",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(TripleDiceConfiguration.CUBE_COUNT) { cube ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = editorCube == cube,
                            onClick = { editorCube = cube },
                            label = {
                                Text(when (cube) {
                                    0 -> "Plum"
                                    1 -> "Rose"
                                    else -> "Champagne"
                                })
                            }
                        )
                    }
                }

                val activeCube = config.cubes[editorCube]
                repeat(2) { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { column ->
                            val index = row * 3 + column
                            val face = activeCube[index]
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = editorFace == index,
                                onClick = { editorFace = index },
                                label = {
                                    Text(
                                        "وجه ${index + 1}" +
                                            if (face.configured) " ✓" else ""
                                    )
                                }
                            )
                        }
                    }
                }

                PersonalCube3D(
                    faces = activeCube,
                    visibleFace = editorFace,
                    spinProgress = 1f,
                    material = DiceMaterial.forCube(editorCube),
                    rollStyle = editorCube,
                    vault = vault,
                    label = "معاينة المكعب ${editorCube + 1}",
                    modifier = Modifier.size(176.dp).align(Alignment.CenterHorizontally)
                )

                val active = activeCube[editorFace]
                OutlinedTextField(
                    value = active.text,
                    onValueChange = { value ->
                        editFace(editorCube, editorFace) {
                            it.copy(text = value.take(72))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("النص اللي هتكتبه للوجه") },
                    minLines = 2,
                    maxLines = 3,
                    supportingText = { Text("${active.text.length} / 72") }
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoTarget = editorCube to editorFace
                            choosePhoto.launch("image/*")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (active.imageId == null) "إضافة صورة" else "تغيير الصورة")
                    }
                    if (active.imageId != null) {
                        TextButton(
                            onClick = {
                                // Do not delete photo bytes: the two-cube vault
                                // may still independently reference this image.
                                val next = config.changeFace(
                                    editorCube, editorFace, active.copy(imageId = null)
                                )
                                clearResult()
                                persist(next)
                            }
                        ) { Text("حذف الصورة") }
                    }
                }

                val sum = activeCube.sumOf { it.weight }
                Text(
                    "وزن الوجه: ${active.weight}% — مجموع المكعب: ${sum}%",
                    color = if (sum == 100) Color(0xFFC9A0DE) else Color(0xFFFFACAC),
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = active.weight.toFloat(),
                    onValueChange = { value ->
                        editFace(editorCube, editorFace) {
                            it.copy(weight = value.toInt().coerceIn(0, 100))
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        config = config.copy(cubes = config.cubes.mapIndexed { cube, faces ->
                            if (cube != editorCube) faces
                            else faces.mapIndexed { index, face ->
                                face.copy(weight = com.r2h.sirr.core.GameLogic.evenWeights[index])
                            }
                        })
                        clearResult()
                    }
                ) { Text("توزيع الأوزان بالتساوي للمكعب ده") }

                Text("مدة الحركة", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 5, 7, 10, 13, 20).forEach { seconds ->
                        FilterChip(
                            selected = config.durationSeconds == seconds,
                            onClick = {
                                config = config.copy(durationSeconds = seconds)
                                clearResult()
                            },
                            label = { Text("$seconds ث") }
                        )
                    }
                }
                EffectPreferencesPanel(effects)

                if (config != saved) {
                    Text(
                        "التغييرات لسه غير محفوظة. تقدر تحفظ مسودة حتى لو الأوجه مش مكتملة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E3E8)
                    )
                }
                Button(
                    onClick = {
                        persist(config) { showEditor = false }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC9A0DE),
                        contentColor = Color(0xFF120B19)
                    )
                ) { Text("احفظ التخصيص المشفر") }
                OutlinedButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("امسح تخصيص المكعبات الثلاثة") }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFFACAC))
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

}
