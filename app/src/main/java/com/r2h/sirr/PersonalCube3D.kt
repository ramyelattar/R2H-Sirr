package com.r2h.sirr

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.r2h.sirr.core.DiceCubeProjection
import com.r2h.sirr.core.PersonalDiceFace

/**
 * Entirely native Canvas rendering, NO generated/static content assets.
 * Each cube face is rendered from the user's own text / optional local photo.
 * Six fixed raster tiles (384²) are cached by the exact authored face.
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
        val original = remember(face.imageId) {
            vault.readPhoto(face.imageId)?.asAndroidBitmap()
        }
        remember(face.text, face.imageId, metallic) {
            createFaceTile(face.text, original, metallic)
        }
    }
    Canvas(modifier.semantics {
        contentDescription = "$label — الوجه ${visibleFace + 1}"
    }) {
        val remaining = (1f - spinProgress.coerceIn(0f, 1f))
        val ordered = DiceCubeProjection.faces(
            selectedIndex = visibleFace,
            spinX = remaining * 1080f,
            spinY = remaining * 720f,
            canvasWidth = size.width,
            canvasHeight = size.height
        )
        drawIntoCanvas { composeCanvas ->
            val native = composeCanvas.nativeCanvas
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.65f
                color = if (metallic) AndroidColor.argb(175, 245, 247, 249)
                    else AndroidColor.argb(190, 237, 181, 246)
            }
            ordered.forEach { projected ->
                val quad = projected.corners
                val src = floatArrayOf(0f, 0f, 384f, 0f, 384f, 384f, 0f, 384f)
                val dst = floatArrayOf(
                    quad[0].x, quad[0].y,
                    quad[1].x, quad[1].y,
                    quad[2].x, quad[2].y,
                    quad[3].x, quad[3].y
                )
                val matrix = Matrix()
                if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) return@forEach
                native.save()
                native.concat(matrix)
                bitmapPaint.alpha = (205 + projected.facing * 50f).toInt().coerceIn(0, 255)
                native.drawBitmap(faceTiles[projected.index], 0f, 0f, bitmapPaint)
                native.restore()
                val path = Path().apply {
                    moveTo(quad[0].x, quad[0].y)
                    quad.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                edgePaint.alpha = (95 + projected.facing * 120f).toInt().coerceIn(0,255)
                native.drawPath(path, edgePaint)
            }
        }
    }
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
    canvas.drawRoundRect(outer, 49f, 49f, bodyPaint)

    if (photo != null && !photo.isRecycled && photo.width > 0 && photo.height > 0) {
        val target = RectF(27f, 27f, 357f, 357f)
        val cropSide = minOf(photo.width, photo.height)
        val left = (photo.width - cropSide) / 2
        val top = (photo.height - cropSide) / 2
        canvas.save()
        canvas.clipRoundRect(target, 35f, 35f)
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
