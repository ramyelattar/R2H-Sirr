package com.r2h.sirr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

internal val LocalSirrAudioRefresh = compositionLocalOf<() -> Unit> { {} }
internal val LocalSirrUserAudioOverride = compositionLocalOf<(Boolean) -> Unit> { {} }
internal val LocalSirrMusicStatus = compositionLocalOf<String> { "" }

internal data class SirrMusicPreferences(
    val enabled: Boolean = true,
    val volume: Int = 55,
    val cinematic: Boolean = true
) {
    companion object {
        fun load(context: Context): SirrMusicPreferences = runCatching {
            val data = LocalVault(context.applicationContext)
                .readPrivate("game-effects")?.let(::JSONObject)
            SirrMusicPreferences(
                enabled = data?.optBoolean("music", true) ?: true,
                volume = (data?.optInt("musicVolume", 55) ?: 55).coerceIn(0, 100),
                cinematic = data?.optBoolean("cinematic", true) ?: true
            )
        }.getOrDefault(SirrMusicPreferences())
    }
}

/** MediaPlayer plays real, packaged WAV files with clear audio diagnostics.
 * There is one player across navigation, and no audio network or microphone calls.
 */
internal class SirrPageMusic(context: Context) {
    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> resumeIfAllowed()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                runCatching { player?.setVolume(gain * .20f, gain * .20f) }
            else -> {
                runCatching { player?.pause() }
                status.value = "الموسيقى متوقفة لأن تطبيق تاني بيستخدم الصوت"
            }
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attrs).setOnAudioFocusChangeListener(focusListener)
        .setWillPauseWhenDucked(false).build()

    val status = mutableStateOf("جارٍ تجهيز الصوت")
    private var player: MediaPlayer? = null
    private var currentScene = ""
    private var gain = .55f
    private var foreground = true
    private var suspendedByUserTrack = false
    private var focusGranted = false
    private var wantsMusic = false

    private fun musicResource(scene: String): Int = when(scene) {
        "splash" -> R.raw.play_music_splash
        "gate" -> R.raw.play_music_gate
        "home" -> R.raw.play_music_home
        "couple" -> R.raw.play_music_couple
        "party" -> R.raw.play_music_party
        "dice" -> R.raw.play_music_dice
        "quiz" -> R.raw.play_music_quiz
        "scratch" -> R.raw.play_music_scratch
        "whispers" -> R.raw.play_music_whispers
        "remote" -> R.raw.play_music_remote
        else -> R.raw.play_music_settings
    }

    private fun focus(): Boolean {
        if (focusGranted) return true
        val granted = runCatching { audioManager.requestAudioFocus(focusRequest) }
            .getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        focusGranted = granted
        if (!granted) status.value = "تعذّر حجز صوت الموبايل؛ اقفل تطبيق الصوت التاني"
        return granted
    }

    private fun abandon() {
        if (focusGranted) runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
        focusGranted = false
    }

    private fun releasePlayer() {
        val old = player
        player = null
        runCatching { old?.stop() }
        runCatching { old?.release() }
    }

    private fun resumeIfAllowed() {
        if (!wantsMusic || !foreground || suspendedByUserTrack || !focus()) return
        val current = player ?: return
        runCatching {
            current.setVolume(gain, gain)
            if (!current.isPlaying) current.start()
            status.value = "الموسيقى شغالة · " + currentScene
        }.onFailure { ex ->
            Log.e("R2HPlayMusic", "Playback failed for " + currentScene, ex)
            status.value = "فشل تشغيل موسيقى الصفحة: " + ex.javaClass.simpleName
        }
    }

    fun playScene(scene: String, volume: Int) {
        wantsMusic = true
        gain = volume.coerceIn(0, 100) / 100f
        if (player != null && currentScene == scene) {
            resumeIfAllowed()
            return
        }
        releasePlayer()
        currentScene = scene
        try {
            val created = MediaPlayer.create(app, musicResource(scene))
                ?: error("MediaPlayer returned null for " + scene)
            player = created
            created.isLooping = true
            created.setOnErrorListener { _, what, extra ->
                Log.e("R2HPlayMusic", "MediaPlayer error " + what + "/" + extra)
                status.value = "في مشكلة صوت (كود " + what + "/" + extra + ")"
                true
            }
            status.value = "ملف الموسيقى جاهز"
            resumeIfAllowed()
        } catch (ex: Exception) {
            Log.e("R2HPlayMusic", "Cannot open WAV for " + scene, ex)
            status.value = "مش قادر أشغل الموسيقى: " + ex.javaClass.simpleName
            releasePlayer()
        }
    }

    fun setMusicVolume(level: Int) {
        gain = level.coerceIn(0, 100) / 100f
        runCatching { player?.setVolume(gain, gain) }
    }

    fun setForeground(active: Boolean) {
        foreground = active
        if (active) resumeIfAllowed() else {
            runCatching { player?.pause() }
            abandon()
        }
    }

    fun setUserAudioPlaying(active: Boolean) {
        suspendedByUserTrack = active
        if (active) {
            runCatching { player?.pause() }
            abandon()
        } else resumeIfAllowed()
    }

    fun stop() {
        wantsMusic = false
        releasePlayer()
        abandon()
        status.value = "الموسيقى متوقفة من الإعدادات"
    }
    fun release() = stop()
}
