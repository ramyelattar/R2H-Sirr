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

    @Test fun pairedComparisonPromptsAvoidSelfVsOtherActorLabels() {
        // "أنا" / "الشريك" choices about WHO acted cannot be treated as same meaning
        // for both respondents merely because the array index matches.
        val ambiguous = setOf("أنا غالبًا", "هو غالبًا", "التاني غالبًا",
            "أنا اللي بحب أجرب", "الطرف التاني")
        val all = packs.flatMap { it.questions }
        assertFalse(all.any { q -> q.choices.any { it in ambiguous } })
        assertFalse(all.any { it.text.startsWith("مين فيكم") })
    }

    @Test fun adultTopicsDiscussSexualityDirectlyWithoutRelabelingTheIntroPack() {
        // Editorial coverage guard, not a user-experience or content-safety assessment.
        val keywords = listOf("جنس", "حميم", "رغب", "جسدي", "ممارسة", "إثارة")
        val minimumPerPack = listOf(0, 6, 8, 12, 12, 12, 12)
        assertEquals(7, packs.size)
        packs.forEachIndexed { index, pack ->
            val questionsAboutSexuality = pack.questions.count { question ->
                keywords.any(question.text::contains)
            }
            assertTrue(
                "Adult sexuality missing in ${pack.name}: ${questionsAboutSexuality} found",
                questionsAboutSexuality >= minimumPerPack[index]
            )
        }
    }

    @Test fun questionsAcrossAllTopicsHaveUniqueText() {
        val questions = packs.flatMap { it.questions }.map { it.text }
        assertEquals(175, questions.toSet().size)
    }
}
