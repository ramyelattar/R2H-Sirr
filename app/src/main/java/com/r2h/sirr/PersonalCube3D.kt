package com.r2h.sirr

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.r2h.sirr.core.PersonalDiceFace

/**
 * Compose adapter for the on-demand GLES2 rounded-cube renderer.
 *
 * Face bitmaps are produced only from the owner's text or encrypted local
 * photos, cached by their exact identity, then uploaded to the GL thread.
 * Animation frames update only the cube pose, not the bitmaps or vault.
 */
@Composable
internal fun PersonalCube3D(
    faces: List<PersonalDiceFace>,
    visibleFace: Int,
    spinProgress: Float,
    metallic: Boolean,
    vault: LocalVault,
    label: String,
    modifier: Modifier = Modifier
) {
    require(faces.size == 6 && visibleFace in 0..5)
    val faceTiles = faces.map { face ->
        val original = remember(vault, face.imageId) {
            vault.readPhotoThumbnail(face.imageId)?.asAndroidBitmap()
        }
        remember(face.text, face.imageId, metallic, original) {
            createFaceTile(face.text, original, metallic)
        }
    }
    AndroidView(
        factory = { context -> PremiumDiceGlView(context) },
        modifier = modifier.semantics {
            contentDescription = "$label — الوجه ${visibleFace + 1}"
        },
        update = { view ->
            view.submit(visibleFace, spinProgress, metallic, faceTiles)
        }
    )
}

/** Non-graphic cube material and user's own authored face media. */
private fun createFaceTile(text: String, photo: Bitmap?, metallic: Boolean): Bitmap {
    val dim = 384
    val bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val outer = RectF(5f, 5f, dim - 5f, dim - 5f)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, dim.toFloat(), dim.toFloat(),
            if (metallic) intArrayOf(
                AndroidColor.rgb(238,240,242), AndroidColor.rgb(182,187,193),
                AndroidColor.rgb(104,110,121)
            ) else intArrayOf(
                AndroidColor.rgb(236,179,242), AndroidColor.rgb(181,110,196),
                AndroidColor.rgb(110,57,134)
            ), null, Shader.TileMode.CLAMP
        )
    }
    // An opaque base is essential: roundedness now comes from real 3D
    // geometry. Transparent bitmap corners would leave holes in the mesh.
    canvas.drawRect(0f, 0f, dim.toFloat(), dim.toFloat(), bodyPaint)

    if (photo != null && !photo.isRecycled && photo.width > 0 && photo.height > 0) {
        val target = RectF(27f, 27f, 357f, 357f)
        val cropSide = minOf(photo.width, photo.height)
        val left = (photo.width - cropSide) / 2
        val top = (photo.height - cropSide) / 2
        canvas.save()
        val clip = Path().apply {
            addRoundRect(target, 35f, 35f, Path.Direction.CW)
        }
        canvas.clipPath(clip)
        canvas.drawBitmap(
            photo,
            Rect(left, top, left + cropSide, top + cropSide),
            target,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        canvas.restore()
        if (text.isNotBlank()) {
            val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(170, 15, 12, 28)
            }
            canvas.drawRoundRect(RectF(31f, 230f, 353f, 351f), 25f, 25f, overlay)
        }
    }
    if (text.isNotBlank()) {
        val size = if (text.length < 18) 49f else if (text.length < 40) 40f else 33f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (photo != null) AndroidColor.WHITE
                else if (metallic) AndroidColor.rgb(48,51,62)
                else AndroidColor.rgb(63,24,82)
            textSize = size
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setShadowLayer(1.3f, 0f, 1.4f,
                if (photo != null) AndroidColor.BLACK else AndroidColor.argb(130,255,255,255))
        }
        val clean = text.trim()
        val layout = StaticLayout.Builder.obtain(clean, 0, clean.length, paint, 306)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false)
            .setMaxLines(if (photo != null) 2 else 3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(39f, if (photo != null) 255f else (384f - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(if (metallic) 175 else 160, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }.also { canvas.drawRoundRect(RectF(10f, 10f, 374f, 374f), 43f, 43f, it) }
    return bitmap
}
