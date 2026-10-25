package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

class GameLogicTest {
    @Test fun edgeCasesOfWeightedSelection() {
        val weights = listOf(10, 20, 0, 30, 20, 20)
        assertEquals(0, GameLogic.weightedFace(weights) { 0 })
        assertEquals(0, GameLogic.weightedFace(weights) { 9 })
        assertEquals(1, GameLogic.weightedFace(weights) { 10 })
        assertEquals(3, GameLogic.weightedFace(weights) { 30 })
        assertEquals(5, GameLogic.weightedFace(weights) { 99 })
    }
    @Test fun invalidDistributionRejected() {
        try {
            GameLogic.weightedFace(listOf(20, 20, 20, 20, 20, 20)) { 10 }
            fail("Should reject probabilities not equal to 100")
        } catch (_: IllegalArgumentException) { }
        try {
            GameLogic.weightedFace(listOf(100, 0, 0, 0, 0, -1)) { 0 }
            fail("Should reject negative weights")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun deterministicScore() {
        assertEquals(3, GameLogic.score(listOf(0, 1, 2)))
        assertEquals(6, GameLogic.score(listOf(2, 2, 2)))
        assertTrue(GameLogic.interpretation(2, 6).isNotBlank())
    }
    @Test fun activeCanonicalContentIsConsistent() {
        // The UI and remote flows use these v4 packs, not OriginalContent.
        val packs = ExpandedQuizzes.packs
        assertEquals(7, packs.size)
        assertTrue(packs.all { it.questions.size == 25 })
        val questions = packs.flatMap { it.questions }
        assertEquals(175, questions.size)
        assertEquals(175, questions.map { it.text }.toSet().size)
        assertTrue(questions.all { q ->
            q.text.isNotBlank() && q.choices.size == 3 &&
                q.choices.all { it.isNotBlank() }
        })

        val levels = listOf(Intensity.SOFT, Intensity.NORMAL, Intensity.SEXY,
            Intensity.HOT, Intensity.HARD)
        val allChallenges = listOf(false, true).flatMap { group ->
            levels.flatMap { level ->
                val deck = Challenges.deck(level, group)
                assertEquals(if (group) 20 else 26, deck.size)
                deck
            }
        }
        assertEquals(230, allChallenges.size)
        assertEquals(230, allChallenges.map { it.text }.toSet().size)

        assertEquals(listOf(17, 17, 17, 17, 16, 16), GameLogic.evenWeights)
        Intensity.entries.forEach { level ->
            val configuration = DiceCatalog.preset(level)
            configuration.validate()
            assertEquals(3, configuration.dice.size)
            assertTrue(configuration.dice.all { die ->
                die.size == 6 &&
                    die.map { it.weight } == GameLogic.evenWeights &&
                    die.all { it.text.isNotBlank() && it.text.length <= 72 }
            })
        }
    }
}
