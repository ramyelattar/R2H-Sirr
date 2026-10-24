package com.r2h.sirr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.security.MessageDigest

/** M6 encrypted, resumable transfer. Only ciphertext and chunk metadata reach the relay. */
internal class RemoteChunkTransfer(private val client:OkHttpClient) {
    companion object {
        const val CHUNK_SIZE=512*1024
        const val MAX_SIZE=48*1024*1024
    }
    data class Pending(val uri:Uri,val roomId:String,val id:String,val mime:String,val total:Int,val digest:String)
    @Volatile var cancelled=false
        private set
    @Volatile var pending:Pending?=null
        private set

    fun cancel(id:RemoteIdentity) {
        cancelled=true
        pending?.takeIf{it.roomId==id.roomId}?.let { item ->
            runCatching { call(id,"${base(id)}/uploads/${item.id}","DELETE") }
        }
        pending=null
    }
    fun size(context:Context,uri:Uri):Long? {
        context.contentResolver.query(uri,arrayOf(OpenableColumns.SIZE),null,null,null)?.use { cursor ->
            if(cursor.moveToFirst())return cursor.getLong(0).takeIf{it>=0}
        }
        return null
    }
    private fun base(id:RemoteIdentity)="${id.server}/api/rooms/${id.roomId}"
    private fun call(id:RemoteIdentity,url:String,method:String="GET",json:String?=null,
                     data:ByteArray?=null,iv:String?=null):Pair<JSONObject?,ByteArray?> {
        val builder=Request.Builder().url(url).header("Authorization","Bearer ${id.auth}")
        val body=when {
            json!=null -> json.toRequestBody("application/json; charset=utf-8".toMediaType())
            data!=null -> data.toRequestBody("application/octet-stream".toMediaType())
            else -> ByteArray(0).toRequestBody(null)
        }
        val req=when(method){"POST"->builder.post(body);"PUT"->builder.put(body);"DELETE"->builder.delete();else->builder.get()}
        if(iv!=null)req.header("X-Encrypted-Iv",iv)
        return client.newCall(req.build()).execute().use { response ->
            if(!response.isSuccessful)error("تعذر نقل الملف (${response.code})")
            val raw=response.body?.bytes()?:ByteArray(0)
            if(url.contains("/chunks/") && method=="GET")null to raw
            else JSONObject(String(raw,Charsets.UTF_8)) to null
        }
    }
    private fun readPart(stream:InputStream,bytes:Int):ByteArray {
        val result=ByteArray(bytes);var got=0
        while(got<bytes){val count=stream.read(result,got,bytes-got);if(count<=0)error("ملف ناقص أو تغيّر أثناء الإرسال");got+=count}
        return result
    }
    /** Fingerprint includes the MIME prefix. Never resume chunks from a different same-size file. */
    private fun fingerprint(context:Context,uri:Uri,prefix:ByteArray,size:Long):String {
        val digest=MessageDigest.getInstance("SHA-256")
        digest.update(prefix)
        var length=0L
        context.contentResolver.openInputStream(uri).use { input ->
            val stream=requireNotNull(input)
            val buffer=ByteArray(16*1024)
            while(true) {
                val n=stream.read(buffer)
                if(n<0)break
                if(n>0){digest.update(buffer,0,n);length+=n;require(length<=MAX_SIZE)}
            }
        }
        require(length==size){"اتغير حجم الملف؛ اختاره من جديد"}
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
    fun upload(id:RemoteIdentity,context:Context,uri:Uri,progress:(String)->Unit):String {
        cancelled=false
        val mime=RemoteCrypto.mime(context.contentResolver.getType(uri))
        require(mime!="application/vnd.sirr.card+json")
        val size=size(context,uri)?:error("تعذر معرفة حجم الملف؛ اختر ملفًا محليًا")
        val prefix=mime.toByteArray(Charsets.UTF_8).let{byteArrayOf(it.size.toByte())+it}
        val total=size+prefix.size
        require(size>0 && total<=MAX_SIZE && total<=Int.MAX_VALUE){"الحد الأقصى ٤٨ ميجابايت"}
        val expectedDigest=fingerprint(context,uri,prefix,size)
        var task=pending?.takeIf{it.uri==uri && it.roomId==id.roomId && it.mime==mime && it.total==total.toInt() && it.digest==expectedDigest}
        if(task==null){
            val request=JSONObject().put("kind",RemoteCrypto.kind(mime)).put("totalBytes",total)
            val info=call(id,"${base(id)}/uploads","POST",json=request.toString()).first!!
            task=Pending(uri,id.roomId,info.getString("id"),mime,total.toInt(),expectedDigest);pending=task
        }
        val draft=task
        val statusUrl="${base(id)}/uploads/${draft.id}"
        val received=call(id,statusUrl).first!!.getJSONArray("received")
        val existing=(0 until received.length()).map{received.getInt(it)}.toSet()
        val count=(draft.total+CHUNK_SIZE-1)/CHUNK_SIZE
        val actualDigest=MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri).use { source ->
            requireNotNull(source)
            SequenceInputStream(ByteArrayInputStream(prefix),source).use { stream ->
                for(i in 0 until count){
                    if(cancelled)error("تم إلغاء النقل")
                    val part=readPart(stream,minOf(CHUNK_SIZE,draft.total-i*CHUNK_SIZE))
                    actualDigest.update(part)
                    if(i in existing){progress("تجاوز الجزء المؤكد ${i+1}/$count");continue}
                    val (iv,encrypted)=RemoteCrypto.encryptChunk(id.roomId,id.sessionKey,draft.id,i,
                        RemoteCrypto.kind(mime),part)
                    var sent=false
                    for(attempt in 0..2){
                        try{
                            call(id,"$statusUrl/chunks/$i","PUT",data=encrypted,iv=iv)
                            sent=true;break
                        }catch(e:Exception){
                            if(cancelled)throw e
                            val remote=runCatching{call(id,statusUrl).first!!.getJSONArray("received")}.getOrNull()
                            if(remote!=null && (0 until remote.length()).any{remote.getInt(it)==i}){sent=true;break}
                            if(attempt==2)throw e
                        }
                    }
                    if(!sent)error("فشل الجزء ${i+1}")
                    progress("تم نقل ${i+1}/$count أجزاء مشفرة")
                }
            }
        }
        if(cancelled)error("تم إلغاء النقل")
        val actual=actualDigest.digest().joinToString(""){"%02x".format(it)}
        if(actual!=expectedDigest){
            runCatching{call(id,statusUrl,"DELETE")};pending=null
            error("الملف اتغير أثناء الرفع، ابدأ نقل جديد")
        }
        val complete=call(id,"$statusUrl/complete","POST").first!!
        pending=null
        return complete.getString("id")
    }
    fun download(id:RemoteIdentity,mediaId:String,kind:String,progress:(String)->Unit):Pair<String,ByteArray> {
        val path="${base(id)}/media/$mediaId"
        val meta=call(id,"$path/manifest").first!!
        require(meta.getString("kind")==kind)
        val total=meta.getInt("totalBytes");val count=meta.getInt("count")
        require(total in 1..MAX_SIZE && count==(total+CHUNK_SIZE-1)/CHUNK_SIZE)
        // Allocate only the final plaintext output; do not hold a second 48 MB copy.
        var plaintext:ByteArray?=null
        var expectedMime:String?=null
        var copied=0
        for(index in 0 until count){
            val url="$path/chunks/$index"
            val req=Request.Builder().url(url).header("Authorization","Bearer ${id.auth}").get().build()
            client.newCall(req).execute().use { response ->
                if(!response.isSuccessful)error("الجزء ${index+1} غير متاح")
                val iv=response.header("X-Encrypted-Iv").orEmpty()
                val payload=response.body?.bytes()?:error("الجزء فارغ")
                val decoded=RemoteCrypto.decryptChunk(id.roomId,id.sessionKey,mediaId,index,kind,iv,payload)
                try {
                    val expected=if(index==count-1)total-index*CHUNK_SIZE else CHUNK_SIZE
                    require(decoded.size==expected){"حجم الجزء غير مطابق"}
                    val offset=if(index==0){
                        val nameSize=decoded[0].toInt() and 255
                        require(nameSize in 1..64 && decoded.size>nameSize)
                        val mime=RemoteCrypto.mime(String(decoded,1,nameSize,Charsets.UTF_8))
                        require(RemoteCrypto.kind(mime)==kind)
                        expectedMime=mime
                        plaintext=ByteArray(total-nameSize-1)
                        nameSize+1
                    } else 0
                    val target=requireNotNull(plaintext)
                    val length=decoded.size-offset
                    require(copied+length<=target.size)
                    System.arraycopy(decoded,offset,target,copied,length)
                    copied+=length
                } finally { decoded.fill(0) }
            }
            progress("تنزيل ${index+1}/$count أجزاء مشفرة")
        }
        val result=requireNotNull(plaintext)
        require(copied==result.size)
        return requireNotNull(expectedMime) to result
    }
}
