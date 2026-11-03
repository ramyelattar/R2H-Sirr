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
        val now = 1_700_000_000_000L
        val started = start.startRoundClock(now)
        val completed = started.resolve(CoupleOutcome.COMPLETED,
            now + start.turns[0].prompt.durationSeconds * 1_000L)
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
        val g = game()
        val now = 1_700_000_000_000L
        val stopped = g.startRoundClock(now).resolve(CoupleOutcome.COMPLETED,
            now + g.turns[0].prompt.durationSeconds * 1_000L).leave()
        assertTrue(stopped.finished)
        assertFalse(stopped.competitionEnded)
        assertNull(stopped.winnerIndex)
        assertFalse(stopped.tiePending)
    }

    @Test fun ownerApprovedV10Has100PreservedRecordsAndActionCodes() {
        val levels = listOf(Intensity.SOFT, Intensity.NORMAL, Intensity.SEXY,
            Intensity.HOT, Intensity.HARD)
        val items = levels.flatMap { CoupleStyleV10Catalog.deck(it) }
        assertEquals(100, items.size)
        assertEquals(100, items.map { it.id }.toSet().size)
        assertEquals(100, items.map { it.kindCode }.toSet().size)
        assertEquals(100, items.map { it.text }.toSet().size)
        assertTrue(items.all { it.durationSeconds in 30..300 && it.title.isNotBlank() })
        assertEquals("c-SOFT-1", CoupleStyleV10Catalog.deck(Intensity.SOFT)[0].id)
        assertEquals(90, CoupleStyleV10Catalog.deck(Intensity.SOFT)[0].durationSeconds)
    }

    @Test fun roundCannotAwardPositivePointsBeforeCountdown() {
        val g = game()
        val now = 1_700_000_000_000L
        val running = g.startRoundClock(now)
        val duration = g.turns[0].prompt.durationSeconds * 1_000L
        try {
            running.resolve(CoupleOutcome.COMPLETED, now + duration - 1L)
            fail("Early completion should not award points")
        } catch (_: IllegalArgumentException) { }
        val resolved = running.resolve(CoupleOutcome.COMPLETED, now + duration)
        assertEquals(10, resolved.scores.sum())
        assertFalse(resolved.competition!!.roundClock.started)
    }

    @Test fun pausedTimeIsNotCountedAfterResuming() {
        val g = game()
        val now = 1_700_000_000_000L
        val running = g.startRoundClock(now)
        val paused = running.pauseRoundClock(now + 30_000L)
        val left = paused.competition!!.roundClock.remainingSeconds(now + 50_000L)
        assertEquals(g.turns[0].prompt.durationSeconds - 30, left)
        val resumed = paused.startRoundClock(now + 80_000L)
        assertEquals(left - 10,
            resumed.competition!!.roundClock.remainingSeconds(now + 90_000L))
    }

    @Test fun balancedSelectionAvoidsDuplicatePromptsAndIncludesKinds() {
        val g = SessionFactory.create(
            listOf("الزوج", "الزوجة"), Intensity.HARD, false, Random(11),
            roundCount = 18, variety = ChallengeVariety.MIX_WITH_LOWER,
            competition = competition()
        )
        assertEquals(18, g.turns.size)
        assertEquals(18, g.turns.map { it.prompt.text }.toSet().size)
        assertEquals(18, g.turns.map { it.prompt.actionCode }.toSet().size)
        assertTrue(g.turns.map { it.prompt.level }.toSet().size >= 2)
        assertTrue(g.turns.zipWithNext().all { (a, b) -> a.actorIndex != b.actorIndex })
    }

    @Test fun customPackCanFillEntireSessionWithoutLegacyPrompts() {
        val custom = (1..18).map { "تحدي شخصي رقم $it مكتوب من الزوجين" }
        val g = SessionFactory.create(
            listOf("الزوج", "الزوجة"), Intensity.CUSTOM, false,
            Random(21), customPrompts = custom, roundCount = 18,
            competition = competition()
        )
        assertEquals(18, g.turns.size)
        assertEquals(custom.toSet(), g.turns.map { it.prompt.text }.toSet())
        assertTrue(g.turns.all { it.prompt.kind == CoupleChallengeKind.PERSONAL })
    }

    @Test fun v10RuntimeEligibilityKeepsFlaggedIdsOutOfAutomaticDecks() {
        val levels = listOf(Intensity.SOFT, Intensity.NORMAL, Intensity.SEXY,
            Intensity.HOT, Intensity.HARD)
        val playable = levels.flatMap { CoupleV10Policy.deck(it) }
        assertEquals(81, playable.size)
        assertEquals(19, CoupleV10Policy.excludedIds.size)
        assertTrue(playable.none { it.id in CoupleV10Policy.excludedIds })
        assertTrue(playable.all { it.actionCode.isNotBlank() })
        assertEquals(16, CoupleV10Policy.eligibleCount(Intensity.SOFT))
        assertEquals(20, CoupleV10Policy.eligibleCount(Intensity.NORMAL))
        assertEquals(20, CoupleV10Policy.eligibleCount(Intensity.SEXY))
        assertEquals(17, CoupleV10Policy.eligibleCount(Intensity.HOT))
        assertEquals(8, CoupleV10Policy.eligibleCount(Intensity.HARD))
    }

    @Test fun newSingleLevelSessionUsesV10PreservingExactActionCode() {
        val session = game()
        val originals = CoupleStyleV10Catalog.deck(Intensity.SOFT).associateBy { it.id }
        assertTrue(session.turns.all { it.prompt.id.startsWith("c-SOFT-") })
        assertTrue(session.turns.all { it.prompt.actionCode ==
            originals.getValue(it.prompt.id).kindCode })
    }

    @Test fun hardSingleLevelCannotOverdrawSafetyEligiblePool() {
        try {
            SessionFactory.create(listOf("الزوج", "الزوجة"), Intensity.HARD,
                false, Random(3), roundCount = 18, competition = competition())
            fail("Must not repeat or silently escalate HARD beyond 8 eligible items")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun newSessionsStayInSelectedLevelUnlessMixExplicitlyRequested() {
        val single = SessionFactory.create(listOf("الزوج", "الزوجة"),
            Intensity.SOFT, false, Random(1), roundCount = 12,
            competition = competition())
        assertTrue(single.turns.all { it.prompt.level == Intensity.SOFT })
        val mixed = SessionFactory.create(listOf("الزوج", "الزوجة"),
            Intensity.HARD, false, Random(1), roundCount = 18,
            variety = ChallengeVariety.MIX_WITH_LOWER, competition = competition())
        assertTrue(mixed.turns.any { it.prompt.level == Intensity.HARD })
        assertTrue(mixed.turns.any { it.prompt.level == Intensity.HOT })
        assertFalse(mixed.turns.any { it.prompt.level == Intensity.SEXY })
    }

    @Test fun newCompetitionIsNotAllowedForPartyMode() {
        try {
            SessionFactory.create(listOf("أ", "ب"), Intensity.SOFT, true,
                competition = competition())
            fail("Competition is couples only")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun stoppedClockDoesNotLeakIntoFinishedSession() {
        val now = 1_700_000_000_000L
        val game = game().startRoundClock(now)
        val stopped = game.leave()
        assertNull(stopped.competition!!.roundClock.runningSinceMillis)
        assertNull(stopped.winnerIndex)
    }

    @Test fun cannotCommitKnowledgeQuestionsTwice() {
        val knowledge = tiedGame().resolveSharedTieBreak(listOf(true, true))
        val questions = listOf(
            KnowledgePrompt("إيه أكتر أكلة بحبها؟", "ملوخية"),
            KnowledgePrompt("أول خروجة كانت فين؟", "القاهرة")
        )
        val prepared = knowledge.registerKnowledgeQuestions(questions)
        try {
            prepared.registerKnowledgeQuestions(questions)
            fail("Cannot overwrite registered answers before grading")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun partnersCanEndTieBreakWithoutWinnerOrReward() {
        val tied = tiedGame().resolveSharedTieBreak(listOf(true, true))
        val stopped = tied.leave()
        assertTrue(stopped.ended)
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
