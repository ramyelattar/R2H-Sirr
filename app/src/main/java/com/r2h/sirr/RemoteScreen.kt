package com.r2h.sirr

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.r2h.sirr.core.ExpandedQuizzes
import com.r2h.sirr.core.Intensity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.min

internal data class RemoteIdentity(
    val server:String,val roomId:String,val auth:String,val participantId:String,
    val sessionKey:ByteArray,val inviteUrl:String?=null
)

/** HTTPS/WSS only. Authentication is the first WS message, never a query parameter. */
private class RemoteTransport(private val client:OkHttpClient) {
    private val main=Handler(Looper.getMainLooper())
    private var ws:WebSocket?=null
    private var disconnected=false
    private var generation=0
    private var reconnectAttempt=0
    private var reconnectTask:Runnable?=null
    private var current:RemoteIdentity?=null
    private var onState:((JSONObject)->Unit)?=null
    private var onStatus:((String)->Unit)?=null

    private fun server(raw:String):String {
        val parsed=Uri.parse(raw.trimEnd('/'))
        require(parsed.scheme=="https" && !parsed.host.isNullOrBlank() && !parsed.authority.orEmpty().contains('@') &&
            (parsed.path.isNullOrEmpty() || parsed.path=="/") && parsed.query==null && parsed.fragment==null)
        return "https://${parsed.authority}"
    }
    private fun request(req:Request,done:(Result<JSONObject>)->Unit) {
        client.newCall(req).enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) {main.post{done(Result.failure(e))}}
            override fun onResponse(call:Call,response:Response) {
                val r=runCatching{response.use{if(!it.isSuccessful)error("تعذر إتمام العملية (${it.code})")
                    JSONObject(it.body?.string().orEmpty())}}
                main.post{done(r)}
            }
        })
    }
    private fun jsonPost(url:String,data:JSONObject,done:(Result<JSONObject>)->Unit){
        request(Request.Builder().url(url).post(data.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build(),done)
    }
    fun create(serverInput:String,name:String,mode:String,level:Intensity,done:(Result<RemoteIdentity>)->Unit){
        val base=runCatching{server(serverInput)}.getOrElse{done(Result.failure(it));return}
        jsonPost("$base/api/rooms",JSONObject().put("nickname",name).put("mode",mode).put("level",level.name)){result->
            done(result.map{data->
                val key=RemoteCrypto.key()
                // The symmetric key is in URL fragment, invisible to the relay HTTP server.
                RemoteIdentity(base,data.getString("roomId"),data.getString("auth"),data.getString("participantId"),
                    key,data.getString("inviteUrl")+"&key="+RemoteCrypto.encode(key))
            })
        }
    }
    fun join(link:String,name:String,done:(Result<RemoteIdentity>)->Unit){
        val params=runCatching{
            val url=Uri.parse(link)
            require(url.scheme=="https" && url.pathSegments.size==2 && url.pathSegments[0]=="join" && url.query==null)
            val id=url.pathSegments[1];require(id.matches(Regex("[0-9a-f-]{36}")))
            val fragment=Uri.parse("https://fragment.invalid/?${url.fragment.orEmpty()}")
            val invite=fragment.getQueryParameter("invite").orEmpty()
            val key=RemoteCrypto.decode(fragment.getQueryParameter("key").orEmpty())
            require(invite.length>=30 && key.size==32)
            Triple(server("https://${url.authority}"),id,Pair(invite,key))
        }.getOrElse{done(Result.failure(IllegalArgumentException("رابط الدعوة أو مفتاح التشفير غير صالح")));return}
        jsonPost("${params.first}/api/rooms/${params.second}/join",JSONObject().put("invite",params.third.first).put("nickname",name)){result->
            done(result.map{data->RemoteIdentity(params.first,params.second,data.getString("auth"),data.getString("participantId"),params.third.second)})
        }
    }
    fun connect(identity:RemoteIdentity,state:(JSONObject)->Unit,status:(String)->Unit){
        disconnected=false;current=identity;onState=state;onStatus=status;reconnectAttempt=0;open()
    }
    private fun open(){
        val id=current?:return
        if(disconnected)return
        reconnectTask?.let(main::removeCallbacks);reconnectTask=null
        val seq=++generation
        ws?.cancel()
        val address=id.server.replaceFirst("https://","wss://")+"/ws"
        ws=client.newWebSocket(Request.Builder().url(address).build(),object:WebSocketListener(){
            override fun onOpen(webSocket:WebSocket,response:Response){
                webSocket.send(JSONObject().put("type","authenticate").put("roomId",id.roomId).put("auth",id.auth).toString())
            }
            override fun onMessage(webSocket:WebSocket,text:String){
                val data=runCatching{JSONObject(text)}.getOrNull()?:return
                main.post{
                    if(seq!=generation||disconnected)return@post
                    when(data.optString("type")){
                        "authenticated"->{reconnectAttempt=0;onStatus?.invoke("متصل — التزامن نشط")}
                        "state"->onState?.invoke(data.getJSONObject("data"))
                        "error"->onStatus?.invoke(data.optString("message","تعذر تنفيذ الأمر"))
                    }
                }
            }
            override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?){main.post{retry(seq,0)}}
            override fun onClosed(webSocket:WebSocket,code:Int,reason:String){main.post{retry(seq,code)}}
        })
    }
    private fun retry(seq:Int,code:Int){
        if(disconnected||seq!=generation)return
        if(code==4000||code==4003){onStatus?.invoke("الجلسة منتهية أو الدخول مرفوض");return}
        val delay=min(30_000L,1000L*(1 shl min(reconnectAttempt++,5)))
        onStatus?.invoke("انقطع الاتصال — إعادة المحاولة تلقائيًا")
        reconnectTask=Runnable{open()}.also{main.postDelayed(it,delay)}
    }
    fun reconnect(){reconnectAttempt=0;open()}
    fun action(type:String,extra:JSONObject=JSONObject()){
        val data=JSONObject().put("type",type).put("eventId",UUID.randomUUID().toString().replace("-",""))
        val keys=extra.keys();while(keys.hasNext()){val key=keys.next();data.put(key,extra.get(key))}
        ws?.send(data.toString())
    }
    fun upload(context:android.content.Context,uri:Uri,status:(String)->Unit){
        val id=current?:return
        Thread{
            val result=runCatching{
                val (mime,data)=RemoteCrypto.load(context,uri)
                val (iv,cipher)=RemoteCrypto.encrypt(id.roomId,id.sessionKey,mime,data)
                val req=Request.Builder().url("${id.server}/api/rooms/${id.roomId}/media")
                    .header("Authorization","Bearer ${id.auth}")
                    .header("X-Media-Kind",RemoteCrypto.kind(mime))
                    .header("X-Encrypted-Iv",iv)
                    .post(cipher.toRequestBody("application/octet-stream".toMediaType())).build()
                client.newCall(req).execute().use{if(!it.isSuccessful)error("الخادم رفض الوسيط (${it.code})")}
            }
            main.post{status(result.fold({"تم إرسال الملف مشفرًا. إتاحته مؤقتة."},{it.message?:"تعذر إرسال الملف"}))}
        }.start()
    }
    /** Share a minimal, E2EE Arabic whisper/reveal card, not plaintext server chat. */
    fun uploadCard(type:String,lines:List<String>,status:(String)->Unit){
        val id=current?:return
        Thread {
            val result=runCatching {
                require(type in listOf("whispers","scratch") && lines.size in 1..3 &&
                    (type!="whispers" || lines.size==3) && lines.all{it.isNotBlank()&&it.length<=350})
                val payload=JSONObject().put("version",1).put("type",type)
                    .put("lines",JSONArray(lines)).toString().toByteArray(Charsets.UTF_8)
                require(payload.size<=12*1024)
                val (iv,cipher)=RemoteCrypto.encrypt(id.roomId,id.sessionKey,"application/vnd.sirr.card+json",payload)
                val req=Request.Builder().url("${id.server}/api/rooms/${id.roomId}/media")
                    .header("Authorization","Bearer ${id.auth}")
                    .header("X-Media-Kind","card").header("X-Encrypted-Iv",iv)
                    .post(cipher.toRequestBody("application/octet-stream".toMediaType())).build()
                client.newCall(req).execute().use { if(!it.isSuccessful) error("تعذر مشاركة البطاقة (${it.code})") }
            }
            main.post { status(result.fold({"تمت مشاركة البطاقة مشفرة؛ تُعرض مرة واحدة للطرف الآخر."},
                {it.message?:"تعذر إرسال البطاقة"})) }
        }.start()
    }
    fun download(mediaId:String,kind:String,done:(Result<Pair<String,ByteArray>>)->Unit){
        val id=current?:return
        val request=Request.Builder().url("${id.server}/api/rooms/${id.roomId}/media/$mediaId")
            .header("Authorization","Bearer ${id.auth}").get().build()
        client.newCall(request).enqueue(object:Callback{
            override fun onFailure(call:Call,e:IOException){main.post{done(Result.failure(e))}}
            override fun onResponse(call:Call,response:Response){
                val result=runCatching{response.use{
                    if(!it.isSuccessful)error("الملف محذوف أو منتهي الصلاحية")
                    val serverKind=it.header("X-Media-Kind").orEmpty()
                    require(serverKind==kind)
                    val raw=it.body?.bytes()?:error("لا توجد بيانات")
                    RemoteCrypto.decrypt(id.roomId,id.sessionKey,kind,it.header("X-Encrypted-Iv").orEmpty(),raw)
                }}
                main.post{done(result)}
            }
        })
    }
    fun close(){disconnected=true;generation++;current=null;ws?.close(1000,"leaving screen");ws=null
        reconnectTask?.let(main::removeCallbacks);reconnectTask=null}
}

