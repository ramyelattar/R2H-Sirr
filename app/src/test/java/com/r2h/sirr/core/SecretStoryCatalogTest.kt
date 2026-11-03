package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

/** Source test declarations only; no Gradle or APK tests have been executed. */
class SecretStoryCatalogTest {
    @Test fun hasTwentyStoriesForEachModeAndThreeLinesPerStory() {
        assertEquals(20, SecretStoryCatalog.whispers.size)
        assertEquals(20, SecretStoryCatalog.scratch.size)
        assertTrue((SecretStoryCatalog.whispers + SecretStoryCatalog.scratch)
            .all { it.lines.size == 3 })
    }

    @Test fun storiesHaveUniqueIdsAndNoRepeatedLinesWithinEachMode() {
        val all = SecretStoryCatalog.whispers + SecretStoryCatalog.scratch
        assertEquals(40, all.map { it.id }.toSet().size)
        assertEquals(120, all.flatMap { it.lines }.toSet().size)
    }

    @Test fun threeTiersArePresentWithExpectedDistribution() {
        for (stories in listOf(SecretStoryCatalog.whispers, SecretStoryCatalog.scratch)) {
            assertEquals(6, stories.count { it.tier == SecretTier.SOFT })
            assertEquals(7, stories.count { it.tier == SecretTier.FLIRT })
            assertEquals(7, stories.count { it.tier == SecretTier.ADULT })
        }
    }

    @Test fun allStoriesHaveReadableBoundedEgyptianCopy() {
        assertTrue((SecretStoryCatalog.whispers + SecretStoryCatalog.scratch)
            .flatMap { it.lines }
            .all { it.trim().length in 12..350 })
    }

    @Test fun scratchSingleLayerRevealsTheConclusionNotJustTheTeaser() {
        SecretStoryCatalog.scratch.forEach {
            assertEquals(listOf(it.lines[2]), SecretStoryCatalog.scratchLayers(it, 1))
        }
    }

    @Test fun scratchTwoLayersRevealSetupAndPayoff() {
        SecretStoryCatalog.scratch.forEach {
            assertEquals(it.lines.takeLast(2), SecretStoryCatalog.scratchLayers(it, 2))
        }
    }

    @Test fun scratchThreeLayersKeepAllOriginalStoryPartsInOrder() {
        SecretStoryCatalog.scratch.forEach {
            assertEquals(it.lines, SecretStoryCatalog.scratchLayers(it, 3))
        }
    }

    @Test fun cannotTreatWhisperAsScratchStoryOrPickInvalidStageCount() {
        try {
            SecretStoryCatalog.scratchLayers(SecretStoryCatalog.whispers.first(), 1)
            fail("Whisper stories do not have Scratch layer semantics")
        } catch (_: IllegalArgumentException) {}
        try {
            SecretStoryCatalog.scratchLayers(SecretStoryCatalog.scratch.first(), 0)
            fail("Scratch supports 1–3 layers only")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun tierPickerNeverCrossesIntoOtherContentLevel() {
        for (tier in SecretTier.entries) {
            repeat(100) { index ->
                assertEquals(tier, SecretStoryCatalog.pick(true, tier, index).tier)
                assertEquals(tier, SecretStoryCatalog.pick(false, tier, index).tier)
            }
        }
    }

    @Test fun pickerWrapsLargeAndNegativeIndicesWithinTheSameTier() {
        assertEquals(SecretTier.ADULT, SecretStoryCatalog.pick(false, SecretTier.ADULT, -1).tier)
        assertEquals(SecretTier.FLIRT, SecretStoryCatalog.pick(true, SecretTier.FLIRT, Int.MAX_VALUE).tier)
    }

    @Test fun emptyOrOversizedSecretLinesAreRejected() {
        try {
            SecretStory("s99", SecretTier.SOFT, listOf("secret", "", "last"))
            fail("Blank line must be rejected")
        } catch (_: IllegalArgumentException) {}
        try {
            SecretStory("s99", SecretTier.SOFT, listOf("x".repeat(351), "two", "three"))
            fail("Oversized line must be rejected")
        } catch (_: IllegalArgumentException) {}
    }
}
