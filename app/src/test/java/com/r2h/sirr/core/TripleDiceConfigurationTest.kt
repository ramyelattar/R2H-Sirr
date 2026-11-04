package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

class TripleDiceConfigurationTest {
    private fun authored(): TripleDiceConfiguration {
        val blank = TripleDiceConfiguration.blank()
        return blank.copy(cubes = blank.cubes.mapIndexed { cube, faces ->
            faces.mapIndexed { index, face ->
                face.copy(text = "user-$cube-$index")
            }
        })
    }

    @Test fun newContractHasExactlyEighteenOwnerBlankFaces() {
        val triple = TripleDiceConfiguration.blank()
        assertEquals(3, triple.cubes.size)
        assertEquals(18, triple.cubes.flatten().size)
        assertTrue(triple.cubes.flatten().all {
            it.text.isEmpty() && it.imageId == null
        })
        assertTrue(triple.cubes.all { it.sumOf { face -> face.weight } == 100 })
        assertFalse(triple.playable)
    }

    @Test fun positiveWeightFaceRequiresOwnerContentOnAllThreeCubes() {
        assertTrue(authored().playable)
        val config = authored().changeFace(
            2, 4, PersonalDiceFace(weight = 16)
        )
        assertFalse(config.playable)
    }

    @Test fun rollSelectsThreeIndependentCanonicalIndices() {
        val triple = authored()
        assertEquals(listOf(0, 0, 0), triple.roll { 0 })
        assertEquals(listOf(5, 5, 5), triple.roll { 99 })
        val picks = listOf(1, 42, 87).iterator()
        assertEquals(listOf(0, 2, 5), triple.roll { picks.next() })
    }

    @Test fun changeFaceDoesNotMutateOtherFacesOrCubes() {
        val original = authored()
        val changed = original.changeFace(
            2, 5, original.cubes[2][5].copy(weight = 15, text = "custom")
        )
        assertEquals(original.cubes[0], changed.cubes[0])
        assertEquals(original.cubes[1], changed.cubes[1])
        assertEquals(original.cubes[2][4], changed.cubes[2][4])
        assertEquals("custom", changed.cubes[2][5].text)
        assertFalse(changed.playable)
    }

    @Test fun oldPairConvertsWithoutTouchingEitherLegacyCube() {
        val old = PersonalDiceConfiguration.blank().let { original ->
            original.changeFace(
                0, 0, PersonalDiceFace(
                    text = "owner one", weight = 17,
                    imageId = "00000000-0000-0000-0000-000000000001"
                )
            ).copy(durationSeconds = 7)
        }
        val converted = TripleDiceConfiguration.fromPair(old)
        assertEquals(3, converted.cubes.size)
        assertEquals(old.cubes[0], converted.cubes[0])
        assertEquals(old.cubes[1], converted.cubes[1])
        assertEquals(7, converted.durationSeconds)
        assertEquals(
            TripleDiceConfiguration.blank().cubes[2], converted.cubes[2]
        )
    }

    @Test fun incompleteDraftCanExistButCannotRoll() {
        val draft = authored().changeFace(
            1, 0, authored().cubes[1][0].copy(weight = 30)
        )
        draft.validateStructure()
        assertFalse(draft.playable)
        try {
            draft.roll { 0 }
            fail("Partial distribution must not roll")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun invalidCubeCountCannotPass() {
        try {
            TripleDiceConfiguration(List(2) {
                List(6) { PersonalDiceFace() }
            })
            fail("Two cubes were accepted as triple")
        } catch (_: IllegalArgumentException) { }
        assertEquals(2, PersonalDiceConfiguration.blank().cubes.size)
    }
}