@Composable
internal fun RemoteScreen(){
    val context=LocalContext.current
    val client=remember{OkHttpClient.Builder().retryOnConnectionFailure(true).build()}
    val transport=remember{RemoteTransport(client)}
    var server by remember{mutableStateOf("")};var link by remember{mutableStateOf("")}
    var name by remember{mutableStateOf("")};var mode by remember{mutableStateOf("couple")}
    var level by remember{mutableStateOf(Intensity.SOFT)}
    var busy by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")}
    val remoteStore=remember{RemoteSessionStorage(LocalVault(context.applicationContext))}
    var identity by remember{mutableStateOf<RemoteIdentity?>(null)}
    var state by remember{mutableStateOf<JSONObject?>(null)}
    var preview by remember{mutableStateOf<Pair<String,ByteArray>?>(null)}
    var draft by remember{mutableStateOf("")}
    var cardMode by remember{mutableStateOf("whispers")}
    val cardTexts=remember{mutableStateListOf("","","")}
    DisposableEffect(Unit){onDispose{transport.close()}}
    LaunchedEffect(Unit){
        remoteStore.restore()?.let { saved ->
            identity=saved;status="استعادة جلسة محفوظة — جارِ التحقق من صلاحيتها"
            transport.connect(saved,{ snapshot ->
                state=snapshot
                if(snapshot.optString("status") in listOf("ENDED","REVOKED","EXPIRED")) remoteStore.clear()
            },{msg->status=msg})
        }
    }
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null)transport.upload(context.applicationContext,uri){status=it}
    }
    fun received(result:Result<RemoteIdentity>){busy=false;result.onSuccess{
        identity=it;state=null;status="جارِ الاتصال"
        // Session expiry validated again by the server on reconnection.
        runCatching { remoteStore.save(it,System.currentTimeMillis()+60*60*1000) }
            .onFailure { status="الجلسة متصلة؛ تعذر حفظ بيانات الاستعادة على هذا الجهاز" }
        transport.connect(it,{s->
            state=s
            if(s.optString("status") in listOf("ENDED","REVOKED","EXPIRED")) remoteStore.clear()
        },{msg->status=msg})
    }.onFailure{status=it.message?:"فشل الاتصال"}}
    Page("جلسة خاصة عبر الإنترنت","ملفات مشفرة بمفتاح الدعوة، واختبارات مقارنة بين جهازين. يتطلب خادم HTTPS مستقلًا."){
        if(identity==null){
            Text("أدخل عنوان خادم سِرّ الخاص بك. لا توجد خدمة عامة مفعلة.",color=Color.LightGray)
            OutlinedTextField(server,{server=it.take(180)},label={Text("عنوان HTTPS")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(name,{name=it.take(32)},label={Text("الاسم المستعار")},modifier=Modifier.fillMaxWidth())
            Row{FilterChip(mode=="couple",{mode="couple"},label={Text("شريكان")});Spacer(Modifier.width(8.dp));
                FilterChip(mode=="party",{mode="party"},label={Text("مجموعة")})}
            Intensity.entries.filter{it!=Intensity.CUSTOM}.forEach{option->
                FilterChip(level==option,{level=option},label={Text(option.arabic)})
            }
            Button(enabled=name.isNotBlank()&&server.startsWith("https://")&&!busy,onClick={
                busy=true;transport.create(server,name.trim(),mode,level,::received)
            },modifier=Modifier.fillMaxWidth()){Text("إنشاء جلسة")}
            HorizontalDivider()
            OutlinedTextField(link,{link=it.take(1200)},label={Text("رابط الدعوة الكامل")},modifier=Modifier.fillMaxWidth())
            Button(enabled=link.startsWith("https://")&&name.isNotBlank()&&!busy,onClick={
                busy=true;transport.join(link,name.trim(),::received)
            },modifier=Modifier.fillMaxWidth()){Text("الانضمام")}
        }else{
            val id=identity!!
            Text(status,color=Color.LightGray)
            id.inviteUrl?.let{url->
                Text("شارك الرابط مع بالغين تثق بهم فقط. يتضمن مفتاح فك الوسائط.")
                OutlinedTextField(url,{},readOnly=true,modifier=Modifier.fillMaxWidth())
                Button(onClick={context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url),"مشاركة الدعوة"))}){Text("مشاركة الدعوة")}
            }
            OutlinedButton(onClick={transport.reconnect()}){Text("إعادة الاتصال")}
            val snapshot=state
            if(snapshot!=null){
                val roomStatus=snapshot.optString("status")
                val members=snapshot.getJSONArray("participants").let{array->(0 until array.length()).map{array.getJSONObject(it)}}
                val mine=members.find{it.optString("id")==id.participantId}
                Text(when(roomStatus){"WAITING"->"في انتظار المشاركين";"ACTIVE"->"الجلسة جارية";
                    "ENDED"->"انتهت الجلسة";"EXPIRED"->"انتهت صلاحيتها";"REVOKED"->"ملغاة";else->roomStatus},
                    style=MaterialTheme.typography.titleLarge)
                members.forEach{Text("${it.optString("name")}: ${it.optInt("score")} نقطة — ${if(it.optBoolean("connected"))"متصل" else "غير متصل"} — ${if(it.optBoolean("consent"))"موافق" else "بانتظار الموافقة"}")}
                if(roomStatus=="WAITING"){
                    if(mine?.optBoolean("consent")==false)Button(onClick={transport.action("consent",JSONObject().put("value",true))}){Text("أوافق على المشاركة")}
                    if(mine?.optBoolean("host")==true)Button(onClick={transport.action("start")},enabled=members.size>=2&&members.all{it.optBoolean("consent")&&it.optBoolean("connected")}){Text("ابدأ الجولات")}
                }
                if(roomStatus=="ACTIVE"){
                    Text("الجولة ${snapshot.optInt("round")+1}/${snapshot.optInt("maxRounds")}")
                    Card(Modifier.fillMaxWidth()){Text(snapshot.optString("prompt"),Modifier.padding(16.dp))}
                    if(snapshot.optString("actorId")==id.participantId){
                        OutlinedTextField(draft,{draft=it.take(500)},label={Text("رد نصي للمشاركين — ليس مشفرًا طرفيًا")},modifier=Modifier.fillMaxWidth())
                        OutlinedButton(enabled=draft.isNotBlank(),onClick={transport.action("message",JSONObject().put("body",draft));draft=""}){Text("إرسال رد")}
                        OutlinedButton(onClick={filePicker.launch("*/*")}){Text("إرسال صورة أو صوت أو فيديو مشفر (حتى ٨MB)")}
                        HorizontalDivider()
                        Text("بطاقة خاصة مشفرة (للشريكين فقط)", style=MaterialTheme.typography.titleMedium)
                        if(snapshot.optString("mode")=="couple") {
                            Row {
                                FilterChip(cardMode=="whispers",{cardMode="whispers"},label={Text("٣ همسات")})
                                Spacer(Modifier.width(8.dp))
                                FilterChip(cardMode=="scratch",{cardMode="scratch"},label={Text("كشف تدريجي")})
                            }
                            val required=if(cardMode=="whispers")3 else 2
                            repeat(required) { index ->
                                OutlinedTextField(cardTexts[index],{cardTexts[index]=it.take(350)},
                                    label={Text("الرسالة ${index+1}")},modifier=Modifier.fillMaxWidth())
                            }
                            OutlinedButton(enabled=cardTexts.take(required).all{it.isNotBlank()},onClick={
                                transport.uploadCard(cardMode,cardTexts.take(required).toList()){msg->status=msg}
                                for(i in cardTexts.indices)cardTexts[i]=""
                            }){Text("إرسال البطاقة مشفرة")}
                            Text("يتسلمها الشريك مرة واحدة. لا تُحفظ نسخة قابلة للفتح مجددًا على الخادم.",
                                style=MaterialTheme.typography.bodySmall,color=Color.LightGray)
                        }
                        Button(onClick={transport.action("complete")}){Text("أكملت التحدي")}
                        OutlinedButton(onClick={transport.action("skip")}){Text("تخطي")}
                    }
                    snapshot.optJSONArray("messages")?.let{messages->repeat(messages.length()){i->
                        val m=messages.getJSONObject(i);Text("${m.optString("author")}: ${m.optString("body")}")
                    }}
                    val media=snapshot.optJSONArray("media")
                    if(media!=null&&media.length()>0){
                        Text("الوسائط المشفرة المؤقتة",style=MaterialTheme.typography.titleMedium)
                        repeat(media.length()){index->
                            val m=media.getJSONObject(index)
                            OutlinedButton(enabled=m.optString("kind")!="card" || m.optString("authorId")!=id.participantId,
                                onClick={transport.download(m.getString("id"),m.getString("kind")){result->
                                result.onSuccess{preview=it;status="تم فك التشفير على هذا الجهاز"}
                                    .onFailure{status=it.message?:"تعذر عرض الوسيط"}
                            }}){Text("عرض ${when(m.optString("kind")){"image"->"صورة";"audio"->"صوت";"card"->"بطاقة خاصة";else->"فيديو"}} مشفر")}
                        }
                    }
                }
                if(roomStatus=="WAITING"||roomStatus=="ACTIVE"){
                    RemoteQuizPanel(snapshot,id,transport)
                    TextButton(onClick={transport.action("leave")}){Text("إنهاء الجلسة للجميع")}
                    if(mine?.optBoolean("host")==true)TextButton(onClick={transport.action("revoke")}){Text("إلغاء الدعوة")}
                }
            }
            preview?.let{MediaResultPreview(it,clear={preview=null})}
            TextButton(onClick={transport.close();remoteStore.clear();identity=null;state=null;preview=null}){Text("إغلاق الشاشة على هذا الهاتف")}
        }
    }
}

