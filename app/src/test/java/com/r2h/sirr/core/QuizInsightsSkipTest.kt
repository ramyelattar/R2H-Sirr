package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

/** Pure JVM tests; -1 is the user's explicit unanswered/skip marker. */
class QuizInsightsSkipTest {
    @Test fun skipsDoNotCountAsAnswersOrAccidentalAgreements() {
        val result = QuizInsights.compareAnswered(
            listOf(0, -1, 2, -1, 1),
            listOf(0, 2, 1, -1, -1)
        )
        assertNotNull(result)
        assertEquals(2, result!!.questions)
        assertEquals(1, result.agreement)
        assertEquals(50, result.agreementPercent)
    }

    @Test fun fullySkippedOrNonOverlappingAnswersHaveNoScore() {
        assertNull(QuizInsights.compareAnswered(listOf(-1, 0), listOf(1, -1)))
        assertNull(QuizInsights.compareAnswered(listOf(-1, -1), listOf(-1, -1)))
    }

    @Test fun originalCompareStillRequiresThreeChoiceAnswerIndices() {
        assertEquals(2, QuizInsights.compare(listOf(1, 0), listOf(1, 0)).questions)
        try {
            QuizInsights.compareAnswered(listOf(-2, 1), listOf(0, 1))
            fail("unexpected answer index was accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
