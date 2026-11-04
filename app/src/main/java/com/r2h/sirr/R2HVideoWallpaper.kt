package com.r2h.sirr

import android.content.Context
import android.graphics.Matrix as AndroidMatrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlin.math.max

/**
 * Root-only video wallpaper. One silent, looping hardware-decoded stream is
 * shared by ALL routes; individual pages never create their own video player.
 *
 * Required local asset: app/src/main/assets/r2h_cosmic_background.mp4
 * Optional static poster: res/drawable-nodpi/r2h_cosmic_poster.webp
 *
 * If assets have not yet been bundled, the app still runs with the dark
 * gradient. No network access, microphone or media permissions are used.
 */
@Composable
internal fun R2HVideoWallpaper(allowMotion: Boolean) {
    val context = LocalContext.current
    val poster = remember(context) {
        context.resources.getIdentifier(
            "r2h_cosmic_poster", "drawable", context.packageName
        )
    }
    val hasVideo = remember(context) {
        runCatching {
            context.assets.openFd(CosmicWallpaperView.ASSET).use { true }
        }.getOrDefault(false)
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF200C32), Color(0xFF0F071C), Color(0xFF090611))
            )
        )
    ) {
        if (poster != 0) {
            Image(
                painter = painterResource(poster),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (allowMotion && hasVideo) {
            val playerView = remember(context) { CosmicWallpaperView(context) }
            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize()
            )
            DisposableEffect(playerView) {
                onDispose { playerView.dispose() }
            }
        }
        // Consistent contrast layer above the animation, below ALL page UI.
        // The video has bright particles; never place raw white text over it.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xA6000613),
                    .32f to Color(0x99080415),
                    .68f to Color(0xB0080313),
                    1f to Color(0xD3050310)
                )
            )
        )
    }
}

/**
 * TextureView rather than SurfaceView is deliberate: it stays behind Compose
 * cards, bottom sheets and controls without Android SurfaceView Z-order issues.
 */
private class CosmicWallpaperView(context: Context) : TextureView(context),
    TextureView.SurfaceTextureListener {

    companion object {
        const val ASSET = "r2h_cosmic_background.mp4"
        private const val TAG = "R2H-VideoWallpaper"
        private const val WIDTH = 478f
        private const val HEIGHT = 850f
    }

    private var player: MediaPlayer? = null
    private var outputSurface: Surface? = null
    private var ready = false
    private var foreground = false
    private var frameWidth = WIDTH
    private var frameHeight = HEIGHT
    private var owner: LifecycleOwner? = null
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> foregroundChanged(true)
            Lifecycle.Event.ON_STOP -> foregroundChanged(false)
            else -> Unit
        }
    }

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        owner = findViewTreeLifecycleOwner()
        owner?.lifecycle?.addObserver(observer)
        foregroundChanged(owner?.lifecycle?.currentState
            ?.isAtLeast(Lifecycle.State.STARTED) == true)
        if (isAvailable && player == null) surfaceTexture?.let(::openPlayback)
    }

    override fun onDetachedFromWindow() {
        owner?.lifecycle?.removeObserver(observer)
        owner = null
        foregroundChanged(false)
        dispose()
        super.onDetachedFromWindow()
    }

    private fun foregroundChanged(visible: Boolean) {
        foreground = visible
        val current = player ?: return
        if (!ready) return
        runCatching {
            if (visible && !current.isPlaying) current.start()
            else if (!visible && current.isPlaying) current.pause()
        }.onFailure { Log.w(TAG, "Playback state change failed", it) }
    }

    private fun updateCrop() {
        if (width == 0 || height == 0) return
        val scale = max(width / frameWidth, height / frameHeight)
        val widthRatio = frameWidth * scale / width
        val heightRatio = frameHeight * scale / height
        val transform = AndroidMatrix()
        transform.setScale(widthRatio, heightRatio, width / 2f, height / 2f)
        setTransform(transform)
    }

    private fun openPlayback(texture: SurfaceTexture) {
        if (player != null) return
        val surface = Surface(texture)
        outputSurface = surface
        try {
            val prepared = MediaPlayer()
            player = prepared
            context.assets.openFd(ASSET).use { asset ->
                prepared.setDataSource(
                    asset.fileDescriptor, asset.startOffset, asset.length
                )
            }
            prepared.setSurface(surface)
            prepared.isLooping = true
            // Strip audio when packaging; keep this explicit as a second guard.
            prepared.setVolume(0f, 0f)
            prepared.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    frameWidth = w.toFloat()
                    frameHeight = h.toFloat()
                    updateCrop()
                }
            }
            prepared.setOnPreparedListener {
                ready = true
                foregroundChanged(foreground)
            }
            prepared.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Video decoder error: $what / $extra")
                dispose()
                true
            }
            prepared.prepareAsync()
            updateCrop()
        } catch (exception: Exception) {
            Log.w(TAG, "Wallpaper video unavailable; keeping static poster", exception)
            dispose()
        }
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture, width: Int, height: Int
    ) {
        updateCrop()
        openPlayback(surface)
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture, width: Int, height: Int
    ) { updateCrop() }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        dispose()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun dispose() {
        ready = false
        val old = player
        player = null
        runCatching { old?.reset() }
        runCatching { old?.release() }
        outputSurface?.release()
        outputSurface = null
    }
}
