package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/**
 * Source-only acceptance tests for the quiz state machine.
 * These have not been executed or compiled in this PR.
 */
class QuizSessionTest {
    private fun pack(): Quiz = Quiz(
        "موضوع اختبارات مصري",
        (1..25).map { n ->
            Question("سؤال جديد رقم $n؟", listOf("إجابة أولى $n", "إجابة تانية $n", "إجابة تالتة $n"))
        }
    )

    private fun session(paired: Boolean = false, count: Int = 8) =
        QuizSession.start("quiz_01", pack(), count, paired, Random(12))

    private fun fillFirst(start: QuizSession): QuizSession {
        var current = start
        repeat(current.questions.size) {
            current = current.answer(it % 3)
        }
        return current
    }

    @Test fun drawsDistinctQuestionsFromChosenTopic() {
        val run = session(count = 20)
        assertEquals(20, run.questions.size)
        assertEquals(20, run.questions.map { it.text }.toSet().size)
        assertEquals(QuizStage.FIRST, run.stage)
        assertEquals(0, run.position)
    }

    @Test fun cannotStartWithInvalidCountsOrMismatchedPack() {
        try {
            QuizSession.start("quiz_01", pack(), 7, false)
            fail("must require 8/12/20/25")
        } catch (_: IllegalArgumentException) {}
        try {
            QuizSession.start("wrong", pack(), 8, false)
            fail("must validate chosen pack id")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun firstResponseMovesForwardAndSkipRetainsAlignment() {
        val started = session()
        val first = started.answer(-1)
        assertEquals(1, first.position)
        assertEquals(listOf(-1), first.answersA)
        assertEquals(started.questions[1], first.currentQuestion)
        val answered = first.answer(2)
        assertEquals(listOf(-1, 2), answered.answersA)
    }

    @Test fun canUndoOwnLastAnswerWithoutChangingOthers() {
        val before = session(paired = true)
        val changed = before.answer(1).answer(-1).back()
        assertEquals(listOf(1), changed.answersA)
        assertEquals(1, changed.position)
        assertTrue(changed.answersB.isEmpty())
        assertEquals(QuizStage.FIRST, changed.stage)
    }

    @Test fun firstResponderCannotBackBeforeFirstQuestion() {
        try {
            session().back()
            fail("must not back outside bounds")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun singlePlayerFinishesWithoutSecondOrFalseMatchScore() {
        val final = fillFirst(session())
        assertEquals(QuizStage.RESULTS, final.stage)
        assertEquals(8, final.answersA.size)
        assertTrue(final.answersB.isEmpty())
        assertNull(final.comparison())
    }

    @Test fun pairedHandoffHidesFirstAnswersByStage() {
        val afterFirst = fillFirst(session(paired = true))
        assertEquals(QuizStage.HANDOFF, afterFirst.stage)
        assertEquals(8, afterFirst.answersA.size)
        assertTrue(afterFirst.answersB.isEmpty())
        val second = afterFirst.startPartner()
        assertEquals(QuizStage.SECOND, second.stage)
        assertEquals(0, second.position)
        try {
            second.startPartner()
            fail("must only hand off once")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun pairedResultsIgnoreSkippedByEitherPartner() {
        var run = fillFirst(session(paired = true)).startPartner()
        // First has answers 0,1,2,0,1,2,0,1
        listOf(0, -1, 1, -1, 1, 0, 2, -1).forEach {
            run = run.answer(it)
        }
        assertEquals(QuizStage.RESULTS, run.stage)
        val comparison = run.comparison()!!
        assertEquals(5, comparison.questions)
        assertEquals(2, comparison.agreement)
    }

    @Test fun allSkippedReturnsNoComparison() {
        var run = session(paired = true)
        repeat(8) { run = run.answer(-1) }
        run = run.startPartner()
        repeat(8) { run = run.answer(-1) }
        assertEquals(QuizStage.RESULTS, run.stage)
        assertNull(run.comparison())
    }

    @Test fun resultReviewCanMoveBackAndForwardWithoutChangingAnswers() {
        val finished = fillFirst(session())
        val firstA = finished.answersA.toList()
        val second = finished.revealNext()
        assertEquals(1, second.revealPosition)
        val previous = second.revealPrevious()
        assertEquals(0, previous.revealPosition)
        assertEquals(firstA, previous.answersA)
    }

    @Test fun finishedRoundCannotAcceptMoreAnswersOrGoBackAsQuestion() {
        val finished = fillFirst(session())
        try {
            finished.answer(1)
            fail("finished round must not accept more")
        } catch (_: IllegalArgumentException) {}
        try {
            finished.back()
            fail("results are not editable")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun stableSnapshotDoesNotFollowMutableOriginalChoices() {
        val choices = mutableListOf("واحد", "اتنين", "تلاتة")
        val original = Quiz("عنوان", List(25) { i ->
            Question("سؤال رقم ${i + 1}", choices)
        })
        val started = QuizSession.start("quiz_01", original, 8, false, Random(2))
        choices[0] = "اتغيرت"
        assertEquals("واحد", started.questions.first().choices.first())
    }


    private fun predictionStart() = QuizSession.start(
        "quiz_01", pack(), 8, paired = true, random = Random(12), guessPartner = true
    )

    @Test fun predictionModeMustHaveTwoParticipants() {
        try {
            QuizSession.start("quiz_01", pack(), 8, paired = false,
                random = Random(12), guessPartner = true)
            fail("prediction mode cannot be single-player")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun firstPlayerFinishesBeforePrivateForecastStage() {
        val run = fillFirst(predictionStart())
        assertEquals(QuizStage.PREDICT, run.stage)
        assertEquals(0, run.position)
        assertEquals(8, run.answersA.size)
        assertTrue(run.predictionsA.isEmpty())
        assertNull(run.predictionSummary())
    }

    @Test fun privatePredictionCanSkipAndUndoWithoutChangingOriginalAnswers() {
        val run = fillFirst(predictionStart())
        val withPredictions = run.predict(1).predict(-1).back()
        assertEquals(QuizStage.PREDICT, withPredictions.stage)
        assertEquals(1, withPredictions.position)
        assertEquals(listOf(1), withPredictions.predictionsA)
        assertEquals(run.answersA, withPredictions.answersA)
        assertTrue(withPredictions.answersB.isEmpty())
    }

    @Test fun forecastingMustFinishBeforeHandoffToPartner() {
        var run = fillFirst(predictionStart())
        repeat(8) { run = run.predict(it % 3) }
        assertEquals(QuizStage.HANDOFF, run.stage)
        assertEquals(8, run.predictionsA.size)
        assertNull(run.predictionSummary())
        run = run.startPartner()
        assertEquals(QuizStage.SECOND, run.stage)
        assertEquals(8, run.answersA.size)
    }

    @Test fun predictionResultsCountOnlyBothAnsweredMatches() {
        var run = fillFirst(predictionStart())
        listOf(0, 1, -1, 2, 2, 0, 1, 2).forEach { run = run.predict(it) }
        run = run.startPartner()
        listOf(0, 2, 1, 2, -1, 0, 1, 1).forEach { run = run.answer(it) }
        assertEquals(QuizStage.RESULTS, run.stage)
        assertNull(run.comparison())
        assertEquals(6, run.predictionSummary()!!.comparable)
        assertEquals(4, run.predictionSummary()!!.matched)
    }

    @Test fun predictionAllSkippedShowsNoComparableForecast() {
        var run = fillFirst(predictionStart())
        repeat(8) { run = run.predict(-1) }
        run = run.startPartner()
        repeat(8) { run = run.answer(0) }
        assertNull(run.predictionSummary())
        assertNull(run.comparison())
    }

    @Test fun predictionResultRejectsInvalidPartialRestoreState() {
        val run = fillFirst(predictionStart())
        try {
            run.copy(stage = QuizStage.HANDOFF)
            fail("cannot bypass required prediction phase")
        } catch (_: IllegalArgumentException) {}
        try {
            run.copy(stage = QuizStage.RESULTS)
            fail("cannot reveal either player's answers mid-game")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun malformedRestoredStateIsRejected() {
        val original = session()
        try {
            original.copy(stage = QuizStage.RESULTS)
            fail("cannot show results before completion")
        } catch (_: IllegalArgumentException) {}
        try {
            original.copy(answersA = listOf(5))
            fail("answer index must be one of -1, 0, 1, 2")
        } catch (_: IllegalArgumentException) {}
    }
}
