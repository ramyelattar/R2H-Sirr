package com.r2h.sirr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.compositionLocalOf
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Broadcast immediately whenever the existing effect preferences panel changes. */
internal val LocalSirrAudioRefresh = compositionLocalOf<() -> Unit> { {} }
internal val LocalSirrUserAudioOverride = compositionLocalOf<(Boolean) -> Unit> { {} }

internal data class SirrMusicPreferences(
    val enabled: Boolean = true,
    val volume: Int = 21,
    val cinematic: Boolean = true
) {
    companion object {
        fun load(context: Context): SirrMusicPreferences = runCatching {
            val data = LocalVault(context.applicationContext)
                .readPrivate("game-effects")
                ?.let { JSONObject(it) }
            SirrMusicPreferences(
                enabled = data?.optBoolean("music", true) ?: true,
                volume = (data?.optInt("musicVolume", 21) ?: 21).coerceIn(0, 45),
                cinematic = data?.optBoolean("cinematic", true) ?: true
            )
        }.getOrDefault(SirrMusicPreferences())
    }
}

/** Offline, original short orchestral loops: distinct notes, rhythm and tonal center per page.
 *  No copyrighted music, internet calls, microphone or runtime binary assets.
 *  Work is invoked from Dispatchers.Default, NEVER on the UI thread.
 */
internal object SirrSoundtrack {
    private const val RATE = 22050
    private const val SECONDS = 8
    const val sampleRate = RATE

    private data class Motif(
        val root: Int, val notes: IntArray, val speed: Double,
        val tint: Double, val pad: Double
    )
    private fun motif(scene: String): Motif = when (scene) {
        "splash" -> Motif(49, intArrayOf(0, 7, 12, 15, 19, 15, 12, 7), .95, .63, .35)
        "gate" -> Motif(49, intArrayOf(0, 7, 10, 12, 15, 12, 10, 7), .65, .38, .48)
        "home" -> Motif(52, intArrayOf(0, 7, 12, 15, 19, 15, 12, 7), 1.0, .55, .40)
        "couple" -> Motif(57, intArrayOf(0, 3, 7, 12, 10, 7, 3, 7), .82, .42, .56)
        "party" -> Motif(55, intArrayOf(0, 7, 12, 10, 15, 12, 7, 3), 1.7, .72, .30)
        "dice" -> Motif(45, intArrayOf(0, 7, 12, 7, 15, 12, 10, 7), 1.5, .83, .28)
        "quiz" -> Motif(50, intArrayOf(0, 3, 10, 7, 12, 15, 10, 3), .92, .48, .45)
        "scratch" -> Motif(47, intArrayOf(0, 12, 7, 15, 10, 7, 12, 3), 1.2, .77, .32)
        "whispers" -> Motif(54, intArrayOf(0, 12, 15, 7, 10, 3, 7, 12), .72, .35, .55)
        "remote" -> Motif(48, intArrayOf(0, 7, 10, 12, 7, 15, 12, 10), 1.10, .38, .45)
        else -> Motif(50, intArrayOf(0, 7, 12, 10, 15, 12, 7, 3), .68, .30, .43)
    }
    private fun frequency(midi: Int): Double =
        440.0 * 2.0.pow((midi - 69) / 12.0)

    fun render(scene: String): ShortArray {
        val m = motif(scene)
        val count = RATE * SECONDS
        val buffer = ShortArray(count)
        val fundamental = frequency(m.root)
        val fifth = frequency(m.root + 7)
        val minorThird = frequency(m.root + 3)
        // Eight distinct musical phrases are looped at a fixed eight-second boundary.
        for (i in 0 until count) {
            val time = i / RATE.toDouble()
            val interval = 1.0 / m.speed
            val step = (time / interval).toInt() % m.notes.size
            val beatAge = time % interval
            val note = frequency(m.root + m.notes[step] + 12)
            val decay = exp(-beatAge * (4.2 + m.speed))
            val pad = (
                sin(2.0 * PI * fundamental * time) * .29 +
                sin(2.0 * PI * fifth * time + .55) * .18 +
                sin(2.0 * PI * minorThird * time + .21) * .12
            ) * m.pad
            val chime = (
                sin(2.0 * PI * note * time) * .64 +
                sin(2.0 * PI * note * 2.005 * time) * .17 +
                sin(2.0 * PI * note * 3.01 * time) * .08
            ) * decay * m.tint
            val undertone = sin(2.0 * PI * (fundamental / 2.0) * time) * .065
            val fade = min(1.0, min(time / .24, (SECONDS - time) / .29)).coerceIn(0.0, 1.0)
            val sample = ((pad + chime + undertone) * .40 * fade *
                Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }
}

/** Exactly one looping track across navigation; handles foreground, focus, mute and teardown. */
internal class SirrPageMusic(context: Context) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_GAME)
        .build()
    private var track: AudioTrack? = null
    private var samples: ShortArray? = null
    private var desired = false
    private var foreground = true
    private var suspended = false
    private var requestedFocus = false
    private var volume = .16f

    private val focusListener = AudioManager.OnAudioFocusChangeListener { state ->
        when (state) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (desired && foreground && !suspended) {
                    runCatching { track?.setVolume(volume); track?.play() }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                runCatching { track?.setVolume(volume * .18f) }
            else -> runCatching { track?.pause() }
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(focusListener)
        .setWillPauseWhenDucked(false)
        .build()

    private fun hasFocus(): Boolean {
        if (requestedFocus) return true
        val result = audioManager.requestAudioFocus(focusRequest)
        requestedFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return requestedFocus
    }
    private fun dropFocus() {
        if (requestedFocus) {
            runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
        }
        requestedFocus = false
    }
    private fun dropTrack() {
        val old = track
        track = null
        runCatching { old?.pause() }
        runCatching { old?.flush() }
        runCatching { old?.release() }
    }
    private fun activate() {
        if (!desired || !foreground || suspended || !hasFocus()) return
        if (track == null) {
            val pcm = samples ?: return
            try {
                val next = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(SirrSoundtrack.sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                if (next.state != AudioTrack.STATE_INITIALIZED) {
                    next.release()
                    dropFocus()
                    return
                }
                val written = next.write(pcm, 0, pcm.size)
                if (written != pcm.size || next.setLoopPoints(0, pcm.size, -1) != AudioTrack.SUCCESS) {
                    next.release()
                    dropFocus()
                    return
                }
                track = next
            } catch (_: Exception) {
                dropTrack()
                dropFocus()
                return
            }
        }
        runCatching {
            track?.setVolume(volume)
            track?.play()
        }
    }

    fun playScene(pcm: ShortArray, level: Int) {
        dropTrack()
        dropFocus()
        samples = pcm
        desired = true
        volume = (level.coerceIn(0, 45) / 100f) * .85f
        activate()
    }
    fun setForeground(active: Boolean) {
        foreground = active
        if (active) activate()
        else {
            runCatching { track?.pause() }
            dropFocus()
        }
    }
    fun setUserAudioPlaying(active: Boolean) {
        suspended = active
        if (active) {
            runCatching { track?.pause() }
            dropFocus()
        } else activate()
    }
    fun stop() {
        desired = false
        samples = null
        dropTrack()
        dropFocus()
    }
    fun release() = stop()
}
