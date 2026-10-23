package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.media.MediaPlayer
import android.net.Uri
import com.r2h.sirr.core.EphemeralEnvelope
import kotlinx.coroutines.delay

/** One-use on-device experience; this screen intentionally does not persist message text. */
@Composable
internal fun WhispersScreenV2() {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    val messages = remember { mutableStateListOf("", "", "") }
    var sender by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var ttlHours by remember { mutableIntStateOf(3) }
    var photoId by remember { mutableStateOf<String?>(null) }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    DisposableEffect(photoId) { val activeId = photoId; onDispose { vault.delete(activeId) } }
    var envelope by remember { mutableStateOf<EphemeralEnvelope?>(null) }
    var error by remember { mutableStateOf("") }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val newId = vault.importPhoto(uri)
            vault.delete(photoId); photoId = newId; error = ""
        }.onFailure { error = "تعذر إدراج الصورة المحددة" }
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        audioUri = uri?.toString() // Session-scoped only. Media will not be copied/uploaded.
    }
    fun clear() {
        messages.indices.forEach { messages[it] = "" }
        sender = ""; receiver = ""; signature = ""; page = -1; envelope = null
        runCatching { player.reset() }; playing = false
        vault.delete(photoId); photoId = null; audioUri = null; error = ""
    }
    LaunchedEffect(envelope, page) {
        val current = envelope ?: return@LaunchedEffect
        if (page < 0) return@LaunchedEffect
        val remaining = current.expiresAtMillis - System.currentTimeMillis()
        if (remaining > 0) delay(remaining)
        clear()
    }
    Page("الهمسات الخاصة", "ثلاث رسائل وصورة وتوقيع. تُعرض مرة واحدة محليًا مع انتهاء صلاحية تلقائي.") {
        if (page == -1) {
            OutlinedTextField(sender, { sender = it.take(35) }, label = { Text("اسم المُرسل (اختياري)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(receiver, { receiver = it.take(35) }, label = { Text("اسم المُستقبل (اختياري)") }, modifier = Modifier.fillMaxWidth())
            repeat(3) { index ->
                OutlinedTextField(messages[index], { messages[index] = it.take(350) }, label = { Text("الهمسة ${index + 1}") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            OutlinedTextField(signature, { signature = it.take(80) }, label = { Text("التوقيع (اختياري)") }, modifier = Modifier.fillMaxWidth())
            Text("تنتهي صلاحية العرض بعد:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3,6,24).forEach { value -> FilterChip(ttlHours == value, onClick = { ttlHours = value }, label = { Text("$value ساعات") }) }
            }
            OutlinedButton(onClick = { pickPhoto.launch("image/*") }) { Text(if (photoId == null) "إضافة صورة ختامية" else "تغيير الصورة") }
            if (photoId != null) TextButton(onClick = { vault.delete(photoId); photoId = null }) { Text("حذف الصورة") }
            OutlinedButton(onClick = { pickAudio.launch("audio/*") }) { Text("اختيار ملف موسيقى محلي (دون تشغيل آلي)") }
            if (audioUri != null) {
                OutlinedButton(onClick = {
                    runCatching {
                        if (playing) { player.stop(); player.reset(); playing = false }
                        else { player.reset(); player.setDataSource(context, Uri.parse(requireNotNull(audioUri))); player.prepare(); player.start(); playing = true }
                    }.onFailure { error = "تعذر تشغيل الملف الصوتي"; playing = false }
                }) { Text(if (playing) "إيقاف معاينة الصوت" else "معاينة الموسيقى المحلية") }
                Text("الموسيقى للمعاينة داخل الهاتف فقط، ولا تُشارك عن بُعد.", color = Color.LightGray)
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Button(enabled = messages.all { it.isNotBlank() }, onClick = {
                val now = System.currentTimeMillis()
                envelope = EphemeralEnvelope(now, now + ttlHours * 3_600_000L)
                page = 0
            }, modifier = Modifier.fillMaxWidth()) { Text("بدء العرض مرة واحدة") }
        } else if (page < 3) {
            if (receiver.isNotBlank()) Text("إلى $receiver", color = MaterialTheme.colorScheme.primary)
            Text("الرسالة ${page + 1} من ٣")
            Card(Modifier.fillMaxWidth()) { Text(messages[page], Modifier.padding(25.dp), style = MaterialTheme.typography.titleLarge) }
            Button(onClick = {
                messages[page] = "" // Destroy each message immediately after advancing.
                page++
            }) { Text(if (page == 2) "إظهار الخاتمة" else "الرسالة التالية") }
            TextButton(onClick = { clear() }) { Text("إلغاء ومسح المحتوى") }
        } else {
            Text("انتهى العرض", style = MaterialTheme.typography.titleLarge)
            if (sender.isNotBlank()) Text("من: $sender")
            if (signature.isNotBlank()) Text(signature)
            val photo = remember(photoId) { vault.readPhoto(photoId) }
            if (photo != null) Image(photo, contentDescription = "الصورة الختامية", modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit)
            Text("عند الضغط على الإغلاق، تُمسح كل بيانات هذه التجربة من الذاكرة المحلية.", color = Color.LightGray)
            Button(onClick = { clear() }) { Text("إغلاق ومسح نهائي") }
        }
    }
}
