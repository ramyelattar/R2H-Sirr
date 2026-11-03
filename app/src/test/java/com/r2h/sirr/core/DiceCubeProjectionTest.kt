package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** Mathematically tests which physical face faces the camera after each roll. */
class DiceCubeProjectionTest {
    @Test fun allSixFacesCanFinishFacingTheViewer() {
        (0..5).forEach { selected ->
            val visible = DiceCubeProjection.faces(selected, 0f, 0f, 240f, 240f)
            val face = visible.single { it.index == selected }
            assertTrue("Face $selected must be in front", face.facing > 0.75f)
            assertEquals(4, face.corners.size)
        }
    }

    @Test fun fullSpinsAlwaysFinishOnTheSameChosenFace() {
        for (selected in 0..5) {
            val final = DiceCubeProjection.faces(selected, 0f, 0f, 220f, 220f)
            val revolved = DiceCubeProjection.faces(selected, 1080f, 720f, 220f, 220f)
            val center = final.single { it.index == selected }
            val after = revolved.single { it.index == selected }
            assertTrue(abs(center.facing - after.facing) < .001f)
            center.corners.zip(after.corners).forEach { (a, b) ->
                assertTrue(abs(a.x - b.x) < .01f)
                assertTrue(abs(a.y - b.y) < .01f)
            }
        }
    }

    @Test fun everyVisibleFaceUsesFiniteOrderedProjectedVertices() {
        for (selected in 0..5) {
            for (spin in listOf(0f, 45f, 183f, 360f)) {
                val quad = DiceCubeProjection.faces(selected, spin, spin / 2f, 220f, 220f)
                assertTrue(quad.size in 1..3)
                assertTrue(quad.all { it.corners.size == 4 && it.corners.all { p ->
                    p.x.isFinite() && p.y.isFinite()
                } })
                assertEquals(quad.sortedBy { it.averageDepth }, quad)
            }
        }
    }

    @Test fun projectionRejectsOutOfRangeFaceAndNonFiniteAngles() {
        try {
            DiceCubeProjection.faces(6, 0f, 0f, 100f, 100f)
            fail("Out-of-range face")
        } catch (_: IllegalArgumentException) { }
        try {
            DiceCubeProjection.faces(0, Float.NaN, 0f, 100f, 100f)
            fail("Non-finite spin")
        } catch (_: IllegalArgumentException) { }
        try {
            DiceCubeProjection.faces(0, 0f, 0f, 0f, 100f)
            fail("Invalid canvas size")
        } catch (_: IllegalArgumentException) { }
    }
}
