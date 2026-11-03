package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

/** Source acceptance requirements only. No build or test execution has occurred. */
class QuizContentV4Test {
    private val packs get() = ExpandedQuizzes.packs

    @Test fun sevenTopicsHave25DistinctQuestionsEach() {
        assertEquals(7, packs.size)
        assertTrue(packs.all { it.questions.size == 25 })
        assertEquals(175, packs.flatMap { it.questions }.size)
    }

    @Test fun eachQuestionHasThreeUsefulNonduplicatedChoices() {
        val all = packs.flatMap { it.questions }
        assertTrue(all.all { q ->
            q.text.length in 5..240 &&
                q.text.isNotBlank() && q.choices.size == 3 &&
                q.choices.all { it.isNotBlank() && it.trim().length >= 5 } &&
                q.choices.distinct().size == 3
        })
        assertEquals(525, all.sumOf { it.choices.size })
    }

    @Test fun topicNamesCorrespondToTheDisplayedSevenCategories() {
        assertEquals(
            listOf(
                "إنت من جوّه", "الحب على مزاجك", "إحنا شبه بعض؟",
                "الجاذبية والإشارات", "أمنيات متخبية",
                "راحة وحدود", "جرأة ولا صراحة؟"
            ),
            packs.map { it.name }
        )
    }

    @Test fun questionsAcrossAllTopicsHaveUniqueText() {
        val questions = packs.flatMap { it.questions }.map { it.text }
        assertEquals(175, questions.toSet().size)
    }
}
