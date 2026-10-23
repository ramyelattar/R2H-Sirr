package com.r2h.sirr

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Interoperable with WebCrypto AES-GCM: ciphertext contains the appended 128-bit auth tag. */
internal object RemoteCrypto {
    const val MAX_PLAINTEXT = 8 * 1024 * 1024 - 512
    private val random = SecureRandom()
    private val allowed = setOf("image/jpeg", "image/png", "image/webp",
        "audio/mpeg", "audio/ogg", "audio/mp4", "video/mp4", "video/webm", "application/vnd.sirr.card+json")
    fun key(): ByteArray = ByteArray(32).also(random::nextBytes)
    fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    fun decode(text: String): ByteArray = Base64.decode(text, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    fun mime(raw: String?): String {
        val result = raw?.lowercase()?.substringBefore(';')?.trim().orEmpty()
        require(result in allowed) { "صيغة وسائط غير مدعومة" }
        return result
    }
    fun kind(mime: String): String = if(mime=="application/vnd.sirr.card+json") "card" else mime.substringBefore('/')
    fun load(context: Context, uri: Uri): Pair<String, ByteArray> {
        val mime = mime(context.contentResolver.getType(uri))
        val out = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            val buffer=ByteArray(8192)
            while(true){val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n)
                require(out.size()<=MAX_PLAINTEXT){ "حجم الملف يتجاوز ٨ ميجابايت" }}
        }
        return mime to out.toByteArray()
    }
    /** Prefix authenticated plaintext with MIME length+name, not exposed to the relay. */
    fun encrypt(roomId: String, key: ByteArray, mime: String, bytes: ByteArray): Pair<String, ByteArray> {
        require(key.size==32 && bytes.size<=MAX_PLAINTEXT)
        val type=mime(mime);val name=type.toByteArray(Charsets.UTF_8)
        val value=byteArrayOf(name.size.toByte()) + name + bytes
        val iv=ByteArray(12).also(random::nextBytes)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        cipher.updateAAD("$roomId:${kind(type)}".toByteArray(Charsets.UTF_8))
        return encode(iv) to cipher.doFinal(value)
    }
    fun decrypt(roomId: String, key: ByteArray, kind: String, ivText: String, ciphertext: ByteArray): Pair<String,ByteArray> {
        require(key.size==32 && kind in setOf("image","audio","video","card") && ciphertext.size<=8*1024*1024)
        val iv=decode(ivText);require(iv.size==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        cipher.updateAAD("$roomId:$kind".toByteArray(Charsets.UTF_8))
        val data=cipher.doFinal(ciphertext)
        require(data.isNotEmpty());val n=data[0].toInt() and 255
        require(n in 1..64 && data.size>n)
        val type=mime(String(data,1,n,Charsets.UTF_8));require(kind(type)==kind)
        return type to data.copyOfRange(n+1,data.size)
    }
}
