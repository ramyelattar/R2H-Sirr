package com.r2h.sirr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.*

@Composable
internal fun QuizScreenV2() {
    var packIndex by remember { mutableIntStateOf(-1) }
    var pairing by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(8) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var phase by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(0) }
    val answersA = remember { mutableStateListOf<Int>() }
    val answersB = remember { mutableStateListOf<Int>() }
    var acceptedB by remember { mutableStateOf(false) }
    val packs = ExpandedQuizzes.packs
    fun reset() {
        packIndex = -1; phase = 0; step = 0
        answersA.clear(); answersB.clear()
        acceptedB = false; selectedIndices = emptyList()
    }
    Page("هتعرفوا بعض أكتر؟", "أسئلة خفيفة تفتح كلام مكنش على البال.") {
        if (packIndex < 0) {
            Text("اختاروا موضوع", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold)
            packs.forEachIndexed { index, quiz ->
                Surface(
                    onClick = {
                        packIndex = index; phase = 0; step = 0
                        answersA.clear(); answersB.clear(); selectedIndices = emptyList()
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(quiz.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("اختار اللي على مزاجك… وبعدين نبدأ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("←", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        } else {
            val quiz = packs[packIndex]
            if (phase == 0) {
                Text(quiz.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("تقدر تلعب لوحدك أو تقارن إجاباتك مع اللي معاك.", style = MaterialTheme.typography.bodyMedium)
                Row {
                    Checkbox(pairing, { pairing = it })
                    Text("هنلعب إحنا الاتنين")
                }
                Text("نختار كام سؤال؟", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            } else if (phase == 2 && pairing && !acceptedB) {
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
                                    if (phase == 1) answersA.add(i) else answersB.add(i)
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
                val first = QuizInsights.summary(answersA)
                Text("المفاجأة في إجاباتكم", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("دي مجرد مقارنة لطيفة، مش حكم على شخصيتك أو علاقتك.")
                Text("إجابات الشخص الأول:", style = MaterialTheme.typography.titleMedium)
                first.counts.forEachIndexed { option, count ->
                    Text("الاختيار ${option + 1}: ${count} من ${first.total} (${first.percentage(option)}٪)")
                }
                if (pairing) {
                    val comparison = QuizInsights.compare(answersA, answersB)
                    Text("إجابات الشخص التاني:", style = MaterialTheme.typography.titleMedium)
                    comparison.second.counts.forEachIndexed { option, count ->
                        Text("الاختيار ${option + 1}: ${count} من ${comparison.questions} (${comparison.second.percentage(option)}٪)")
                    }
                    Text("اتفقتوا في ${comparison.agreement} من ${comparison.questions} إجابة (${comparison.agreementPercent}٪)",
                        style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                Button(onClick = {
                    phase = 0; step = 0; answersA.clear(); answersB.clear()
                    acceptedB = false; selectedIndices = emptyList()
                }, modifier = Modifier.fillMaxWidth()) { Text("نلعب تاني؟") }
            }
            TextButton(onClick = { reset() }) { Text("اختار موضوع تاني") }
        }
    }
}
