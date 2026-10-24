package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.*
import kotlinx.coroutines.delay

@Composable
internal fun DiceScreenV2() {
    val effects=rememberGameEffects()
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    var selected by remember { mutableStateOf(Intensity.SOFT) }
    val dice = remember { mutableStateListOf<List<DieFace>>().apply { addAll(DiceCatalog.preset(Intensity.SOFT).dice) } }
    var duration by remember { mutableIntStateOf(3) }
    var advanced by remember { mutableStateOf(false) }
    var imagesFor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val chooseImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val index = imagesFor
        if (uri != null && index != null) runCatching {
            val id = vault.importPhoto(uri)
            val (die, face) = index
            vault.delete(dice[die][face].imageId)
            dice[die] = dice[die].toMutableList().also { it[face] = it[face].copy(imageId = id) }
        }
        imagesFor = null
    }
    var rolled by remember { mutableStateOf<List<Int>?>(null) }
    // Freeze the visible result at roll time; editing the dice must not rewrite past results.
    var rolledFaces by remember { mutableStateOf<List<DieFace>?>(null) }
    var rolling by remember { mutableStateOf(false) }
    var nonce by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    val config = DiceConfiguration(dice.toList(), duration, effects.preferences.sound)
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        val final = runCatching { config.roll() }.getOrElse { error = "راجع إعدادات النرد الأول"; rolling = false; return@LaunchedEffect }
        rolling = true
        repeat(if(effects.animate)duration * 8 else 0) {
            rolled = List(3) { kotlin.random.Random.nextInt(6) }
            delay(125)
        }
        rolled = final
        rolledFaces = final.mapIndexed { i, index -> config.dice[i][index] }
        rolling = false
        effects.cue(GameCue.REVEAL)
    }
    Page("سيبها على النرد", "3 نردات هتقرروا تعملوا إيه… خليك جاهز.") {
        MysteryHeader("مود الليلة", "مين قال إن الحظ بريء؟", "كل رمية بتفتح حكاية جديدة.", "◇")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("الفعل", "الفكرة", "الطريقة").forEachIndexed { index, label ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    MysteryDie((rolled?.getOrNull(index) ?: index) + 1, rolling, Modifier.fillMaxWidth())
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Text(if(rolling) "استنى المفاجأة…" else if(rolled==null) "اختار المود وارمي أول رمية." else "أهي الرمية اختارت لكم!",
            color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Intensity.entries.forEach { level ->
                FilterChip(selected == level, enabled = !rolling, onClick = {
                    selected = level
                    if (level != Intensity.CUSTOM) {
                        val preset = DiceCatalog.preset(level)
                        dice.clear(); dice.addAll(preset.dice)
                    } else {
                        val saved = DiceStorage.load(vault) ?: DiceCatalog.preset(Intensity.CUSTOM)
                        dice.clear(); dice.addAll(saved.dice)
                        duration = saved.durationSeconds
                    }
                    rolled = null; rolledFaces = null; error = ""; success = ""
                }, label = { Text(level.arabic) })
            }
        }
        Button(enabled=!rolling,onClick={error="";rolling=true;nonce++},modifier=Modifier.fillMaxWidth().height(56.dp),
            shape=RoundedCornerShape(18.dp)) {
            Text(if(rolling)"النرد بيلف…" else if(rolled==null)"ارمي النرد ✦" else "ارمي كمان مرة ✦",fontWeight=FontWeight.Bold)
        }
        OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.fillMaxWidth()) {
            Text(if(advanced)"اقفل الإعدادات" else "عايز تغيّر الوقت أو تعمل نرد مخصوص؟")
        }
        if(advanced) {
            EffectPreferencesPanel(effects)
            Text("النرد يلف قد إيه؟", color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                listOf(2,3,5,7,10,13,20).forEach { seconds ->
                    FilterChip(duration==seconds,enabled=!rolling,onClick={duration=seconds},label={Text("$seconds ث")})
                }
            }
        if (selected == Intensity.CUSTOM) {
            Text("اكتب الأوجه على مزاجك. لازم مجموع نسب كل نرد يبقى 100٪.")
            repeat(3) { die ->
                Text("النرد ${die + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                repeat(6) { face ->
                    val item = dice[die][face]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = item.text, enabled = !rolling, onValueChange = { text ->
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(text = text.take(72)) }
                        }, label = { Text("الوجه ${face + 1}") }, modifier = Modifier.weight(2f), singleLine = true)
                        OutlinedTextField(value = item.weight.toString(), enabled = !rolling, onValueChange = { number ->
                            val parsed = number.toIntOrNull()?.coerceIn(0, 100) ?: 0
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(weight = parsed) }
                        }, label = { Text("النسبة") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled=!rolling,onClick = { imagesFor = die to face; chooseImage.launch("image/*") }) { Text("صورة لهذا الوجه") }
                        if (item.imageId != null) TextButton(enabled=!rolling,onClick = {
                            vault.delete(item.imageId)
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(imageId = null) }
                        }) { Text("حذف الصورة") }
                    }
                }
                Text("المجموع: ${dice[die].sumOf { it.weight }}٪", color = if (dice[die].sumOf { it.weight } == 100) Color.LightGray else MaterialTheme.colorScheme.error)
            }
            Button(onClick = {
                runCatching { DiceStorage.save(vault, config) }.onSuccess { success = "اتحفظت إعداداتك مشفّرة على الموبايل"; error = "" }
                    .onFailure { error = "معرفناش نحفظ. اتأكد من مجموع النسب" }
            }, modifier = Modifier.fillMaxWidth(), enabled = !rolling) { Text("احفظ النرد بتاعي") }
            OutlinedButton(enabled=!rolling,onClick = {
                val preset = DiceCatalog.preset(Intensity.CUSTOM)
                dice.clear(); dice.addAll(preset.dice)
                rolled = null;rolledFaces = null;error = ""; success = ""
            }) { Text("رجّع الإعدادات الأصلية") }
        }
        }
        if (rolled != null && !rolling) {
            val outcome = requireNotNull(rolled)
            val faces = rolledFaces ?: outcome.mapIndexed { i, faceIndex -> dice[i][faceIndex] }
            ChallengeRevealCard(faces.joinToString(" ") { it.text })
            if (selected == Intensity.CUSTOM) {
                faces.forEachIndexed { index, face ->
                    val bitmap = remember(face.imageId) { vault.readPhoto(face.imageId) }
                    if (bitmap != null) {
                        Text("الصورة اللي طلعت في النرد ${index + 1}",
                            color = MaterialTheme.colorScheme.primary)
                        Image(
                            bitmap,
                            contentDescription = "الصورة المختارة للنرد ${index + 1}",
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (success.isNotBlank()) Text(success)
        Text("تقدر تقفل في أي لحظة. صورك الخاصة بتفضل مشفّرة على الموبايل.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
