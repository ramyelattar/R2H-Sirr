package com.r2h.sirr.core

import kotlin.math.sqrt

/**
 * Pure, deterministic rounded-cube geometry. No Android, content, or random state.
 * Faces keep DiceCubeProjection's canonical indices:
 * FRONT, RIGHT, BACK, LEFT, TOP, BOTTOM.
 *
 * Each face uses the same top-left -> top-right -> bottom-right -> bottom-left
 * UV direction as the owner's 384px face tiles.
 *
 * GLES2 interleaved array: xyz + outward normal xyz + uv, 8 floats/vertex.
 * The bevel is geometry, not a rounded square texture or painted outline.
 */
internal object RoundedDiceMesh {
    const val FACE_COUNT = 6
    const val STRIDE_FLOATS = 8
    const val DEFAULT_GRID = 24
    const val DEFAULT_RADIUS = 0.135f

    private data class V(val x: Float, val y: Float, val z: Float) {
        operator fun plus(b: V) = V(x + b.x, y + b.y, z + b.z)
        operator fun minus(b: V) = V(x - b.x, y - b.y, z - b.z)
        operator fun times(f: Float) = V(x * f, y * f, z * f)
        fun normalized(): V {
            val length = sqrt(x * x + y * y + z * z)
            require(length > 0f)
            return this * (1f / length)
        }
    }

    // Normal, right (texture +U), down (texture +V).
    // The right/down vector ordering is intentionally outward CCW for vertices.
    private val axes = listOf(
        Triple(V(0f, 0f, 1f), V(1f, 0f, 0f), V(0f, -1f, 0f)),
        Triple(V(1f, 0f, 0f), V(0f, 0f, -1f), V(0f, -1f, 0f)),
        Triple(V(0f, 0f, -1f), V(-1f, 0f, 0f), V(0f, -1f, 0f)),
        Triple(V(-1f, 0f, 0f), V(0f, 0f, 1f), V(0f, -1f, 0f)),
        Triple(V(0f, 1f, 0f), V(1f, 0f, 0f), V(0f, 0f, 1f)),
        Triple(V(0f, -1f, 0f), V(1f, 0f, 0f), V(0f, 0f, -1f))
    )

    fun verticesPerFace(grid: Int = DEFAULT_GRID): Int {
        require(grid in 2..48)
        return grid * grid * 6
    }

    fun generate(grid: Int = DEFAULT_GRID, radius: Float = DEFAULT_RADIUS): FloatArray {
        require(grid in 2..48 && radius in 0.025f..0.24f)
        val result = FloatArray(FACE_COUNT * verticesPerFace(grid) * STRIDE_FLOATS)
        var at = 0
        val inside = 0.5f - radius

        fun append(normal: V, right: V, down: V, u: Float, v: Float) {
            val point = normal * 0.5f + right * (u - 0.5f) + down * (v - 0.5f)
            val inner = V(
                point.x.coerceIn(-inside, inside),
                point.y.coerceIn(-inside, inside),
                point.z.coerceIn(-inside, inside)
            )
            val outward = (point - inner).normalized()
            val position = inner + outward * radius
            result[at++] = position.x
            result[at++] = position.y
            result[at++] = position.z
            result[at++] = outward.x
            result[at++] = outward.y
            result[at++] = outward.z
            result[at++] = u
            result[at++] = v
        }

        for ((normal, right, down) in axes) {
            for (y in 0 until grid) {
                for (x in 0 until grid) {
                    val u0 = x.toFloat() / grid
                    val u1 = (x + 1).toFloat() / grid
                    val v0 = y.toFloat() / grid
                    val v1 = (y + 1).toFloat() / grid
                    // Outward-facing CCW triangles for GLES GL_CULL_FACE.
                    append(normal, right, down, u0, v0)
                    append(normal, right, down, u0, v1)
                    append(normal, right, down, u1, v0)
                    append(normal, right, down, u1, v0)
                    append(normal, right, down, u0, v1)
                    append(normal, right, down, u1, v1)
                }
            }
        }
        check(at == result.size)
        return result
    }
}
