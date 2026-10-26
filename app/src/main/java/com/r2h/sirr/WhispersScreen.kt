package com.r2h.sirr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.net.Uri
import com.r2h.sirr.core.EphemeralEnvelope
import kotlinx.coroutines.delay

/** One-use on-device experience; this screen intentionally does not persist message text. */
@Composable
internal fun WhispersScreenV2() {
    val context = LocalContext.current
    val vault = remember { LocalVault(context.applicationContext) }
    val messages = remember { mutableStateListOf("", "", "") }
    var composeStep by remember { mutableIntStateOf(0) }
    var extras by remember { mutableStateOf(false) }
    val whisperStages = remember { listOf(
        "أول همسة", "الكلام اللي وراها", "المفاجأة الأخيرة"
    ) }
    val suggestions=remember { listOf(
        "كنت مستني فرصة أقولك حاجة من زمان.",
        "في تفاصيل في كلامك بتخليني أبتسم حتى بعد ما نمشي.",
        "لو هترد بهمسة واحدة، هتقول إيه؟", // Group 01
        "في رسالة مش لازم كل الناس تعرفها.",
        "وجودك بيخلّي القعدة العادية تبقى أحلى بكتير.",
        "تحب تسمع اعتراف كمان ولا نخلي الباقي مفاجأة؟", // Group 02
        "أول كلمة في الحكاية دي «وحشتني».",
        "مش عارف ليه كل أغنية هادية بتفكرني بيك.",
        "تختار أغنية لرسالتنا ولا تسيب الاختيار ليا؟", // Group 03
        "النهارده عندي سر أصغر من كلمة.",
        "أنا بحفظ طريقة ضحكتك أكتر من أي صورة.",
        "قول لي موقف ضحكنا فيه سوا ولسه فاكره.", // Group 04
        "لو الغموض له صوت، كان هيبقى زي همستك.",
        "في مرة كنت مستني تقول حاجة وسبقتك أنا بالضحك.",
        "نمثّل الموقف تاني ولا نكتفي بذكرى حلوة؟", // Group 05
        "مش كل اعتراف محتاج مقدمة طويلة.",
        "أنا بحب فيك إنك بتسمع الكلام للآخر.",
        "تحب أسألك سؤال صريح ولا سؤال خفيف؟", // Group 06
        "في حاجة صغيرة بتفرّق في يومي.",
        "لما بتفتكر حاجة قلتها من زمان، بحس إني مهم عندك.",
        "تفتكر إيه أكتر تفصيلة اتكلمنا عنها قريب؟", // Group 07
        "هقول لك سر والرد اختياري تمامًا.",
        "أنا بتكسف ساعات من المجاملات اللي بجد بكون محتاج أسمعها.",
        "أنهي كلمة لطيفة تحب تسمعها دلوقتي؟", // Group 08
        "الرسالة دي مكتوبة لمود مختلف.",
        "نفسي نلاقي وقت من غير إشعارات ولا استعجال.",
        "تختار قعدة هادية ولا خروجة مفاجئة؟", // Group 09
        "في نظرة واحدة ممكن تتحكي قصة.",
        "مرة بصيت لي وكنت متأكد إننا فاهمين بعض.",
        "لو اللحظة دي فيلم، هتسميه إيه؟", // Group 10
        "عندي اعتراف جريء لكن بسيط.",
        "أنا ساعات بفتح كلام في أي موضوع علشان بس أكلمك.",
        "لو عرفت ده بدري، كنت هترد بإيه؟", // Group 11
        "أكتر حاجة مخبيها مش سر كبير.",
        "أنا بفتكر أول مرة ضحكنا فيها على نفس النكتة.",
        "فاكر النكتة ولا نعمل واحدة جديدة؟", // Group 12
        "في سؤال ما سألتكهوش قبل كده.",
        "إيه الحاجة اللي بتحسسك إنك مرتاح مع شخص قريب؟",
        "تحب تجاوب بصراحة ولا تسيبها لوقت تاني؟", // Group 13
        "بعتلك ثلاث كلمات من غير شرح.",
        "«قرب، ضحكة، وذكرى» دول ملخص اللي في بالي.",
        "لو هتغيّر كلمة واحدة، تختار إيه؟", // Group 14
        "نفسي أقول جملة من غير ما أتوه.",
        "أنت من الناس اللي وجودهم بيغيّر شكل اليوم.",
        "ترد بمجاملة ولا تضحك وتعدّي؟", // Group 15
        "كل همسة ليها عنوان سري.",
        "عنوان همستي النهارده «حاجة ما اتقالتش».",
        "تختار تسمعها دلوقتي ولا في نهاية اللعبة؟", // Group 16
        "النهارده هخلي الاختيار ليك.",
        "أنا عندي قصة لطيفة عن أول انطباع أخدته عنك.",
        "تحب أحكيها ولا تبدأ أنت بحكايتك؟", // Group 17
        "خلي بالك، دي رسالة فيها تحدي صغير.",
        "عندنا دقيقة واحدة نقول فيها أجمل حاجة حصلت النهارده.",
        "تبدأ أنت ولا أبدأ أنا؟", // Group 18
        "الكلام الجاي مخصوص للي فاهمني.",
        "في حاجات بسيطة بتطمني لما أعرف إنك موجود.",
        "تحب تقول حاجة بتطمنك ولا نخليها سر؟", // Group 19
        "آخر رسالة قبل ما الصندوق يتقفل.",
        "في حكايات لسه جاية ومش لازم نستعجلها.",
        "نختار موعد لذكرى جديدة ولا نسيبها للصدفة؟", // Group 20
    ) }
    var sender by remember { mutableStateOf("") }
    var receiver by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var ttlHours by remember { mutableIntStateOf(3) }
    var photoId by remember { mutableStateOf<String?>(null) }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    val overrideBackgroundMusic = LocalSirrUserAudioOverride.current
    val player = remember { MediaPlayer() }
    // User-selected soundtrack must not continue playing after the app is backgrounded.
    val activity = context as? ComponentActivity
    DisposableEffect(activity, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                runCatching { if (player.isPlaying) player.pause() }
                playing = false
                overrideBackgroundMusic(false)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            runCatching { player.stop() }
            runCatching { player.release() }
            overrideBackgroundMusic(false)
        }
    }
    DisposableEffect(photoId) { val activeId = photoId; onDispose { vault.delete(activeId) } }
    var envelope by remember { mutableStateOf<EphemeralEnvelope?>(null) }
    val effects=rememberGameEffects()
    LaunchedEffect(page){if(page in 0..3)effects.cue(GameCue.REVEAL)}
    var error by remember { mutableStateOf("") }
    fun toggleAudio() {
        val selectedAudio = audioUri ?: return
        runCatching {
            if (playing) {
                player.stop()
                player.reset()
                playing = false
                overrideBackgroundMusic(false)
            } else {
                // Stop generated page music before starting the chosen personal soundtrack.
                overrideBackgroundMusic(true)
                player.reset()
                player.setDataSource(context, Uri.parse(selectedAudio))
                player.prepare()
                player.start()
                playing = true
            }
        }.onFailure {
            runCatching { player.reset() }
            error = "مش قادرين نشغّل الأغنية"
            playing = false
            overrideBackgroundMusic(false)
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val newId = vault.importPhoto(uri)
            vault.delete(photoId); photoId = newId; error = ""
        }.onFailure { error = "تعذر إدراج الصورة المحددة" }
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        audioUri = uri?.toString() // Session-scoped only. Media will not be copied/uploaded.
    }
    fun clear() {
        messages.indices.forEach { messages[it] = "" }
        sender = ""; receiver = ""; signature = ""; page = -1; envelope = null
        composeStep=0; extras=false
        runCatching { player.reset() }; playing = false
        overrideBackgroundMusic(false)
        vault.delete(photoId); photoId = null; audioUri = null; error = ""
    }
    LaunchedEffect(envelope, page) {
        val current = envelope ?: return@LaunchedEffect
        if (page < 0) return@LaunchedEffect
        val remaining = current.expiresAtMillis - System.currentTimeMillis()
        if (remaining > 0) delay(remaining)
        clear()
    }
    Page("همسة ليك وبس", "كل سر هيوصل في وقته… وملوش إعادة بعد ما يتفتح.") {
        if(page == -1) {
            SirrIntro(
                overline = "رسالة مش هتتقري مرتين",
                title = "3 همسات. سر واحد.",
                description = "اكتب 3 رسائل متتابعة. كل رسالة بتختفي بعد ما تعدّيها.",
                accent = Color(0xFF9F4D72),
                motif = "whispers"
            )
            SirrSection("0${composeStep + 1}", whisperStages[composeStep],
                "الخطوة ${composeStep + 1} من 3 · اكتب أو اختار من الاقتراحات")
            LinearProgressIndicator(
                progress = { (composeStep + 1).toFloat() / 3f },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = {
                // Preserve the original three-part arc in the author's chosen order.
                val groupStart = (0 until (suggestions.size / 3)).random() * 3
                repeat(3) { i -> messages[i] = suggestions[groupStart + i] }
                composeStep = 2
            }, modifier = Modifier.fillMaxWidth()) {
                Text("✦  مش عارف تكتب إيه؟ اختار 3 همسات جاهزين")
            }
            OutlinedTextField(
                messages[composeStep],
                { messages[composeStep] = it.take(350) },
                label = { Text(whisperStages[composeStep]) },
                placeholder = { Text("مثلاً: عندي حاجة عايز أقولهالك…") },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 6
            )
            if(composeStep<2) {
                SirrAction(
                    text = "اكتب الهمسة اللي بعدها",
                    onClick = { composeStep++ },
                    enabled = messages[composeStep].isNotBlank(),
                    explanation = if (messages[composeStep].isBlank())
                        "اكتب همسة واحدة على الأقل علشان تكمل" else null
                )
            } else {
                SirrAction(
                    text = "افتحوا أول همسة  ✦",
                    enabled = messages.all { it.isNotBlank() },
                    onClick = {
                        val now = System.currentTimeMillis()
                        envelope = EphemeralEnvelope(now, now + ttlHours * 3_600_000L)
                        page = 0
                    },
                    explanation = if (messages.any { it.isBlank() })
                        "لازم التلات همسات يكونوا جاهزين" else
                        "متأكد؟ كل همسة مش هتظهر تاني بعد ما تعدّيها"
                )
            }
            if(composeStep>0)TextButton(onClick={composeStep--}){Text("أرجع للهمسة اللي قبلها")}
            OutlinedButton(onClick={extras=!extras},modifier=Modifier.fillMaxWidth()){
                Text(if(extras)"اقفل الإضافات" else "حط اسم أو صورة أو أغنية في المفاجأة")
            }
            if(extras) {
                OutlinedTextField(sender,{sender=it.take(35)},label={Text("من مين؟ (اختياري)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(receiver,{receiver=it.take(35)},label={Text("لمين؟ (اختياري)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(signature,{signature=it.take(80)},label={Text("توقيعك (اختياري)")},modifier=Modifier.fillMaxWidth())
                Text("المفاجأة تفضل متاحة قد إيه؟")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    listOf(3,6,24).forEach{value->
                        FilterChip(ttlHours==value,onClick={ttlHours=value},label={Text("$value ساعات")})
                    }
                }
                OutlinedButton(onClick={pickPhoto.launch("image/*")}){
                    Text(if(photoId==null)"حط صورة في الآخر" else "غيّر الصورة")
                }
                if(photoId!=null)TextButton(onClick={vault.delete(photoId);photoId=null}){Text("امسح الصورة")}
                OutlinedButton(onClick={pickAudio.launch("audio/*")}){Text("اختار أغنية من موبايلك")}
                if (audioUri != null) {
                    OutlinedButton(onClick = { toggleAudio() }) {
                        Text(if (playing) "وقّف الأغنية" else "اسمع الأغنية")
                    }
                    Text("الأغنية بتفضل على الموبايل ده، ومش بتتبعت لحد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        } else if (page < 3) {
            if (receiver.isNotBlank()) Text("إلى $receiver", color = MaterialTheme.colorScheme.primary)
            SirrSection("0${page + 1}", whisperStages[page],
                "الهمسة دي هتختفي أول ما تكمل")
            SecretLetter(messages[page],page+1,3)
            if (audioUri != null) {
                OutlinedButton(onClick = { toggleAudio() },modifier=Modifier.fillMaxWidth()) {
                    Text(if (playing) "وقّف الموسيقى" else "شغّل موسيقى المفاجأة")
                }
            }
            SirrAction(
                text = if (page == 2) "افتح النهاية  ✦" else "افتح الهمسة اللي بعدها",
                onClick = {
                    messages[page] = "" // Destroy each message immediately after advancing.
                    page++
                },
                explanation = "الرسالة الحالية هتتمسح ومش هتترجع"
            )
            TextButton(onClick = { clear() }) { Text("اقفل وامسح") }
        } else {
            SirrIntro(
                "نهاية الحكاية",
                "ده كان آخر سر…",
                "اللي اتقال هنا فضل بينكم. تقدر تمسح الجلسة كلها دلوقتي."
            )
            if (sender.isNotBlank()) Text("من: $sender")
            if (signature.isNotBlank()) Text(signature)
            if (audioUri != null) {
                OutlinedButton(onClick = { toggleAudio() },modifier=Modifier.fillMaxWidth()) {
                    Text(if (playing) "وقّف الموسيقى" else "شغّل موسيقى المفاجأة")
                }
            }
            val photo = remember(photoId) { vault.readPhoto(photoId) }
            if (photo != null) Image(photo, contentDescription = "الصورة الختامية", modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit)
            Text("لما تقفل، الرسائل والصورة هيتمسحوا من الجلسة دي.", color = Color.LightGray)
            SirrAction(text = "اقفل وامسح السر", onClick = { clear() })
        }
    }
}
