package com.r2h.sirr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.r2h.sirr.core.*

/**
 * Seven Egyptian-Arabic, subjective conversation packs.
 * Local questions and answers are an encrypted snapshot, not an assessment
 * and not a remote/cloud session. Back and skip are valid for either player.
 */
@Composable
internal fun QuizScreenV2() {
    val context = LocalContext.current
    val vault = remember(context) { LocalVault(context.applicationContext) }
    var session by remember(vault) { mutableStateOf(QuizSessionStore.load(vault)) }
    var topic by remember { mutableIntStateOf(-1) }
    var paired by remember { mutableStateOf(false) }
    var guessPartner by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(8) }
    var acceptedContent by remember { mutableStateOf(false) }
    var acceptedPartner by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val packs = ExpandedQuizzes.packs
    // Titles come directly from the canonical generated quiz catalog.
    val topics = packs.map { it.name }
    val hints = listOf(
        "أسئلة تكتشف بيها نفسك من غير أحكام",
        "كلام عن الاهتمام والتفاصيل الصغيرة",
        "الحاجات اللي بتجمعكم… واللي بتفرقكم",
        "إشارات بنفهمها من غير كلام",
        "أحلام صغيرة محدش سأل عنها",
        "إزاي تقول آه أو لأ براحتك",
        "اختار الصراحة… والباقي عليكم"
    )
    fun persist(next: QuizSession) {
        runCatching { QuizSessionStore.save(vault, next) }
            .onSuccess { session = next; error = "" }
            .onFailure { error = "تعذر حفظ الجولة مشفّرة. المحاولة ما اتحسبتش؛ حاول تاني." }
    }
    fun clearRound() {
        runCatching { QuizSessionStore.clear(vault) }
            .onSuccess {
                session = null
                topic = -1
                paired = false
                guessPartner = false
                count = 8
                acceptedContent = false
                acceptedPartner = false
                showResetConfirm = false
                error = ""
            }
            .onFailure {
                showResetConfirm = false
                error = "تعذر مسح الإجابات. جرّب تاني قبل ما تبدأ جولة جديدة."
            }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("تمسح إجابات الجولة؟") },
            text = { Text("الإجابات الخاصة بالجولة دي هتتمسح من التخزين المحلي. مش هتقدر ترجعها بعد كده.") },
            confirmButton = {
                TextButton(onClick = { clearRound() }) { Text("امسح وابدأ من جديد") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("كمّل الجولة") }
            }
        )
    }

    Page("هتعرفوا بعض أكتر؟", "أسئلة مصرية من الخفيف للجريء — اختاروا اللي يناسبكم.") {
        val run = session
        if (run == null) {
            if (topic < 0) {
                SirrIntro(
                    overline = "7 أبواب · 25 سؤال في كل باب",
                    title = "إيه اللي لسه متقالش؟",
                    description = "اختاروا باب… وكل إجابة ممكن تفتح كلام جديد.",
                    accent = Color(0xFF64538A),
                    motif = "quiz"
                )
                SirrSection("01", "اختاروا الباب", "كل باب ليه جو مختلف")
                packs.forEachIndexed { index, _ ->
                    QuizTopicTile(
                        number = index + 1,
                        title = topics[index],
                        subtitle = hints[index]
                    ) {
                        topic = index
                        paired = false
                        guessPartner = false
                        count = 8
                        acceptedContent = false
                        error = ""
                    }
                }
            } else {
                Text(topics[topic], style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                SirrSection("02", "مين هيجاوب؟", "فردي أو مقارنة وصفية؛ مفيش صح وغلط")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SirrOption("أنا لوحدي", !paired, onClick = {
                        paired = false; guessPartner = false; acceptedContent = false
                    }, modifier = Modifier.weight(1f))
                    SirrOption("نقارن إجاباتنا", paired && !guessPartner, onClick = {
                        paired = true; guessPartner = false; acceptedContent = false
                    }, modifier = Modifier.weight(1f))
                }
                SirrOption(
                    title = "أتوقّع إجابات شريكي",
                    selected = paired && guessPartner,
                    onClick = { paired = true; guessPartner = true; acceptedContent = false },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (paired && guessPartner)
                        "الأول هتجاوب عن نفسك، وبعدها تتوقع إجابات شريكك. شريكك يجاوب من غير ما يشوف توقعاتك. النتيجة بتقيس عدد التوقعات المطابقة بس."
                    else if (paired)
                        "هتجاوبوا عن نفس الأسئلة كل واحد لوحده، ونشوف مواضع التشابه من غير درجات توافق."
                    else
                        "هتجاوب عن نفسك وتراجع إجاباتك في الآخر. كل إجابة اختيارية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "الأسئلة شخصية وممكن تكون محرجة. لكل واحد حق التخطي أو التوقف في أي وقت. المقارنة مش اختبار نفسي ولا مقياس للتوافق.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                SirrConsent(
                    name = "أنا موافق أبدأ بعد ما عرفت طبيعة الأسئلة",
                    checked = acceptedContent,
                    onChange = { acceptedContent = it }
                )
                SirrSection("03", "كام سؤال؟", "اختاروا عدد مناسب للقعدة")
                listOf(8, 12, 20, 25).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { n ->
                            SirrOption("$n سؤال", count == n,
                                onClick = { count = n }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                SirrAction(
                    text = "افتح أول سؤال ✦",
                    enabled = acceptedContent,
                    explanation = if (!acceptedContent) "محتاج موافقتك على طبيعة الأسئلة" else null,
                    onClick = {
                        persist(QuizSession.start(
                            packId = "quiz_%02d".format(java.util.Locale.US, topic + 1),
                            quiz = packs[topic], count = count, paired = paired,
                            guessPartner = guessPartner
                        ))
                    }
                )
                TextButton(onClick = { topic = -1; acceptedContent = false }) {
                    Text("اختار موضوع تاني")
                }
            }
        } else {
            when (run.stage) {
                QuizStage.FIRST, QuizStage.PREDICT, QuizStage.SECOND -> {
                    val isFirst = run.stage == QuizStage.FIRST
                    val predicting = run.stage == QuizStage.PREDICT
                    val question = run.currentQuestion
                    SirrSection(
                        "${run.position + 1}", run.packName,
                        if (predicting) "التوقعات اختيارية ومش هتظهر لشريكك قبل ما يجاوب."
                        else "كل سؤال اختياري. اللي مش مناسب ممكن يتعدّى من غير إجابة."
                    )
                    Text(
                        when {
                            predicting -> "تفتكر شريكك هيختار إيه؟"
                            isFirst -> "دور الشخص الأول"
                            else -> "دور الشخص التاني"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { (run.position + 1).toFloat() / run.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("سؤال ${run.position + 1} من ${run.questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.fillMaxWidth()
                            .r2hGlass(accent = Color(0xFFD4A5F1), corner = 26.dp, lift = 16.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(question.text, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold)
                            question.choices.forEachIndexed { i, choice ->
                                Surface(
                                    onClick = {
                                        persist(if (predicting) run.predict(i) else run.answer(i))
                                    },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(17.dp),
                                    modifier = Modifier.fillMaxWidth()
                                        .r2hGlass(accent = Color(0xFFE0ABD7), corner = 17.dp, lift = 7.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                                    ) {
                                        Text("${i + 1}", color = Color(0xFFFFDBC6),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp))
                                        Text(choice, style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            persist(if (predicting) run.predict(-1) else run.answer(-1))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (predicting) "مش متأكد؟ عدّي التوقع" else "مش حابب أجاوب؟ عدّي السؤال") }
                    if (run.position > 0) {
                        TextButton(
                            onClick = { persist(run.back()) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ارجع للسؤال اللي فات وغيّر إجابتي") }
                    }
                    Text(
                        if (predicting)
                            "خمن بهدوء. الشريك مش مطالب يختار نفس اللي توقعته."
                        else "مفيش إجابة صح أو غلط. الرجوع بيخص إجاباتك في دورك بس.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                QuizStage.HANDOFF -> {
                    SirrIntro(
                        overline = "تبادل الأدوار",
                        title = "الدور على الشخص التاني",
                        description = if (run.guessPartner)
                            "إجابات الشخص الأول وتوقعاته مش هتظهر هنا. سلّم الموبايل للشخص التاني وهو يختار بنفسه."
                        else "إجابات الشخص الأول متخزنة مشفّرة ومش هتظهر هنا. سلّمه الموبايل لما يكون جاهز.",
                        accent = Color(0xFF64538A),
                        motif = "quiz"
                    )
                    SirrConsent(
                        name = "الشخص التاني موافق يشارك",
                        checked = acceptedPartner,
                        onChange = { acceptedPartner = it }
                    )
                    SirrAction(
                        text = "ابدأ دور الشخص التاني",
                        enabled = acceptedPartner,
                        explanation = if (!acceptedPartner) "الشخص التاني لازم يوافق بنفسه" else null,
                        onClick = {
                            persist(run.startPartner())
                            acceptedPartner = false
                        }
                    )
                }

                QuizStage.RESULTS -> {
                    val current = run.currentQuestion
                    val a = run.answersA[run.revealPosition]
                    val b = if (run.paired) run.answersB[run.revealPosition] else -1
                    SirrIntro(
                        overline = if (run.paired) "حكاية مشتركة" else "إجاباتك ليك",
                        title = if (run.paired) "مش لازم تتفقوا في كل حاجة" else "إنت اخترت… والباقي حكاية",
                        description = "دي إجابات ومواقف للنقاش، مش تشخيص أو درجة توافق.",
                        accent = Color(0xFF64538A),
                        motif = "quiz"
                    )
                    Text("الشخص الأول أجاب عن ${run.answersA.count { it >= 0 }} من ${run.questions.size}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (run.paired) {
                        Text("الشخص التاني أجاب عن ${run.answersB.count { it >= 0 }} من ${run.questions.size}",
                            style = MaterialTheme.typography.bodyMedium)
                        if (run.guessPartner) {
                            val prediction = run.predictionSummary()
                            Text(
                                if (prediction == null) "مفيش توقعات اتجاوب عليها الطرف التاني علشان نقارن"
                                else "توقعت صح في ${prediction.matched} من ${prediction.comparable} توقع قابل للمقارنة",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("التوقع المتخطّى أو السؤال اللي شريكك عدّاه مش بيتحسب. ده لعب وتخمين، مش تقييم للعلاقة.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            val comparison = run.comparison()
                            Text(
                                if (comparison == null) "مفيش أسئلة مشتركة اتجاوبت علشان نقارن"
                                else "اتفقتوا في ${comparison.agreement} من ${comparison.questions} سؤال اتجاوب من الاتنين",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("التخطي خارج المقارنة، والنتيجة وصفية بس.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (run.revealPosition + 1).toFloat() / run.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("كشف السؤال ${run.revealPosition + 1} من ${run.questions.size}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                            .r2hGlass(accent = Color(0xFFFFAEDC), corner = 26.dp, lift = 15.dp)
                    ) {
                        Column(Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(current.text, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                if (a >= 0) "الشخص الأول اختار: ${current.choices[a]}"
                                else "الشخص الأول عدّى السؤال ده",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (run.paired) {
                                Text(
                                    if (b >= 0) "الشخص التاني اختار: ${current.choices[b]}"
                                    else "الشخص التاني عدّى السؤال ده",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (run.guessPartner) {
                                    val guess = run.predictionsA[run.revealPosition]
                                    Text(
                                        if (guess >= 0) "توقع الشخص الأول: ${current.choices[guess]}"
                                        else "الشخص الأول ما توقعش إجابة للسؤال ده",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        when {
                                            guess < 0 || b < 0 -> "السؤال ده مش داخل في حساب التوقعات."
                                            guess == b -> "التوقع طلع مطابق. اتكلموا عن السبب!"
                                            else -> "التوقع كان مختلف. اسألوا بعض إيه اللي فاجأكم."
                                        },
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                } else {
                                    Text(
                                        when {
                                            a < 0 || b < 0 -> "السؤال ده مش داخل في المقارنة علشان فيه تخطي."
                                            a == b -> "نفس الاختيار… افتحوا الكلام فيه!"
                                            else -> "اختيارين مختلفين… اسألوا بعض ليه."
                                        },
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            } else {
                                Text(
                                    if (a >= 0) "تقدر تحكي ليه اخترت الإجابة دي لو حابب."
                                    else "حقك تعدّي من غير ما تشرح السبب.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            enabled = run.revealPosition > 0,
                            onClick = { persist(run.revealPrevious()) },
                            modifier = Modifier.weight(1f)
                        ) { Text("السابق") }
                        Button(
                            enabled = run.revealPosition < run.questions.lastIndex,
                            onClick = { persist(run.revealNext()) },
                            modifier = Modifier.weight(1f)
                        ) { Text("التالي") }
                    }
                    Text("النتيجة محفوظة على الموبايل. امسحها من «جولة جديدة» لما تخلصوا.")
                }
            }
            HorizontalDivider()
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (run.stage == QuizStage.RESULTS)
                "جولة جديدة — امسح الإجابات القديمة"
                else "إنهاء الجولة ومسح الإجابات") }
        }
        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
        }
    }
}
