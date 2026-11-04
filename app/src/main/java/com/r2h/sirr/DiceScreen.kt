package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.PersonalDiceConfiguration
import com.r2h.sirr.core.PersonalDiceFace

/**
 * Two dice matching the physical reference concept.
 * ALL 12 faces are deliberately blank until authored by the user.
 * The original three-dice experience lives in LegacyDiceScreen().
 */
@Composable
internal fun DiceScreenV2() {
    val context = LocalContext.current
    val vault = remember(context) { LocalVault(context.applicationContext) }
    val effects = rememberGameEffects()
    val restore = remember(vault) { PersonalDiceStore.load(vault) }
    var config by remember(vault) {
        mutableStateOf(restore.configuration ?: PersonalDiceConfiguration.blank())
    }
    var saved by remember(vault) { mutableStateOf(config) }
    var unreadable by remember(vault) { mutableStateOf(restore.unreadable) }
    var showLegacy by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editorCube by remember { mutableIntStateOf(0) }
    var editorFace by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    var photoTarget by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val progress = remember { Animatable(1f) }
    var rollNumber by remember { mutableIntStateOf(0) }
    var rolling by remember { mutableStateOf(false) }
    var visibleIndices by remember { mutableStateOf(listOf(0,0)) }
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
    fun persist(next: PersonalDiceConfiguration, done: () -> Unit = {}) {
        runCatching { PersonalDiceStore.save(vault, next) }
            .onSuccess {
                config = next
                saved = next
                notice = "تم حفظ الأوجه في التخزين المحلي المشفّر"
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
                val old = config.cubes[cube][faceIndex].imageId
                importedId = vault.importPhoto(uri)
                val next = config.changeFace(cube, faceIndex,
                    config.cubes[cube][faceIndex].copy(imageId = importedId))
                // Save before deleting the previous photo; rollback a failed import.
                PersonalDiceStore.save(vault, next)
                config = next
                saved = next
                clearResult()
                notice = "تم حفظ الصورة على وجه المكعب"
                if (old != null && next.cubes.flatten().none { it.imageId == old }) {
                    vault.delete(old)
                }
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
            progress.snapTo(0f)
            if (effects.animate) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = config.durationSeconds * 1000,
                        easing = FastOutSlowInEasing
                    )
                )
            } else progress.snapTo(1f)
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
        ) { Text("ارجع للمكعبين الجديدين") }
        LegacyDiceScreen()
        return
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("تمسح الأوجه اللي كتبتها؟") },
            text = { Text("هيتمسح تخصيص المكعبين والصور الخاصة بيهم فقط. وضع الـ3 نردات القديم مش هيتغير.") },
            confirmButton = {
                TextButton(onClick = {
                    val previousIds = (config.cubes + saved.cubes).flatten()
                        .mapNotNull { it.imageId }.toSet()
                    runCatching { PersonalDiceStore.clear(vault) }
                        .onSuccess {
                            previousIds.forEach(vault::delete)
                            val blank = PersonalDiceConfiguration.blank()
                            config = blank
                            saved = blank
                            unreadable = false
                            clearResult()
                            notice = "اتمسحت أوجه المكعبين"
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

    Page("مكعباتك إنت", "مكعبين ثلاثيي الأبعاد — اكتب أو ارفع محتوى كل وجه بنفسك.") {
        if (unreadable) {
            SirrSection("!", "إعدادات قديمة مش مقروءة",
                "مش هنكتب فوق بياناتك المحفوظة من غير اختيار واضح.")
            Text(
                "ممكن تسيب البيانات وتجرّب تفتح التطبيق تاني، أو تمسح إعدادات المكعبين فقط.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth()) {
                Text("مسح إعدادات المكعبين غير المقروءة")
            }
        } else {
            Text(
                "DICE STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC9A0DE),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF100D17),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFF342C3C)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { cube ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PersonalCube3D(
                                    faces = config.cubes[cube],
                                    visibleFace = visibleIndices[cube],
                                    spinProgress = progress.value,
                                    metallic = cube == 1,
                                    vault = vault,
                                    label = "المكعب ${cube + 1}",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                )
                                Text(
                                    if (cube == 0) "SATIN LILAC" else "PEARL SILVER",
                                    color = if (cube == 0) Color(0xFFC9A0DE)
                                    else Color(0xFFE2E3E8),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
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
                    containerColor = Color(0xFFC9A0DE),
                    contentColor = Color(0xFF120B19),
                    disabledContainerColor = Color(0xFF332A3B),
                    disabledContentColor = Color(0xFFAFA4B6)
                )
            ) {
                Text(
                    when {
                        rolling -> "المكعبات بتلف…"
                        revealedFaces != null -> "ارمِ المكعبين تاني"
                        else -> "ارمِ المكعبين"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (revealedFaces != null && !rolling) {
                Text(
                    "النتيجة",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF8F5FA)
                )
                revealedFaces!!.forEachIndexed { index, face ->
                    Surface(
                        color = Color(0xFF191421),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFF342C3C)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "المكعب ${index + 1}",
                                color = if (index == 0) Color(0xFFC9A0DE)
                                    else Color(0xFFE2E3E8),
                                style = MaterialTheme.typography.labelLarge
                            )
                            val photo = remember(vault, face.imageId) {
                                vault.readPhotoThumbnail(face.imageId)
                            }
                            if (photo != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = photo,
                                    contentDescription = "الصورة اللي أضفتها للوجه",
                                    modifier = Modifier.fillMaxWidth()
                                        .height(168.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            if (face.text.isNotBlank()) {
                                Text(
                                    face.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFF8F5FA)
                                )
                            } else if (photo != null) {
                                Text("وجه بصورة من إضافتك", color = Color(0xFFB9B0C2))
                            }
                        }
                    }
                }
                Text(
                    "تقدر ترفض أي نتيجة أو تعيد الرمية. مفيش تنفيذ إجباري.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB9B0C2)
                )
            }

            OutlinedButton(
                enabled = !rolling,
                onClick = { showEditor = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6E5B78))
            ) { Text("Face Studio — تخصيص الأوجه", color = Color(0xFFE2E3E8)) }

            HorizontalDivider()
            OutlinedButton(
                onClick = { showLegacy = true },
                enabled = !rolling && config == saved,
                modifier = Modifier.fillMaxWidth()
            ) { Text("افتح وضع الـ3 نردات القديم") }
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
    // the gameplay stage. Drafts are still saved with PersonalDiceStore.
    if (showEditor && !unreadable) {
        ModalBottomSheet(
            onDismissRequest = { showEditor = false },
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
                Text("خصص أوجه المكعبين",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { cube ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = editorCube == cube,
                            onClick = { editorCube = cube },
                            label = {
                                Text(if (cube == 0) "المكعب الليلكي" else "المكعب الفضي")
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
                    metallic = editorCube == 1,
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
                                val oldImage = active.imageId
                                val next = config.changeFace(
                                    editorCube, editorFace, active.copy(imageId = null)
                                )
                                clearResult()
                                persist(next) {
                                    if (next.cubes.flatten().none { it.imageId == oldImage })
                                        vault.delete(oldImage)
                                }
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
                ) { Text("امسح تخصيص المكعبين") }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFFACAC))
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

}
