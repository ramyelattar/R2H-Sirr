package com.r2h.sirr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.r2h.sirr.core.*

@Composable
internal fun ExperienceScreen(group: Boolean) {
    var roundCount by remember(group) { mutableIntStateOf(if (group) 12 else 10) }
    var variety by remember { mutableStateOf(ChallengeVariety.ONE_LEVEL) }
    var avoidRepeat by remember { mutableStateOf(true) }
    var seats by remember { mutableIntStateOf(2) }
    var intensity by remember { mutableStateOf(Intensity.SOFT) }
    val names = remember { mutableStateListOf("المشارك ١", "المشارك ٢", "المشارك ٣", "المشارك ٤") }
    val consent = remember { mutableStateListOf(false, false, false, false) }
    var game by remember { mutableStateOf<LocalGame?>(null) }
    var reply by remember { mutableStateOf("") }
    val effects=rememberGameEffects()
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    val storageName = if (group) "custom-party" else "custom-couple"
    val historyName = if (group) "recent-party" else "recent-couple"
    var customText by remember(group) { mutableStateOf(vault.readPrivate(storageName).orEmpty()) }
    var customStatus by remember { mutableStateOf("") }
    val customEntries = remember(customText) { customText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList() }
    val customValid = customEntries.size <= 60 && customEntries.all { it.length in 6..200 } &&
        customEntries.distinct().size == customEntries.size
    Page(if (group) "تحديات المجموعة" else "تحديات الشريكين", "${roundCount} جولات محلية. يستطيع كل مشارك الرفض أو الإنهاء في أي وقت.") {
        EffectPreferencesPanel(effects)
        if (game == null) {
            if (group) {
                Text("عدد المقاعد: $seats")
                Slider(value = seats.toFloat(), onValueChange = { seats = it.toInt().coerceIn(2,4) }, valueRange = 2f..4f, steps = 1)
            }
            Text("عدد الجولات", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6,10,12,18).forEach { count ->
                    FilterChip(roundCount==count,{roundCount=count},label={Text("$count")})
                }
            }
            Text("تنويع التحديات", fontWeight = FontWeight.Bold)
            ChallengeVariety.entries.forEach { option ->
                FilterChip(variety==option,{variety=option},label={Text(option.arabic)})
            }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(avoidRepeat,{avoidRepeat=it});Text("تجنب تحديات الجلسة السابقة إذا توفر بديل")
            }
            Text("اختيار مستوى التحديات", fontWeight = FontWeight.Bold)
            Intensity.entries.forEach { level ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = intensity == level, onClick = { intensity = level })
                    Text(level.arabic)
                }
            }
            if (intensity == Intensity.CUSTOM) {
                Text("تحديات مخصصة — اكتب كل تحدٍ عربي في سطر مستقل. تظهر هذه التحديات في الجلسة أولًا مع استكمال الباقي من المكتبة.")
                OutlinedTextField(customText, { customText=it.take(12_000) },
                    modifier=Modifier.fillMaxWidth(),minLines=4,maxLines=9,
                    label={Text("التحديات الخاصة (حتى ٦٠ تحديًا)")})
                Text("${customEntries.size} تحديًا — من ٦ إلى ٢٠٠ حرف للتحدي دون تكرار")
                if (!customValid) Text("تحقق من طول التحديات أو التكرارات",color=MaterialTheme.colorScheme.error)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=customValid,onClick={
                        runCatching{vault.writePrivate(storageName,customText)}
                            .onSuccess{customStatus="حُفظت تحدياتك مشفرة على هذا الجهاز"}
                            .onFailure{customStatus="تعذر حفظ التحديات"}
                    }) { Text("حفظ") }
                    TextButton(onClick={vault.deletePrivate(storageName);customText="";customStatus="حُذفت القائمة المحفوظة"}) { Text("مسح") }
                }
                if(customStatus.isNotEmpty())Text(customStatus,color=Color.LightGray)
            }
            repeat(seats) { i ->
                OutlinedTextField(value = names[i], onValueChange = { names[i] = it.take(32) }, label = { Text("اسم المشارك ${i + 1}") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(consent[i], { consent[i] = it })
                    Text("${names[i]} يوافق على المشاركة")
                }
            }
            Button(onClick = {
                val recent = if(avoidRepeat) vault.readPrivate(historyName)
                    ?.lineSequence()?.filter { it.isNotBlank() }?.toSet().orEmpty() else emptySet()
                game = SessionFactory.create(names.take(seats), intensity, group,
                    customPrompts = if(intensity==Intensity.CUSTOM)customEntries else emptyList(),
                    roundCount=roundCount, variety=variety, recentlySeen=recent)
                runCatching {
                    vault.writePrivate(historyName, game!!.turns.joinToString("\n") { it.prompt.text })
                }
                reply = ""
            }, enabled = (intensity != Intensity.CUSTOM || customValid) && (0 until seats).all { consent[it] && names[it].isNotBlank() }, modifier = Modifier.fillMaxWidth()) { Text("بدء الجلسة المحلية") }
            Text("المشاركة من هاتفين مختلفين موجودة في شاشة الجلسات عبر الإنترنت وتتطلب خادمًا خاصًا.", color = Color.LightGray)
        } else {
            val current = game!!
            if (!current.finished) {
                val turn = current.turns[current.round]
                LaunchedEffect(current.round){effects.cue(GameCue.ROUND)}
                LinearProgressIndicator(
                    progress = { current.round.toFloat() / current.turns.size },
                    modifier=Modifier.fillMaxWidth()
                )
                Text("الجولة ${current.round + 1} / ${current.turns.size}", color = MaterialTheme.colorScheme.primary)
                Text("دور: ${current.playerNames[turn.actorIndex]}", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) { Text(turn.prompt.text, Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge) }
                OutlinedTextField(reply, { reply = it.take(1000) }, label = { Text("رد نصي اختياري (يبقى داخل الجلسة)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                LocalResponseMedia(current.round)
                Button(onClick = {
                    game = current.resolve(completed = true); reply = ""
                }, modifier = Modifier.fillMaxWidth()) { Text("إكمال التحدي (+١)") }
                OutlinedButton(onClick = {
                    game = current.resolve(completed = false); reply = ""
                }, modifier = Modifier.fillMaxWidth()) { Text("تخطي دون عقوبة") }
                TextButton(onClick = { game = current.leave(); reply = "" }) { Text("إيقاف الجلسة فورًا") }
            } else {
                Text(if (current.ended) "أُوقفت الجلسة" else "اكتملت الجولات", style = MaterialTheme.typography.headlineSmall)
                current.playerNames.forEachIndexed { index, name ->
                    Text("$name — ${current.scores[index]} نقاط، ${current.skipped[index]} تخطٍ")
                }
                Button(onClick = { game = null; consent.indices.forEach { consent[it] = false } }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء جلسة جديدة") }
            }
        }
    }
}
