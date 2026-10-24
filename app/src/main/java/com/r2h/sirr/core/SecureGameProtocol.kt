package com.r2h.sirr.core

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** JVM / Android 26+ protocol: interoperable with browser WebCrypto. */
object SecureGameProtocol {
    data class Sealed(val iv:String,val cipher:String)
    private val rng=SecureRandom()
    private val encoder=Base64.getUrlEncoder().withoutPadding()
    private val decoder=Base64.getUrlDecoder()
    private fun b64(v:ByteArray)=encoder.encodeToString(v)
    private fun raw(v:String)=decoder.decode(v)
        fun seal(room:String,key:ByteArray,domain:String,message:String):Sealed {
        require(key.size==32 && domain in setOf("message","challenge") && message.isNotBlank() && message.length<=500)
        return sealWithIv(room,key,domain,message,ByteArray(12).also(rng::nextBytes))
    }
    fun sealWithIv(room:String,key:ByteArray,domain:String,message:String,iv:ByteArray):Sealed {
        require(key.size==32 && iv.size==12 && domain in setOf("message","challenge"))
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        cipher.updateAAD(("$"+"room:$"+"domain").toByteArray(StandardCharsets.UTF_8))
        return Sealed(b64(iv),b64(cipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))))
    }
    fun open(room:String,key:ByteArray,domain:String,value:Sealed):String {
        require(key.size==32 && domain in setOf("message","challenge"))
        val iv=raw(value.iv);val data=raw(value.cipher)
        require(iv.size==12 && data.size in 17..3072)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        cipher.updateAAD(("$"+"room:$"+"domain").toByteArray(StandardCharsets.UTF_8))
        return String(cipher.doFinal(data),StandardCharsets.UTF_8)
    }
    fun quizTag(room:String,key:ByteArray,quizId:String,questionIndex:Int,choice:Int):String {
        require(key.size==32 && questionIndex>=0 && choice in 0..2)
        val mac=Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key,"HmacSHA256"))
        return b64(mac.doFinal(("sirr:v4:quiz:$"+"room:$"+"quizId:$"+"questionIndex:$"+"choice").toByteArray(StandardCharsets.UTF_8)))
    }
}
