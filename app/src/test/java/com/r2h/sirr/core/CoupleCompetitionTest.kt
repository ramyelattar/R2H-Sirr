package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CoupleCompetitionTest {
    private fun competition(minutes: Int = 30) =
        CoupleCompetition("المكافأة اللي اتفقنا عليها", minutes, 1_700_000_000_000L)

    private fun game(): LocalGame = SessionFactory.create(
        listOf("الأول", "التاني"), Intensity.SOFT, false, Random(3),
        roundCount = 6, competition = competition()
    )

    private fun tiedGame(): LocalGame {
        var result = game()
        repeat(6) { result = result.resolve(CoupleOutcome.AGREED_SKIP) }
        return result
    }

    @Test fun scoringExactlyPlusTenMinusTenOrZero() {
        val start = game()
        val first = start.turns[0].actorIndex
        val second = start.turns[1].actorIndex
        assertNotEquals(first, second)
        val completed = start.resolve(CoupleOutcome.COMPLETED)
        assertEquals(10, completed.scores[first])
        assertEquals(0, completed.skipped[first])
        val notCompleted = completed.resolve(CoupleOutcome.NOT_COMPLETED)
        assertEquals(-10, notCompleted.scores[second])
        assertEquals(1, notCompleted.skipped[second])
        val agreed = notCompleted.resolve(CoupleOutcome.AGREED_SKIP)
        assertEquals(notCompleted.scores, agreed.scores)
        assertEquals(3, agreed.competition!!.outcomes.size)
        assertEquals(1, agreed.skipped[agreed.turns[2].actorIndex])
    }

    @Test fun turnsAlternateForTwoPlayers() {
        val all = game().turns
        assertTrue(all.zipWithNext().all { (a, b) -> a.actorIndex != b.actorIndex })
    }

    @Test fun rewardAndTimeLimitAreValidated() {
        assertFalse(competition(30).timeExpired(1_700_000_000_000L + 29 * 60_000L))
        assertTrue(competition(30).timeExpired(1_700_000_000_000L + 30 * 60_000L))
        assertFalse(competition(0).timeExpired(Long.MAX_VALUE))
        try {
            CoupleCompetition("", 30, 1L)
            fail("Empty reward must be rejected")
        } catch (_: IllegalArgumentException) { }
        try {
            CoupleCompetition("جايزة", 17, 1L)
            fail("Unsupported duration must be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun tiedScoresRequireSharedChallengeFirst() {
        val tied = tiedGame()
        assertTrue(tied.finished)
        assertTrue(tied.tiePending)
        assertNull(tied.winnerIndex)
        val winner = tied.resolveSharedTieBreak(listOf(true, false))
        assertEquals(0, winner.winnerIndex)
        assertEquals(TieStage.DECIDED, winner.competition!!.tieBreak!!.stage)
        assertEquals(tied.scores, winner.scores)
    }

    @Test fun sharedTieContinuesToKnowledgeWhenBothDoSameThing() {
        for (results in listOf(listOf(true, true), listOf(false, false))) {
            val next = tiedGame().resolveSharedTieBreak(results)
            assertEquals(TieStage.KNOWLEDGE, next.competition!!.tieBreak!!.stage)
            assertNull(next.winnerIndex)
        }
    }

    @Test fun knowledgeQuestionsAreRegisteredBeforeJudging() {
        val knowledge = tiedGame().resolveSharedTieBreak(listOf(true, true))
        try {
            knowledge.resolveKnowledgePair(listOf(true, false))
            fail("Cannot grade before questions are registered")
        } catch (_: IllegalArgumentException) { }
        val prepared = knowledge.registerKnowledgeQuestions(listOf(
            KnowledgePrompt("إيه أكتر أكلة بحبها؟", "ملوخية"),
            KnowledgePrompt("فاكر أول خروجة فين؟", "الإسكندرية")
        ))
        assertEquals(2, prepared.competition!!.tieBreak!!.questions.size)
        val winner = prepared.resolveKnowledgePair(listOf(false, true))
        assertEquals(1, winner.winnerIndex)
        assertEquals(TieStage.DECIDED, winner.competition!!.tieBreak!!.stage)
    }

    @Test fun knowledgeTieNeedsNewQuestionsAndPreservesRoundNumber() {
        val questions = listOf(
            KnowledgePrompt("أنا بحب أنهي لون؟", "أزرق"),
            KnowledgePrompt("أول فيلم شوفناه سوا؟", "فيلم قديم")
        )
        val base = tiedGame().resolveSharedTieBreak(listOf(false, false))
            .registerKnowledgeQuestions(questions)
        val tiedAgain = base.resolveKnowledgePair(listOf(true, true))
        assertEquals(2, tiedAgain.competition!!.tieBreak!!.knowledgeRound)
        assertTrue(tiedAgain.competition!!.tieBreak!!.questions.isEmpty())
        assertTrue(tiedAgain.tiePending)
        val followup = tiedAgain.registerKnowledgeQuestions(questions)
            .resolveKnowledgePair(listOf(true, false))
        assertEquals(0, followup.winnerIndex)
    }

    @Test fun earlySettlementDoesNotPenalizeUnplayedRounds() {
        val first = game().resolve(CoupleOutcome.AGREED_SKIP)
        val closed = first.settleEarly()
        assertTrue(closed.finished)
        assertEquals(1, closed.round)
        assertEquals(listOf(0, 0), closed.scores)
        assertTrue(closed.tiePending)
        assertEquals(first.competition!!.outcomes, closed.competition!!.outcomes)
    }

    @Test fun leavingDoesNotDeclareWinnerOrAwardReward() {
        val stopped = game().resolve(CoupleOutcome.COMPLETED).leave()
        assertTrue(stopped.finished)
        assertFalse(stopped.competitionEnded)
        assertNull(stopped.winnerIndex)
        assertFalse(stopped.tiePending)
    }

    @Test fun originalGroupScoringRemainsOnePoint() {
        val group = SessionFactory.create(
            listOf("أ", "ب", "ج", "د"), Intensity.SOFT, true, Random(3)
        )
        val resolved = group.resolve(true)
        assertEquals(1, resolved.scores.sum())
        assertNull(resolved.competition)
    }

    @Test fun cannotApplyNewScoringToLegacySession() {
        val legacy = SessionFactory.create(
            listOf("أ", "ب"), Intensity.SOFT, false, Random(4)
        )
        try {
            legacy.resolve(CoupleOutcome.COMPLETED)
            fail("Legacy session must keep legacy resolver")
        } catch (_: IllegalArgumentException) { }
    }
}
