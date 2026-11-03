package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

/** Source-declared unit tests only: no Gradle execution or APK build. */
class PersonalDiceConfigurationTest {
    private fun authored(): PersonalDiceConfiguration {
        val blank = PersonalDiceConfiguration.blank()
        return blank.copy(cubes = blank.cubes.mapIndexed { cube, faces ->
            faces.mapIndexed { index, face ->
                face.copy(text = "user-$cube-$index")
            }
        })
    }

    @Test fun newCubesHaveNoAuthoredTextOrPictures() {
        val config = PersonalDiceConfiguration.blank()
        assertEquals(2, config.cubes.size)
        assertTrue(config.cubes.all { it.size == 6 })
        assertTrue(config.cubes.flatten().all { it.text.isEmpty() && it.imageId == null })
        assertFalse(config.playable)
    }

    @Test fun authorMustFillEveryPositiveWeightFace() {
        val blank = PersonalDiceConfiguration.blank()
        assertFalse(blank.playable)
        assertTrue(authored().playable)
        assertEquals(100, authored().cubes[0].sumOf { it.weight })
        assertEquals(100, authored().cubes[1].sumOf { it.weight })
    }

    @Test fun editingOneCubeNeverChangesOtherCubeOrFace() {
        val original = authored()
        val changed = original.changeFace(0, 3, original.cubes[0][3].copy(text = "edited"))
        assertEquals(original.cubes[1], changed.cubes[1])
        assertEquals(original.cubes[0][2], changed.cubes[0][2])
        assertEquals("edited", changed.cubes[0][3].text)
    }

    @Test fun intermediateDraftWeightsCanBeSavedButCannotRoll() {
        val original = authored()
        val modified = original.changeFace(0, 1, original.cubes[0][1].copy(weight = 30))
        modified.validateStructure()
        assertFalse(modified.playable)
        try {
            modified.roll { 0 }
            fail("Roll must require exactly 100 percent")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun deterministicRollMapsBothSelectedFacesIndependently() {
        val config = authored()
        assertEquals(listOf(0, 0), config.roll { 0 })
        assertEquals(listOf(5, 5), config.roll { 99 })
        assertTrue(config.roll { 45 }.all { it in 0..5 })
    }

    @Test fun zeroWeightUnconfiguredFaceDoesNotBlockRoll() {
        val complete = authored()
        val groups = complete.cubes.map { cube ->
            cube.mapIndexed { index, face ->
                if (index == 0) face.copy(text = "", imageId = null, weight = 0)
                else if (index == 1) face.copy(weight = 34)
                else face
            }
        }
        val config = complete.copy(cubes = groups)
        assertTrue(config.playable)
        assertTrue(config.roll { 0 }.all { it != 0 })
    }

    @Test fun imageOnlyFacesCanBeConfiguredWithoutText() {
        val withImages = authored().copy(cubes = authored().cubes.map { cube ->
            cube.map { face -> face.copy(text = "", imageId = "00000000-0000-0000-0000-000000000001") }
        })
        assertTrue(withImages.playable)
    }

    @Test fun invalidFaceLengthsAndIdsFail() {
        try {
            PersonalDiceFace(text = "x".repeat(73), weight = 10)
            fail("Text must be bounded")
        } catch (_: IllegalArgumentException) { }
        try {
            PersonalDiceFace(imageId = "../../sensitive")
            fail("Photo identifiers must be validated")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun nonTwoCubeStructuresAreRejectedWithoutAffectingLegacyMode() {
        try {
            PersonalDiceConfiguration(listOf(List(6) { PersonalDiceFace() }))
            fail("Only two new cubes allowed")
        } catch (_: IllegalArgumentException) { }
        assertEquals(3, DiceCatalog.preset(Intensity.SOFT).dice.size)
    }
}
