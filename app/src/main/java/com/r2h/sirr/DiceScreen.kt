package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.*
import kotlinx.coroutines.delay
import android.media.ToneGenerator
import android.media.AudioManager

@Composable
internal fun DiceScreenV2() {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    var selected by remember { mutableStateOf(Intensity.SOFT) }
    val dice = remember { mutableStateListOf<List<DieFace>>().apply { addAll(DiceCatalog.preset(Intensity.SOFT).dice) } }
    var duration by remember { mutableIntStateOf(7) }
    var sound by remember { mutableStateOf(false) }
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
    var rolling by remember { mutableStateOf(false) }
    var nonce by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    val config = DiceConfiguration(dice.toList(), duration, sound)
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        val final = runCatching { config.roll() }.getOrElse { error = "يجب ضبط الأوجه والاحتمالات قبل الرمي"; rolling = false; return@LaunchedEffect }
        rolling = true
        repeat(duration * 8) {
            rolled = List(3) { kotlin.random.Random.nextInt(6) }
            delay(125)
        }
        rolled = final
        rolling = false
        if (sound) {
            val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull()
            if (tone != null) try {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(150)
            } finally { tone.release() }
        }
    }
    Page("النرد التفاعلي", "ستة أوضاع، ثلاثة نرود، احتمالات مستقلة، صور اختيارية وإعدادات محلية مشفرة.") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Intensity.entries.forEach { level ->
                FilterChip(selected == level, onClick = {
                    selected = level
                    if (level != Intensity.CUSTOM) {
                        val preset = DiceCatalog.preset(level)
                        dice.clear(); dice.addAll(preset.dice)
                    } else {
                        val saved = DiceStorage.load(vault) ?: DiceCatalog.preset(Intensity.CUSTOM)
                        dice.clear(); dice.addAll(saved.dice)
                        duration = saved.durationSeconds; sound = saved.sound
                    }
                    rolled = null; error = ""; success = ""
                }, label = { Text(level.arabic) })
            }
        }
        Text("مدة الدوران", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(5, 7, 10, 13, 20).forEach { s ->
                FilterChip(duration == s, onClick = { duration = s }, label = { Text("$s ث") })
            }
        }
        Row { Checkbox(sound, onCheckedChange = { sound = it }); Text("صوت قصير عند نهاية الرمية") }
        if (selected == Intensity.CUSTOM) {
            Text("تعديل ١٨ وجهًا. مجموع أوزان كل نرد يجب أن يساوي ١٠٠٪.")
            repeat(3) { die ->
                Text("النرد ${die + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                repeat(6) { face ->
                    val item = dice[die][face]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = item.text, onValueChange = { text ->
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(text = text.take(72)) }
                        }, label = { Text("الوجه ${face + 1}") }, modifier = Modifier.weight(2f), singleLine = true)
                        OutlinedTextField(value = item.weight.toString(), onValueChange = { number ->
                            val parsed = number.toIntOrNull()?.coerceIn(0, 100) ?: 0
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(weight = parsed) }
                        }, label = { Text("النسبة") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imagesFor = die to face; chooseImage.launch("image/*") }) { Text("صورة لهذا الوجه") }
                        if (item.imageId != null) TextButton(onClick = {
                            vault.delete(item.imageId)
                            dice[die] = dice[die].toMutableList().also { it[face] = item.copy(imageId = null) }
                        }) { Text("حذف الصورة") }
                    }
                }
                Text("المجموع: ${dice[die].sumOf { it.weight }}٪", color = if (dice[die].sumOf { it.weight } == 100) Color.LightGray else MaterialTheme.colorScheme.error)
            }
            Button(onClick = {
                runCatching { DiceStorage.save(vault, config) }.onSuccess { success = "حُفظ الإعداد المخصص مشفرًا على الهاتف"; error = "" }
                    .onFailure { error = "لم تُحفظ الإعدادات؛ راجع مجموع الاحتمالات" }
            }, modifier = Modifier.fillMaxWidth(), enabled = !rolling) { Text("حفظ التخصيص") }
            OutlinedButton(onClick = {
                val preset = DiceCatalog.preset(Intensity.CUSTOM)
                dice.clear(); dice.addAll(preset.dice)
                error = ""; success = ""
            }) { Text("استعادة القيم الافتراضية") }
        }
        rolled?.forEachIndexed { i, selectedFace ->
            val face = dice[i][selectedFace]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("النرد ${i + 1}", color = MaterialTheme.colorScheme.primary)
                    Text(face.text, style = MaterialTheme.typography.titleLarge)
                    val bitmap = remember(face.imageId) { vault.readPhoto(face.imageId) }
                    if (bitmap != null) Image(bitmap, contentDescription = "صورة الوجه", modifier = Modifier.fillMaxWidth().height(125.dp), contentScale = ContentScale.Fit)
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (success.isNotBlank()) Text(success)
        Button(enabled = !rolling, onClick = { error = ""; nonce++ }, modifier = Modifier.fillMaxWidth()) {
            Text(if (rolling) "النرد يدور..." else "رمية جديدة")
        }
        Text("يمكن إيقاف الرمية بمغادرة الصفحة. الصور محفوظة مشفرة محليًا في وضع التخصيص فقط.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
    }
}
