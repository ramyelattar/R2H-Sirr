package com.r2h.sirr

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
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
import androidx.compose.ui.viewinterop.AndroidView

/** Private local response-preview picker. No upload or file transfer to other players. */
@Composable
internal fun LocalResponseMedia(turnKey: Int) {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    var photo by remember(turnKey) { mutableStateOf<String?>(null) }
    var video by remember(turnKey) { mutableStateOf<Uri?>(null) }
    var audio by remember(turnKey) { mutableStateOf<Uri?>(null) }
    var playing by remember(turnKey) { mutableStateOf(false) }
    var mediaError by remember(turnKey) { mutableStateOf("") }
    val audioPlayer = remember(turnKey) { MediaPlayer() }
    DisposableEffect(turnKey) { onDispose { audioPlayer.release() } }
    DisposableEffect(photo) { val path = photo; onDispose { vault.delete(path) } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching { val id = vault.importPhoto(uri); vault.delete(photo); photo = id }
            .onFailure { mediaError = "تعذر قراءة الصورة" }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> video = uri }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> audio = uri }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("صورة") }
        OutlinedButton(onClick = { videoPicker.launch("video/*") }) { Text("فيديو") }
        OutlinedButton(onClick = { audioPicker.launch("audio/*") }) { Text("صوت") }
    }
    val bitmap = remember(photo) { vault.readPhoto(photo) }
    if (bitmap != null) Image(bitmap, contentDescription = "معاينة الصورة الخاصة", modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit)
    if (video != null) {
        Text("معاينة الفيديو المحلي")
        AndroidView(factory = { ctx -> VideoView(ctx).apply { setMediaController(MediaController(ctx)) } },
            update = { view -> if (view.tag != video) { view.setVideoURI(video); view.tag = video } },
            modifier = Modifier.fillMaxWidth().height(220.dp))
    }
    if (audio != null) OutlinedButton(onClick = {
        runCatching {
            if (playing) { audioPlayer.stop(); audioPlayer.reset(); playing = false }
            else { audioPlayer.reset(); audioPlayer.setDataSource(context, requireNotNull(audio)); audioPlayer.prepare(); audioPlayer.start(); playing = true }
        }.onFailure { mediaError = "تعذر تشغيل التسجيل الصوتي"; playing = false }
    }) { Text(if (playing) "إيقاف الصوت" else "معاينة الصوت") }
    if (mediaError.isNotBlank()) Text(mediaError, color = MaterialTheme.colorScheme.error)
    Text("الملفات المعروضة محلية فقط، وليست مرسلة للطرف الآخر. للصور تخزين خاص مشفر، والفيديو والصوت دون استيراد دائم.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
}
