package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test

class TripleDiceStageGeometryTest {
    @Test fun threeCubesShareOneDeterministicStage() {
        val a = TripleDiceStageGeometry.pose(0)
        val b = TripleDiceStageGeometry.pose(1)
        val c = TripleDiceStageGeometry.pose(2)
        assertEquals(-a.x, b.x, .0001f)
        assertEquals(a.y, b.y, .0001f)
        assertEquals(a.z, b.z, .0001f)
        assertEquals(0f, c.x, .0001f)
        assertTrue(c.y < a.y)
        assertTrue(c.z > a.z)
        assertTrue(c.scale > a.scale)
    }

    @Test fun designedCubesHaveNonOverlappingRestFootprints() {
        val a = TripleDiceStageGeometry.pose(0)
        val b = TripleDiceStageGeometry.pose(1)
        val c = TripleDiceStageGeometry.pose(2)
        assertTrue(b.x - a.x > (a.scale + b.scale) / 2f)
        assertTrue(a.y - c.y > (a.scale + c.scale) / 2f)
    }

    @Test fun eighteenTextureIndicesAreDistinctAndInOrder() {
        val indices = (0..2).flatMap { cube ->
            (0..5).map { face -> TripleDiceStageGeometry.textureIndex(cube, face) }
        }
        assertEquals((0..17).toList(), indices)
        assertEquals(18, indices.toSet().size)
    }

    @Test fun textureIndexRejectsInvalidCubeOrFace() {
        for (invalid in listOf(-1, 3)) {
            try {
                TripleDiceStageGeometry.textureIndex(invalid, 0)
                fail("Invalid cube index accepted")
            } catch (_: IllegalArgumentException) { }
        }
        for (invalid in listOf(-1, 6)) {
            try {
                TripleDiceStageGeometry.textureIndex(1, invalid)
                fail("Invalid face accepted")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
