package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ExperienceLogicTest {
    @Test fun dicePresetWeightsAndDurations() {
        Intensity.entries.forEach { level ->
            val c = DiceCatalog.preset(level)
            c.validate()
            assertEquals(3, c.roll { 0 }.size)
        }
        assertEquals(100, GameLogic.evenWeights.sum())
    }
    @Test fun diceInvalidWeightsRejected() {
        val config = DiceCatalog.preset(Intensity.SOFT)
        val broken = config.copy(dice = config.dice.mapIndexed { i, die ->
            if (i==0) die.mapIndexed { j, f -> if(j==0)f.copy(weight=100) else f } else die
        })
        try { broken.validate(); fail("invalid configuration passed") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun scoresAreFairForFourPlayers() {
        var game=SessionFactory.create(listOf("ا","ب","ج","د"),Intensity.HOT,true,Random(3))
        assertEquals(12,game.turns.size)
        repeat(12){ game=game.resolve(true) }
        assertTrue(game.finished)
        assertEquals(listOf(3,3,3,3),game.scores)
        assertEquals(12,game.scores.sum())
    }
    @Test fun coupleCanStopAndCannotReplay() {
        val g=SessionFactory.create(listOf("ا","ب"),Intensity.SEXY,false,Random(2))
        assertEquals(10,g.turns.size)
        assertTrue(g.leave().finished)
    }
    @Test fun quizBankHas175DistinctQuestions() {
        val packs = ExpandedQuizzes.packs
        assertEquals(7, packs.size)
        assertTrue(packs.all { it.questions.size == 25 })
        assertEquals(175, packs.sumOf { it.questions.size })
        val questions = packs.flatMap { it.questions }
        assertEquals(175, questions.map { it.text }.toSet().size)
        assertTrue(questions.all { it.choices.size == 3 &&
            it.choices.all { choice -> choice.isNotBlank() } })
    }
    @Test fun customChallengePromptsAreValidAndAlwaysIncluded() {
        val extras = listOf("اختر موضوعًا خاصًا لكن لطيفًا", "اقترح موعدًا مختلفًا عن المعتاد")
        val g = SessionFactory.create(listOf("ا", "ب"), Intensity.CUSTOM, false, Random(1), extras)
        assertTrue(extras.all { extra -> g.turns.any { it.prompt.text == extra } })
        assertEquals(10, g.turns.map { it.prompt.text }.distinct().size)
        try {
            SessionFactory.create(listOf("ا", "ب"), Intensity.NORMAL, false, Random(2), extras)
            fail("custom prompts should be rejected in predefined mode")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun ephemeralsExpireAndOneViewOnly() {
        val e=EphemeralEnvelope(1000,2000)
        assertTrue(e.available(1001))
        assertFalse(e.available(2000))
        assertFalse(e.consume(1001).available(1500))
    }
}
