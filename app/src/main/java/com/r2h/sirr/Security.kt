package com.r2h.sirr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.r2h.sirr.core.*

/** Keystore AES-GCM; no image is written unencrypted. Does not claim audited E2EE. */
internal class LocalVault(private val context: Context) {
    private val key: SecretKey
        get() {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey("sirr-local-v2", null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder("sirr-local-v2", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).setKeySize(256).build())
            return generator.generateKey()
        }

    private fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(bytes)
    }
    private fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
    }
    private val dir get() = File(context.noBackupFilesDir, "sirr-vault").apply { mkdirs() }

    fun importPhoto(uri: Uri): String {
        // Decode with sampling to bound memory before removing metadata / re-encoding.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "تعذر قراءة الصورة" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth <= 20000 && bounds.outHeight <= 20000)
        val maxDim = maxOf(bounds.outHeight, bounds.outWidth)
        var sample = 1
        while (maxDim / sample > 1280) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(requireNotNull(input), null, options)
        } ?: error("لم تُقرأ الصورة")
        val stream = java.io.ByteArrayOutputStream()
        try { require(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)) }
        finally { bitmap.recycle() }
        val bytes = stream.toByteArray()
        require(bytes.size < 6 * 1024 * 1024)
        val fileId = java.util.UUID.randomUUID().toString()
        File(dir, "$fileId.bin").writeBytes(encrypt(bytes))
        bytes.fill(0)
        return fileId
    }
    fun readPhoto(fileId: String?): ImageBitmap? {
        if (fileId == null || !Regex("[0-9a-f-]{36}").matches(fileId)) return null
        return runCatching {
            val bytes = decrypt(File(dir, "$fileId.bin").readBytes())
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            bytes.fill(0)
            bitmap
        }.getOrNull()
    }
    /**
     * Lightweight encrypted-photo preview for animated 3D cube tiles.
     * Never writes a plaintext thumbnail to disk or changes the stored photo.
     * Bounds the decoded preview to roughly 384 px rather than retaining
     * twelve full 1280px photo bitmaps in Compose state.
     */
    fun readPhotoThumbnail(fileId: String?): ImageBitmap? {
        if (fileId == null || !Regex("[0-9a-f-]{36}").matches(fileId)) return null
        return runCatching {
            val bytes = decrypt(File(dir, "$fileId.bin").readBytes())
            try {
                val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dimensions)
                require(dimensions.outWidth > 0 && dimensions.outHeight > 0)
                var sample = 1
                while (maxOf(dimensions.outWidth, dimensions.outHeight) / sample > 384)
                    sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )?.asImageBitmap()
            } finally { bytes.fill(0) }
        }.getOrNull()
    }
    fun delete(fileId: String?) {
        if (fileId != null && Regex("[0-9a-f-]{36}").matches(fileId)) File(dir, "$fileId.bin").delete()
    }
    fun writePrivate(name: String, value: String) {
        require(name.matches(Regex("[a-z0-9-]{1,30}")))
        val plain = value.toByteArray(Charsets.UTF_8)
        val encrypted = try { encrypt(plain) } finally { plain.fill(0) }
        val file = AtomicFile(File(dir, "$name.private"))
        try {
            val stream = file.startWrite()
            try {
                stream.write(encrypted)
                file.finishWrite(stream)
            } catch (failure: Throwable) {
                file.failWrite(stream)
                throw failure
            }
        } finally { encrypted.fill(0) }
    }
    fun readPrivate(name: String): String? = runCatching {
        require(name.matches(Regex("[a-z0-9-]{1,30}")))
        val encrypted = AtomicFile(File(dir, "$name.private")).openRead().use { it.readBytes() }
        val plain = try { decrypt(encrypted) } finally { encrypted.fill(0) }
        try { String(plain, Charsets.UTF_8) } finally { plain.fill(0) }
    }.getOrNull()
    /**
     * Existence check only; never decrypts or exposes payload.
     * Covers interrupted AtomicFile backups, so unreadable records are not treated as absent.
     */
    fun hasPrivate(name: String): Boolean {
        require(name.matches(Regex("[a-z0-9-]{1,30}")))
        val file = File(dir, "$name.private")
        return file.exists() || File(dir, "$name.private.bak").exists() ||
            File(dir, "$name.private.new").exists()
    }

    fun deletePrivate(name: String) {
        require(name.matches(Regex("[a-z0-9-]{1,30}")))
        AtomicFile(File(dir, "$name.private")).delete()
    }
    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}

internal object DiceStorage {
    fun save(vault: LocalVault, config: DiceConfiguration) {
        config.validate()
        val data = JSONObject()
        data.put("version", 1).put("duration", config.durationSeconds).put("sound", config.sound)
        val dice = JSONArray()
        config.dice.forEach { group ->
            val faces = JSONArray()
            group.forEach { face -> faces.put(JSONObject().put("text", face.text).put("weight", face.weight).put("image", face.imageId)) }
            dice.put(faces)
        }
        data.put("dice", dice)
        vault.writePrivate("dice", data.toString())
    }
    fun load(vault: LocalVault): DiceConfiguration? = runCatching {
        val json = JSONObject(vault.readPrivate("dice") ?: return null)
        require(json.getInt("version") == 1)
        val groups = json.getJSONArray("dice")
        val config = DiceConfiguration((0 until groups.length()).map { group ->
            val faces = groups.getJSONArray(group)
            (0 until faces.length()).map { index ->
                val face = faces.getJSONObject(index)
                DieFace(face.getString("text"), face.getInt("weight"), face.optString("image").takeIf { it.isNotBlank() && it != "null" })
            }
        }, json.getInt("duration"), json.getBoolean("sound"))
        config.validate()
        config
    }.getOrNull()
}
