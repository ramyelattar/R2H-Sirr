package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
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
            SirrIntro(
                "مكعبان × 6 أوجه",
                "كل رمية بتجمع وجه من كل مكعب.",
                "المكعب البنفسجي والفضي زي الفكرة اللي اخترتها. المحتوى اللي بيظهر عليهم من إضافتك أنت.",
                accent = Color(0xFFCBA7E4),
                motif = "dice"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Top
            ) {
                repeat(2) { cube ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                            "المكعب ${cube + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            SirrAction(
                text = if (rolling) "المكعبات بتلف…" else
                    if (revealedFaces != null) "ارمي المكعبين تاني" else "ارمي المكعبين",
                enabled = !rolling && config.playable,
                explanation = if (!config.playable) "كمّل الأوجه والأوزان من زر التخصيص" else null,
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
                }
            )
            if (revealedFaces != null && !rolling) {
                SirrSection("01", "الوجهان اللي ظهروا",
                    "كل وجه نتيجة مستقلة، من غير تركيب جملة تلقائية")
                revealedFaces!!.forEachIndexed { index, face ->
                    val caption = face.text.ifBlank {
                        if (face.imageId != null) "وجه بصورة من إضافتك"
                        else "الوجه غير مخصص"
                    }
                    Text(
                        "المكعب ${index + 1}: $caption",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text("ممكن ترمي تاني أو ترفض أي نتيجة. مفيش أي تنفيذ إجباري.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedButton(
                enabled = !rolling,
                onClick = { showEditor = !showEditor },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (showEditor) "اقفل تخصيص الأوجه" else "خصص الأوجه والصور والأوزان") }

            if (showEditor) {
                SirrSection("02", "الأوجه بتاعتك", "المكعبان مستقلان. تقدر تحفظ إعدادات ناقصة وتكملها بعدين.")
                Text(
                    "لكل مكعب 6 أوجه. اكتب النص أو ضيف صورة من جهازك. لا توجد عبارات جاهزة.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                config.cubes.forEachIndexed { cube, faces ->
                    val weightSum = faces.sumOf { it.weight }
                    Text(
                        "المكعب ${cube + 1} — مجموع الأوزان: $weightSum٪",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (weightSum == 100) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    faces.forEachIndexed { index, face ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = face.text,
                                enabled = !rolling,
                                onValueChange = { value ->
                                    editFace(cube, index) { it.copy(text = value.take(72)) }
                                },
                                label = { Text("وجه ${index + 1}") },
                                modifier = Modifier.weight(3f),
                                maxLines = 2
                            )
                            OutlinedTextField(
                                value = face.weight.toString(),
                                enabled = !rolling,
                                onValueChange = { value ->
                                    val number = value.toIntOrNull()?.coerceIn(0,100) ?: 0
                                    editFace(cube,index) { it.copy(weight = number) }
                                },
                                label = { Text("الوزن ٪") },
                                modifier = Modifier.weight(1.4f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            TextButton(
                                enabled = !rolling,
                                onClick = {
                                    visibleIndices = visibleIndices.toMutableList().also {
                                        it[cube] = index
                                    }
                                    clearResult()
                                }
                            ) { Text("عاين الوجه") }
                            OutlinedButton(
                                enabled = !rolling,
                                onClick = {
                                    photoTarget = cube to index
                                    choosePhoto.launch("image/*")
                                }
                            ) {
                                Text(if (face.imageId == null) "ضيف صورة" else "غيّر الصورة")
                            }
                            if (face.imageId != null) TextButton(
                                enabled = !rolling,
                                onClick = {
                                    val oldImage = face.imageId
                                    val next = config.changeFace(cube,index,face.copy(imageId = null))
                                    clearResult()
                                    persist(next) {
                                        if (next.cubes.flatten().none { it.imageId == oldImage })
                                            vault.delete(oldImage)
                                    }
                                }
                            ) { Text("امسح الصورة") }
                        }
                    }
                }
                Text("مدة حركة المكعبين",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2,3,5,7,10,13,20).forEach { seconds ->
                        FilterChip(
                            selected = config.durationSeconds == seconds,
                            enabled = !rolling,
                            onClick = {
                                config = config.copy(durationSeconds = seconds)
                                clearResult()
                            },
                            label = { Text("$seconds ث") }
                        )
                    }
                }
                EffectPreferencesPanel(effects)
                Button(
                    onClick = { persist(config) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !rolling
                ) { Text("احفظ تخصيص المكعبين مشفّرًا") }
                OutlinedButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !rolling
                ) { Text("امسح التخصيص وابدأ من جديد") }
            }
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
}
