package com.r2h.sirr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.*

@Composable
internal fun QuizScreenV2() {
    var packIndex by rememberSaveable { mutableIntStateOf(-1) }
    var pairing by rememberSaveable { mutableStateOf(false) }
    var questionCount by rememberSaveable { mutableIntStateOf(8) }
    var selectedIndices by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var revealStep by rememberSaveable { mutableIntStateOf(0) }
    var answersA by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var answersB by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var acceptedB by rememberSaveable { mutableStateOf(false) }
    var acceptedContent by rememberSaveable { mutableStateOf(false) }
    val packs = ExpandedQuizzes.packs
    val moodTitles = listOf("إنت من جوّه", "الحب على مزاجك", "إحنا شبه بعض؟",
        "الجاذبية والإشارات", "أمنيات متخبية", "راحة وحدود", "جرأة ولا صراحة؟")
    val moodHints = listOf(
        "أسئلة تكتشف بيها نفسك من غير أحكام",
        "كلام عن الاهتمام والتفاصيل الصغيرة",
        "الحاجات اللي بتجمعكم… واللي بتفرقكم",
        "إشارات بنفهمها من غير كلام",
        "أحلام صغيرة محدش سأل عنها",
        "إزاي تقول آه أو لأ براحتك",
        "اختار الصراحة… والباقي عليكم"
    )
    fun reset() {
        packIndex = -1; phase = 0; step = 0; revealStep = 0
        answersA = emptyList(); answersB = emptyList()
        acceptedB = false; acceptedContent = false; selectedIndices = emptyList()
    }
    Page("هتعرفوا بعض أكتر؟", "أسئلة من الخفيف للجريء — اختاروا اللي يناسبكم.") {
        if (packIndex < 0) {
            SirrIntro(
                overline = "7 أبواب · 25 سؤال في كل باب",
                title = "إيه اللي لسه متقالش؟",
                description = "اختاروا باب… وكل إجابة ممكن تفتح كلام جديد.",
                accent = Color(0xFF64538A),
                motif = "quiz"
            )
            SirrSection("01", "اختاروا الباب", "كل باب ليه جو مختلف")
            packs.forEachIndexed { index, _ ->
                QuizTopicTile(number = index + 1, title = moodTitles[index],
                    subtitle = moodHints[index]) {
                    packIndex = index
                    phase = 0
                    step = 0
                    revealStep = 0
                    answersA = emptyList()
                    answersB = emptyList()
                    acceptedB = false
                    acceptedContent = false
                    selectedIndices = emptyList()
                }
            }
        } else {
            val quiz = packs[packIndex]
            if (phase == 0) {
                Text(moodTitles[packIndex], style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                SirrSection("02", "مين هيجاوب؟", "اللعب الفردي أو مقارنة من غير درجات توافق")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SirrOption("أنا لوحدي", !pairing, onClick = { pairing = false },
                        modifier = Modifier.weight(1f))
                    SirrOption("إحنا الاتنين", pairing, onClick = { pairing = true },
                        modifier = Modifier.weight(1f))
                }
                Text(
                    if (packIndex == 0 || packIndex == 6)
                        "تنبيه: الباب ده ممكن يحتوي أسئلة شخصية جدًا وصريحة للكبار. تقدر تتخطى أي سؤال أو تنهي الجولة."
                    else "بعض الأسئلة شخصية وقد تكون محرجة. مفيش إجابة إجبارية، والتخطي متاح في أي وقت.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                SirrConsent(
                    name = "أنا موافق أبدأ أسئلة الباب ده بعد ما عرفت طبيعتها",
                    checked = acceptedContent,
                    onChange = { acceptedContent = it }
                )
                SirrSection("03", "كام سؤال تحبوا؟", "ابدأوا بقليل… وكملوا لو حبيتوا")
                listOf(8, 12, 20, 25).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { n ->
                            SirrOption("$n سؤال", questionCount == n,
                                onClick = { questionCount = n }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                SirrAction(
                    text = "افتح أول سؤال  ✦",
                    enabled = acceptedContent,
                    explanation = if (acceptedContent) null else "لازم توافق على طبيعة الأسئلة قبل ما تبدأ",
                    onClick = {
                        selectedIndices = quiz.questions.indices.shuffled().take(questionCount)
                        phase = 1; step = 0
                    }
                )
            } else if (phase == 2 && pairing) {
                Text("الدور على الشخص التاني", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                SirrIntro("دور جديد", "اللي اتقال هيفضل سر.",
                    "الإجابات الأولى مش هتظهر. سلّم الموبايل للشخص التاني.")
                SirrConsent("الشخص التاني", acceptedB, onChange = { acceptedB = it })
                SirrAction(
                    text = "ابدأ دور الشخص التاني",
                    onClick = { phase = 3; step = 0 },
                    enabled = acceptedB,
                    explanation = if (acceptedB) null else "محتاجين موافقة الشخص التاني"
                )
            } else if (phase == 1 || phase == 3) {
                val question = quiz.questions[selectedIndices[step]]
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / selectedIndices.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("سؤال ${step + 1} من ${selectedIndices.size}",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(if (phase == 1) "دورك إنت" else "دور الشخص التاني",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(question.text, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        question.choices.forEachIndexed { i, answer ->
                            Surface(
                                onClick = {
                                    if (phase == 1) answersA = answersA + i else answersB = answersB + i
                                    if (step + 1 == selectedIndices.size) {
                                        phase = if (phase == 1 && pairing) 2 else 4
                                    } else step++
                                },
                                color = Color(0xFF362639),
                                shape = RoundedCornerShape(17.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFF8D637E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(13.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF5B3A51),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("${i + 1}",
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                            color = Color(0xFFFFDBC6),
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Text(answer, style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Text("اختار على طول… مفيش إجابة صح أو غلط.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else if (phase == 4) {
                val currentQuestion = quiz.questions[selectedIndices[revealStep]]
                SirrIntro(
                    if (pairing) "حكاية مشتركة" else "إجاباتك ليك",
                    if (pairing) "مش دايمًا لازم نتفق." else "إنت اخترت… والباقي حكاية.",
                    "مفيش صح وغلط. اللي يهمنا الكلام اللي يبدأ بعد كل إجابة.",
                    accent = Color(0xFF64538A)
                )
                if (pairing) {
                    val result = QuizInsights.compare(answersA, answersB)
                    Text(
                        "اتّفقتوا في ${result.agreement} من ${result.questions} إجابة",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("مش درجة توافق ولا اختبار شخصية — مجرد لعبة كلام.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                LinearProgressIndicator(
                    progress = { (revealStep + 1).toFloat() / selectedIndices.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("نكشف سؤال ${revealStep + 1} من ${selectedIndices.size}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(currentQuestion.text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("إنت اخترت: ${currentQuestion.choices[answersA[revealStep]]}",
                            style = MaterialTheme.typography.bodyLarge)
                        if (pairing) {
                            Text("اللي معاك اختار: ${currentQuestion.choices[answersB[revealStep]]}",
                                style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (answersA[revealStep] == answersB[revealStep])
                                    "✦  نفس الإجابة… افتحوا الكلام فيها!"
                                else "إجابتين مختلفتين… اسألوا بعض ليه.",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text("ممكن تحكي للي معاك ليه اخترت الإجابة دي.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (revealStep < selectedIndices.lastIndex) {
                    Button(
                        onClick = { revealStep++ },
                        shape = RoundedCornerShape(17.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("اكشف السؤال اللي بعده ✦") }
                } else {
                    Text("✦  كل حكاية بتبدأ بسؤال… عايزين جولة تانية؟",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(
                    onClick = {
                        phase = 0; step = 0; revealStep = 0
                        answersA = emptyList(); answersB = emptyList()
                        acceptedB = false; acceptedContent = false; selectedIndices = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("نلعب جولة جديدة؟") }
            }
            TextButton(onClick = { reset() }) { Text("اختار موضوع تاني") }
        }
    }
}
