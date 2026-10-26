package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

class DiceCatalogGrammarTest {
    @Test fun all1080PresetCombinationsAreStructurallyCompatible() {
        Intensity.entries.filter { it != Intensity.CUSTOM }.forEach { level ->
            val p = DiceCatalog.preset(level)
            p.validate()
            assertEquals(3, p.dice.size)
            assertTrue(p.dice[1].all { it.text.startsWith("عن ") })
            assertTrue(p.dice[2].all { f ->
                listOf("ب", "و", "في ", "من غير ", "مع ", "بعد ")
                    .any { f.text.startsWith(it) }
            })
            var total = 0
            p.dice[0].forEach { a -> p.dice[1].forEach { topic -> p.dice[2].forEach { how ->
                val result = a.text + " " + topic.text + " " + how.text
                assertTrue(result.length >= 15)
                assertFalse(result.contains("  "))
                total++
            } } }
            assertEquals(216, total)
        }
    }
    @Test fun customAndWeightsStillWork() {
        val p = DiceCatalog.preset(Intensity.CUSTOM)
        p.validate()
        assertEquals(listOf(17,17,17,17,16,16),p.dice[0].map { it.weight })
    }
}
