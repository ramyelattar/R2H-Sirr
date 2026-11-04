package com.r2h.sirr.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class RoundedDiceMeshTest {
    private val stride = RoundedDiceMesh.STRIDE_FLOATS
    private val grid = RoundedDiceMesh.DEFAULT_GRID
    private val perFace = RoundedDiceMesh.verticesPerFace()

    @Test fun sixFacesHaveTheExactExpectedVertexCount() {
        val mesh = RoundedDiceMesh.generate()
        assertEquals(6 * grid * grid * 6 * stride, mesh.size)
        assertEquals(6, RoundedDiceMesh.FACE_COUNT)
    }

    @Test fun allNormalsAreFiniteAndUnitLength() {
        val mesh = RoundedDiceMesh.generate()
        for (offset in mesh.indices step stride) {
            val xyz = mesh.sliceArray(offset until offset + 3)
            val nx = mesh[offset + 3]
            val ny = mesh[offset + 4]
            val nz = mesh[offset + 5]
            assertTrue(xyz.all { it.isFinite() && abs(it) <= .501f })
            val length = sqrt(nx * nx + ny * ny + nz * nz)
            assertEquals(1f, length, 0.0005f)
            assertTrue(mesh[offset + 6] in 0f..1f)
            assertTrue(mesh[offset + 7] in 0f..1f)
        }
    }

    @Test fun centerNormalsPreserveCanonicalFaceOrdering() {
        val expected = arrayOf(
            floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 0f, -1f), floatArrayOf(-1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, -1f, 0f)
        )
        val mesh = RoundedDiceMesh.generate()
        val cell = (grid / 2) * grid + grid / 2
        for (face in 0 until 6) {
            val offset = (face * perFace + cell * 6) * stride
            val actual = floatArrayOf(mesh[offset+3], mesh[offset+4], mesh[offset+5])
            assertArrayEquals("Face $face", expected[face], actual, .001f)
        }
    }

    @Test fun meshContainsRealBeveledCornersNotFlatSquares() {
        val mesh = RoundedDiceMesh.generate()
        for (face in 0 until 6) {
            val offset = face * perFace * stride
            // First sampled point of every face is a rounded 3-axis corner.
            assertTrue(abs(mesh[offset]) < .5f)
            assertTrue(abs(mesh[offset+1]) < .5f)
            assertTrue(abs(mesh[offset+2]) < .5f)
            assertTrue(abs(mesh[offset+3]) > .1f)
            assertTrue(abs(mesh[offset+4]) > .1f)
            assertTrue(abs(mesh[offset+5]) > .1f)
        }
    }

    @Test fun windingIsOutwardOnEveryFace() {
        val mesh = RoundedDiceMesh.generate()
        val cell = (grid / 2) * grid + grid / 2
        for (face in 0 until 6) {
            val start = (face * perFace + cell * 6) * stride
            fun xyz(vertex: Int) = (0..2).map { mesh[start + vertex * stride + it] }
            val a = xyz(0); val b = xyz(1); val c = xyz(2)
            val ab = (0..2).map { b[it] - a[it] }
            val ac = (0..2).map { c[it] - a[it] }
            val cross = floatArrayOf(
                ab[1]*ac[2] - ab[2]*ac[1],
                ab[2]*ac[0] - ab[0]*ac[2],
                ab[0]*ac[1] - ab[1]*ac[0]
            )
            val dot = (0..2).sumOf { cross[it].toDouble() * mesh[start+3+it] }
            assertTrue("Face $face winding must face outward", dot > 0.0)
        }
    }

    @Test fun meshIsDeterministicAndRejectsUnsafeParameters() {
        assertArrayEquals(RoundedDiceMesh.generate(8), RoundedDiceMesh.generate(8), 0f)
        for (radius in listOf(-.1f, 0f, .3f)) {
            try {
                RoundedDiceMesh.generate(radius = radius)
                fail("Invalid radius: $radius")
            } catch (_: IllegalArgumentException) { }
        }
        try {
            RoundedDiceMesh.generate(grid = 0)
            fail("Invalid grid accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