@Composable
private fun RemoteQuizPanel(snapshot:JSONObject,id:RemoteIdentity,transport:RemoteTransport){
    val members=snapshot.getJSONArray("participants")
    if(members.length()!=2 || !(0 until members.length()).all{members.getJSONObject(it).optBoolean("consent")})return
    val mine=(0 until members.length()).map{members.getJSONObject(it)}.find{it.optString("id")==id.participantId}
    val quiz=snapshot.optJSONObject("quiz")
    HorizontalDivider();Text("اختبار ثنائي",style=MaterialTheme.typography.titleLarge)
    if(quiz==null){
        if(mine?.optBoolean("host")==true){
            var chosen by remember{mutableIntStateOf(0)}
            ExpandedQuizzes.packs.forEachIndexed{index,pack->FilterChip(chosen==index,{chosen=index},label={Text(pack.name)})}
            Button(onClick={transport.action("quiz_open",JSONObject().put("pack",chosen))}){Text("بدء اختبار الشريكين")}
        }else Text("انتظر اختيار الاختبار من المضيف")
    }else{
        val quizId=quiz.optString("id");val pack=ExpandedQuizzes.packs[quiz.optInt("pack").coerceIn(0,6)]
        val answers=remember(quizId){mutableStateListOf<Int>()}
        Text(pack.name)
        if(mine?.optBoolean("host")==true)TextButton(onClick={transport.action("quiz_close")}){Text("إغلاق الاختبار")}
        val result=quiz.optJSONObject("result")
        if(result!=null){
            val scores=result.getJSONArray("scores")
            repeat(scores.length()){i->val score=scores.getJSONObject(i)
                val name=(0 until members.length()).map{members.getJSONObject(it)}.find{it.optString("id")==score.optString("id")}
                Text("${name?.optString("name")?:"مشارك"}: ${score.optInt("score")}/16")
            }
            Text("تطابقت ${result.optInt("agreement")} إجابات من أصل ٨")
        }else if(quiz.getJSONArray("submitted").let{array->(0 until array.length()).any{array.getString(it)==id.participantId}}){
            Text("وصلت إجاباتك. ننتظر المشارك الآخر.")
        }else if(answers.size<8){
            val indices=quiz.getJSONArray("indices")
            val question=pack.questions[indices.getInt(answers.size)]
            Text("السؤال ${answers.size+1}/8: ${question.text}")
            question.choices.forEachIndexed{index,option->
                OutlinedButton(modifier=Modifier.fillMaxWidth(),onClick={answers.add(index)}){Text(option)}
            }
        }else Button(onClick={
            transport.action("quiz_submit",JSONObject().put("quizId",quizId).put("answers",JSONArray(answers.toList())))
        }){Text("إرسال الإجابات للمقارنة")}
    }
    Text("أسئلة ترفيهية؛ يرى الخادم الإجابات أثناء احتساب النتيجة، ثم يحذف الإجابات الفردية من الذاكرة.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun MediaResultPreview(result:Pair<String,ByteArray>,clear:()->Unit){
    val context=LocalContext.current
    val mime=result.first
    val buffer=result.second
    if(mime=="application/vnd.sirr.card+json") {
        val content=remember(result){runCatching {
            val json=JSONObject(String(buffer,Charsets.UTF_8))
            require(json.getInt("version")==1 && json.getString("type") in listOf("whispers","scratch"))
            val lines=json.getJSONArray("lines")
            require(lines.length() in 1..3 &&
                (json.getString("type")!="whispers" || lines.length()==3))
            json.getString("type") to (0 until lines.length()).map{lines.getString(it)}.also{all->
                require(all.all{it.isNotBlank()&&it.length<=350})}
        }.getOrNull()}
        if(content!=null) RemoteCardViewer(content.first,content.second,clear)
        else Text("تعذر قراءة البطاقة المشفرة",color=MaterialTheme.colorScheme.error)
        return
    }
    val temp=remember(result){
        if(mime.startsWith("image/"))null else runCatching{File.createTempFile("sirr_media_",".${if(mime.contains("webm"))"webm" else if(mime=="audio/ogg")"ogg" else if(mime=="audio/mpeg")"mp3" else "mp4"}",context.cacheDir).apply{writeBytes(buffer)}}.getOrNull()
    }
    val player=remember(temp){MediaPlayer()}
    DisposableEffect(temp){onDispose{runCatching{player.release()};temp?.delete()}}
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
        Text("معاينة وسيط على هذا الجهاز",style=MaterialTheme.typography.titleMedium)
        if(temp==null&&!mime.startsWith("image/"))Text("تعذر إنشاء المعاينة المؤقتة",color=MaterialTheme.colorScheme.error)
        if(mime.startsWith("image/")){
            val bmp=remember(result){
                val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeByteArray(buffer,0,buffer.size,opts)
                if(opts.outWidth<=0||opts.outHeight<=0||opts.outWidth>20_000||opts.outHeight>20_000)null
                else {
                    var sample=1
                    while(opts.outWidth/sample>2048||opts.outHeight/sample>2048)sample*=2
                    runCatching{BitmapFactory.decodeByteArray(buffer,0,buffer.size,
                        BitmapFactory.Options().apply{inSampleSize=sample})?.asImageBitmap()}.getOrNull()
                }
            }
            if(bmp!=null)Image(bitmap=bmp,contentDescription="صورة مستلمة",modifier=Modifier.fillMaxWidth().height(200.dp))
        }else if(mime.startsWith("video/")){
            AndroidView(factory={ctx->VideoView(ctx).apply{setMediaController(MediaController(ctx))}},
                update={view->if(view.tag!=temp?.absolutePath){view.setVideoPath(temp?.absolutePath);view.tag=temp?.absolutePath}},
                modifier=Modifier.fillMaxWidth().height(200.dp))
        }else{
            OutlinedButton(onClick={runCatching{player.reset();player.setDataSource(requireNotNull(temp).absolutePath);player.prepare();player.start()}}){Text("تشغيل الصوت")}
        }
        Text("للصوت والفيديو تُنشأ نسخة مؤقتة داخل ذاكرة التطبيق وتُحذف عند الخروج. لا يمكن منع تسجيل الشاشة بكاميرا أخرى.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick=clear){Text("إغلاق المعاينة ومسح الملف")}
    }}
}

/** No remote read receipt is implied: ciphertext is removed at first authenticated download. */
@Composable
private fun RemoteCardViewer(type:String,lines:List<String>,clear:()->Unit) {
    var index by remember(type,lines){mutableIntStateOf(0)}
    var revealed by remember(type,lines){mutableStateOf(false)}
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(type=="whispers") "همسات خاصة" else "بطاقة كشف خاصة",style=MaterialTheme.typography.titleLarge)
        if(index<lines.size) {
            Text("الرسالة ${index+1} من ${lines.size}")
            if(type=="scratch") {
                key(index) { ScratchPanel(lines[index],onRevealed={revealed=true}) }
            } else Text(lines[index],style=MaterialTheme.typography.titleMedium)
            Button(enabled=type!="scratch" || revealed,onClick={index++;revealed=false}){Text(if(index==lines.lastIndex) "إنهاء العرض" else "التالي")}
        } else Text("انتهى العرض. لا يمكن استرجاع البطاقة من الخادم.")
        TextButton(onClick=clear){Text("إغلاق البطاقة")}
    } }
}
