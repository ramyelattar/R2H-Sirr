package com.r2h.sirr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.r2h.sirr.core.CoupleOutcome
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.r2h.sirr.core.CoupleTieBreak
import com.r2h.sirr.core.KnowledgePrompt
import com.r2h.sirr.core.LocalGame
import com.r2h.sirr.core.TieStage

/**
 * Tie break is entered only after normal competition scoring has finished.
 * All committed questions and expected answers persist through the encrypted game record.
 */
@Composable
internal fun CoupleCompetitionPanel(game: LocalGame, persist: (LocalGame) -> Boolean) {
    val rules = game.competition ?: return
    var saveError by remember(game) { mutableStateOf(false) }
    fun save(next: LocalGame) { saveError = !persist(next) }
    Text("النتيجة النهائية", style = MaterialTheme.typography.titleLarge)
    game.playerNames.forEachIndexed { index, name ->
        Text("$name — ${game.scores[index]} نقطة")
        val rounds = rules.outcomes.withIndex().filter { (round, _) ->
            game.turns[round].actorIndex == index
        }.map { it.value }
        val completed = rounds.count { it == CoupleOutcome.COMPLETED }
        val uncompleted = rounds.count { it == CoupleOutcome.NOT_COMPLETED }
        val agreed = rounds.count { it == CoupleOutcome.AGREED_SKIP }
        Text("تم: $completed · لم يتم: $uncompleted · بالاتفاق: $agreed",
            style = MaterialTheme.typography.bodySmall)
    }
    if (game.ended) {
        Text("الجلسة اتوقفت من غير إعلان فائز. مفيش مكافأة مستحقة.")
        return
    }
    val winner = game.winnerIndex
    if (winner != null) {
        SirrIntro(
            overline = "بطل الجلسة",
            title = "الفائز: ${game.playerNames[winner]}",
            description = "الجايزة اللي اتفقتوا عليها: ${rules.reward}",
            accent = Color(0xFF9F4D72),
            motif = "couple"
        )
        Text("المكافأة تتنفذ بموافقة الطرفين، ومن حق أي طرف يغيّر رأيه.")
        return
    }
    if (!game.tiePending) return
    val tie = rules.tieBreak ?: CoupleTieBreak()
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
    Text(
        if (tie.stage == TieStage.SHARED_CHALLENGE)
            "النقاط متعادلة — هنحسمها بتحدي مشترك."
        else "التعادل مستمر — مين يعرف التاني أكتر؟",
        style = MaterialTheme.typography.titleMedium
    )
    when (tie.stage) {
        TieStage.SHARED_CHALLENGE -> {
            Text("تحدي كسر التعادل: ${rules.tieChallenge}")
            Text("من ينفذه لوحده يفوز؛ لو الاتنين نفذوه أو الاتنين ما نفذوهوش ننتقل للأسئلة.")
            var doneA by remember(game.round) { mutableStateOf(false) }
            var doneB by remember(game.round) { mutableStateOf(false) }
            SirrConsent(game.playerNames[0] + " نفّذ التحدي", doneA, { doneA = it })
            SirrConsent(game.playerNames[1] + " نفّذ التحدي", doneB, { doneB = it })
            Button(
                onClick = { save(game.resolveSharedTieBreak(listOf(doneA, doneB))) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("سجّل نتيجة التحدي المشترك") }
        }
        TieStage.KNOWLEDGE -> {
            Text("مين يعرف شريكه أكتر؟ جولة الأسئلة رقم ${tie.knowledgeRound}")
            if (tie.questions.isEmpty()) {
                var questionA by remember(tie.knowledgeRound) { mutableStateOf("") }
                var answerA by remember(tie.knowledgeRound) { mutableStateOf("") }
                var questionB by remember(tie.knowledgeRound) { mutableStateOf("") }
                var answerB by remember(tie.knowledgeRound) { mutableStateOf("") }
                Text("${game.playerNames[0]}: اكتب سؤال عن نفسك وإجابته الصحيحة")
                OutlinedTextField(questionA, { questionA = it.take(180) },
                    label = { Text("سؤالك للشريك") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(answerA, { answerA = it.take(140) },
                    label = { Text("الإجابة المرجعية — مخفية أثناء الكتابة") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Text("${game.playerNames[1]}: اكتب سؤال عن نفسك وإجابته الصحيحة")
                OutlinedTextField(questionB, { questionB = it.take(180) },
                    label = { Text("سؤالك للشريك") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(answerB, { answerB = it.take(140) },
                    label = { Text("الإجابة المرجعية — مخفية أثناء الكتابة") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Text("كل إجابة هتتحفظ مشفّرة قبل عرض السؤال للطرف التاني. ما تبصّوش على إجابات بعض.")
                val valid = questionA.trim().length in 6..180 &&
                    questionB.trim().length in 6..180 &&
                    answerA.isNotBlank() && answerB.isNotBlank()
                Button(
                    enabled = valid,
                    onClick = { save(game.registerKnowledgeQuestions(listOf(
                        KnowledgePrompt(questionA, answerA),
                        KnowledgePrompt(questionB, answerB)
                    ))) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ثبّت السؤالين وابدأ الإجابة") }
            } else {
                var answerByA by remember(tie.knowledgeRound) { mutableStateOf("") }
                var answerByB by remember(tie.knowledgeRound) { mutableStateOf("") }
                var showReference by remember(tie.knowledgeRound) { mutableStateOf(false) }
                var correctA by remember(tie.knowledgeRound) { mutableStateOf(false) }
                var correctB by remember(tie.knowledgeRound) { mutableStateOf(false) }
                Text("سؤال ${game.playerNames[1]} إلى ${game.playerNames[0]}: ${tie.questions[1].question}")
                OutlinedTextField(answerByA, { answerByA = it.take(200) },
                    label = { Text("إجابة ${game.playerNames[0]}") }, modifier = Modifier.fillMaxWidth(),
                    readOnly = showReference)
                Text("سؤال ${game.playerNames[0]} إلى ${game.playerNames[1]}: ${tie.questions[0].question}")
                OutlinedTextField(answerByB, { answerByB = it.take(200) },
                    label = { Text("إجابة ${game.playerNames[1]}") }, modifier = Modifier.fillMaxWidth(),
                    readOnly = showReference)
                if (!showReference) {
                    Button(
                        enabled = answerByA.isNotBlank() && answerByB.isNotBlank(),
                        onClick = { showReference = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("اقفل الإجابات واكشف المرجع للتصحيح") }
                } else {
                    Text("إجابة ${game.playerNames[0]}: $answerByA")
                    Text("الإجابة المرجعية: ${tie.questions[1].expectedAnswer}")
                    SirrConsent("${game.playerNames[0]} جاوب صح", correctA, { correctA = it })
                    Text("إجابة ${game.playerNames[1]}: $answerByB")
                    Text("الإجابة المرجعية: ${tie.questions[0].expectedAnswer}")
                    SirrConsent("${game.playerNames[1]} جاوب صح", correctB, { correctB = it })
                    Text("لو الاتنين صح أو الاتنين غلط، هتبدأ جولة بأسئلة جديدة.")
                    Button(
                        onClick = { save(game.resolveKnowledgePair(listOf(correctA, correctB))) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("اعتمد التصحيح وحدد الفائز") }
                }
            }
        }
        TieStage.DECIDED -> Unit
    }
    OutlinedButton(
        onClick = { save(game.leave()) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("إنهاء الجلسة بالتعادل — من غير فائز أو مكافأة") }
    if (saveError) {
        Text("تعذر حفظ نتيجة كسر التعادل بأمان. حاول تاني.", color = MaterialTheme.colorScheme.error)
    }
}
