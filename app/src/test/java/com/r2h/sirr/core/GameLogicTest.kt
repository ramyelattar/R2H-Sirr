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
    @Test fun dataIsConsistent() {
        assertEquals(7, OriginalContent.quizPacks.size)
        assertTrue(OriginalContent.quizPacks.all { it.questions.isNotEmpty() })
        assertTrue(OriginalContent.quizPacks.flatMap { it.questions }.all { it.choices.size == 3 })
        assertEquals(100, GameLogic.evenWeights.sum())
        assertEquals(3, OriginalContent.diceTemplates.size)
    }
}
