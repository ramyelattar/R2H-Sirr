package com.r2h.sirr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    fun reset() { packIndex = -1; phase = 0; step = 0; answersA.clear(); answersB.clear(); acceptedB = false; selectedIndices = emptyList() }
    Page("اختبارات البالغين", "سبعة مواضيع؛ عشرون سؤالًا عربيًا أصليًا لكل موضوع. النتائج للترفيه فقط.") {
        if (packIndex < 0) {
            packs.forEachIndexed { index, quiz ->
                Card(onClick = { packIndex = index; phase = 0; step = 0; answersA.clear(); answersB.clear() }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(quiz.name, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Text("${quiz.questions.size} سؤالًا — استعراض فردي أو مقارنة محلية")
                    }
                }
            }
        } else {
            val quiz = packs[packIndex]
            Text(quiz.name, style = MaterialTheme.typography.titleLarge)
            if (phase == 0 && step == 0) {
                Row { Checkbox(pairing, { pairing = it }); Text("مقارنة اختيارية بين شخصين على جهاز واحد") }
                Text("عدد الأسئلة")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8,12,20,25).forEach { n ->
                        FilterChip(questionCount == n, onClick={questionCount=n}, label={Text("$n")})
                    }
                }
                Button(onClick = { selectedIndices = quiz.questions.indices.shuffled().take(questionCount); phase = 1 }) { Text("بدء الاختبار") }
            } else if (phase == 2 && pairing && !acceptedB) {
                Text("أكمل المشارك الأول اختباره. لا تُعرض إجاباته للشخص الثاني.")
                Row { Checkbox(acceptedB, { acceptedB = it }); Text("المشارك الثاني يوافق على بدء الاختبار") }
                Button(onClick = { phase = 3; step = 0 }, enabled = acceptedB) { Text("بدء اختبار المشارك الثاني") }
            } else if (phase == 1 || phase == 3) {
                val question = quiz.questions[selectedIndices[step]]
                Text("${if (phase == 1) "المشارك الأول" else "المشارك الثاني"} — السؤال ${step + 1}/${selectedIndices.size}", color = MaterialTheme.colorScheme.primary)
                Text(question.text, style = MaterialTheme.typography.titleMedium)
                question.choices.forEachIndexed { i, answer ->
                    OutlinedButton(onClick = {
                        if (phase == 1) answersA.add(i) else answersB.add(i)
                        if (step + 1 == selectedIndices.size) { phase = if (phase == 1 && pairing) 2 else 4 }
                        else step++
                    }, modifier = Modifier.fillMaxWidth()) { Text(answer) }
                }
            } else if (phase == 4) {
                val first = QuizInsights.summary(answersA)
                Text("ملخص اختيارات المشارك الأول",style=MaterialTheme.typography.titleMedium)
                first.counts.forEachIndexed { option, count ->
                    Text("الاختيار ${option+1}: $count من ${first.total} (${first.percentage(option)}٪)")
                }
                if (pairing) {
                    val comparison = QuizInsights.compare(answersA, answersB)
                    Text("ملخص اختيارات المشارك الثاني",style=MaterialTheme.typography.titleMedium)
                    comparison.second.counts.forEachIndexed { option, count ->
                        Text("الاختيار ${option+1}: $count من ${comparison.questions} (${comparison.second.percentage(option)}٪)")
                    }
                    Text("اتفاق الإجابات: ${comparison.agreement} من ${comparison.questions} (${comparison.agreementPercent}٪)")
                    Text("هذه نسبة اتفاق وصفيّة وليست مقياس توافق نفسي أو عاطفي.")
                }
                Text("هذا ترفيه للحوار فقط وليس تشخيصًا أو مقياس توافق علميًا.", color = Color.LightGray)
                Button(onClick = { phase = 0; step = 0; answersA.clear(); answersB.clear(); acceptedB = false; selectedIndices = emptyList() }) { Text("إعادة الاختبار") }
            }
            TextButton(onClick = { reset() }) { Text("جميع الاختبارات") }
        }
    }
}
