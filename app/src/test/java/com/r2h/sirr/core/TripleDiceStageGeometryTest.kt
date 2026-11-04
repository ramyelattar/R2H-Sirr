package com.r2h.sirr.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class TripleDiceStageGeometryTest {
    private fun identity() = floatArrayOf(
        1f,0f,0f,0f,
        0f,1f,0f,0f,
        0f,0f,1f,0f,
        0f,0f,0f,1f
    )

    @Test fun threeCubesStandOnOneSharedWorldPlane() {
        val rearA = TripleDiceStageGeometry.pose(0)
        val rearB = TripleDiceStageGeometry.pose(1)
        val forward = TripleDiceStageGeometry.pose(2)
        assertEquals(-rearA.x, rearB.x, 0.0001f)
        assertEquals(rearA.z, rearB.z, 0.0001f)
        assertTrue(forward.z > rearA.z)
        assertEquals(0f, forward.x, 0.0001f)
        assertTrue(forward.scale > rearA.scale)

        for (cube in 0..2) {
            val pose = TripleDiceStageGeometry.pose(cube)
            assertEquals(
                TripleDiceStageGeometry.FLOOR_Y,
                TripleDiceStageGeometry.restingCenterY(identity(), pose.scale) -
                    TripleDiceStageGeometry.verticalHalfExtent(identity(), pose.scale),
                0.0001f
            )
        }
    }

    @Test fun worldSpaceCubeFootprintsDoNotIntersect() {
        for (a in 0..2) for (b in (a + 1)..2) {
            assertTrue(
                "Footprints $a and $b must stay apart",
                TripleDiceStageGeometry.footprintGap(a, b) > 0f
            )
        }
    }

    @Test fun rotationAwareSupportPreventsGroundPenetration() {
        val c = (sqrt(2.0) / 2.0).toFloat()
        val tilted = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, c, -c, 0f,
            0f, c, c, 0f,
            0f, 0f, 0f, 1f
        )
        val size = TripleDiceStageGeometry.pose(2).scale
        val axisAligned = TripleDiceStageGeometry.verticalHalfExtent(identity(), size)
        val rotated = TripleDiceStageGeometry.verticalHalfExtent(tilted, size)
        assertEquals(0.5f * size, axisAligned, 0.0001f)
        assertTrue(rotated > axisAligned)
        assertEquals(
            TripleDiceStageGeometry.FLOOR_Y,
            TripleDiceStageGeometry.restingCenterY(tilted, size) - rotated,
            0.0001f
        )
    }

    @Test fun rejectInvalidRotationOrCubeIndices() {
        try {
            TripleDiceStageGeometry.restingCenterY(floatArrayOf(1f), 1f)
            fail("Invalid rotation accepted")
        } catch (_: IllegalArgumentException) {}
        try {
            TripleDiceStageGeometry.verticalHalfExtent(identity(), 0f)
            fail("Zero cube scale accepted")
        } catch (_: IllegalArgumentException) {}
        try {
            TripleDiceStageGeometry.pose(3)
            fail("Invalid cube accepted")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun eighteenTextureIndicesAreUnchangedAndDistinct() {
        val indices = (0..2).flatMap { cube ->
            (0..5).map { face ->
                TripleDiceStageGeometry.textureIndex(cube, face)
            }
        }
        assertEquals((0..17).toList(), indices)
        assertEquals(18, indices.toSet().size)
    }

    @Test fun textureIndexRejectsInvalidFaceAndCube() {
        for (invalid in listOf(-1, 3)) {
            try {
                TripleDiceStageGeometry.textureIndex(invalid, 0)
                fail("Invalid cube accepted")
            } catch (_: IllegalArgumentException) {}
        }
        for (invalid in listOf(-1, 6)) {
            try {
                TripleDiceStageGeometry.textureIndex(1, invalid)
                fail("Invalid face accepted")
            } catch (_: IllegalArgumentException) {}
        }
    }
}
