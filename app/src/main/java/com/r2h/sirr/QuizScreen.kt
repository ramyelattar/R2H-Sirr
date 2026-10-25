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
    val packs = ExpandedQuizzes.packs
    val moodTitles = listOf("إنت من جوّه", "الحب على مزاجك", "إحنا شبه بعض؟",
        "الجاذبية والإشارات", "أمنيات متخبية", "راحة وحدود", "جرأة ولا صراحة؟")
    fun reset() {
        packIndex = -1; phase = 0; step = 0; revealStep = 0
        answersA = emptyList(); answersB = emptyList()
        acceptedB = false; selectedIndices = emptyList()
    }
    Page("هتعرفوا بعض أكتر؟", "أسئلة خفيفة تفتح كلام مكنش على البال.") {
        if (packIndex < 0) {
            Text("اختاروا موضوع", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold)
            MysteryHeader(
                "سبع أبواب",
                "كل باب وراه سؤال مختلف",
                "في حاجات بتتقال لما نسأل السؤال الصح.",
                "؟"
            )
            packs.forEachIndexed { index, _ ->
                QuizTopicTile(number = index + 1, title = moodTitles[index]) {
                    packIndex = index
                    phase = 0
                    step = 0
                    revealStep = 0
                    answersA = emptyList()
                    answersB = emptyList()
                    acceptedB = false
                    selectedIndices = emptyList()
                }
            }
        } else {
            val quiz = packs[packIndex]
            if (phase == 0) {
                Text(moodTitles[packIndex], style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("تقدر تلعب لوحدك أو تقارن إجاباتك مع اللي معاك.", style = MaterialTheme.typography.bodyMedium)
                Row {
                    Checkbox(pairing, { pairing = it })
                    Text("هنلعب إحنا الاتنين")
                }
                Text("نختار كام سؤال؟", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(8, 12, 20, 25).forEach { n ->
                        FilterChip(questionCount == n, onClick = { questionCount = n }, label = { Text("$n") })
                    }
                }
                Button(
                    onClick = {
                        selectedIndices = quiz.questions.indices.shuffled().take(questionCount)
                        phase = 1; step = 0
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("يلا نكتشف", fontWeight = FontWeight.Bold) }
            } else if (phase == 2 && pairing) {
                Text("الدور على الشخص التاني", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("الإجابات الأولى مخفية. سلّم الموبايل للطرف التاني لما يبقى جاهز.")
                Row {
                    Checkbox(acceptedB, { acceptedB = it })
                    Text("أنا موافق أبدأ")
                }
                Button(onClick = { phase = 3; step = 0 }, enabled = acceptedB,
                    modifier = Modifier.fillMaxWidth()) { Text("ابدأ دوري") }
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
                            OutlinedButton(
                                onClick = {
                                    if (phase == 1) answersA = answersA + i else answersB = answersB + i
                                    if (step + 1 == selectedIndices.size) {
                                        phase = if (phase == 1 && pairing) 2 else 4
                                    } else step++
                                },
                                shape = RoundedCornerShape(17.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp)
                            ) { Text(answer, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
                Text("اختار على طول… مفيش إجابة صح أو غلط.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else if (phase == 4) {
                val currentQuestion = quiz.questions[selectedIndices[revealStep]]
                MysteryHeader(
                    if (pairing) "مفاجأة الإجابات" else "آخر صفحة",
                    if (pairing) "قد إيه كنتوا شبه بعض؟" else "دي إجاباتك… من غير أحكام.",
                    "مفيش صح وغلط هنا. الحكاية في الكلام اللي هيبدأ بعد كل إجابة.",
                    "✦"
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
                        acceptedB = false; selectedIndices = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("نلعب جولة جديدة؟") }
            }
            TextButton(onClick = { reset() }) { Text("اختار موضوع تاني") }
        }
    }
}
