package com.r2h.sirr.core

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Real-world cube centers and dimensions for a shared ground plane.
 * Screen-space "shadows" and artificial y-offset stacking were replaced
 * with three objects resting on the same y plane at final orientation.
 */
internal data class TripleCubePose(
    val x: Float,
    val z: Float,
    val scale: Float
)

internal object TripleDiceStageGeometry {
    const val CUBE_COUNT = 3
    const val FACES_PER_CUBE = 6
    const val FLOOR_Y = -1.05f
    const val CAMERA_Z = 4.60f

    // Projected face area is balanced: the front cube is physically smaller
    // because perspective already magnifies it. Keep >0 sphere-gap for spins.
    // The same face indices and 18 texture bindings remain unchanged.
    private val poses = listOf(
        TripleCubePose(-1.04f, -0.65f, 0.92f),
        TripleCubePose( 1.04f, -0.65f, 0.92f),
        TripleCubePose( 0.00f,  0.64f, 0.85f)
    )

    fun pose(cube: Int): TripleCubePose {
        require(cube in 0 until CUBE_COUNT)
        return poses[cube]
    }

    /** This bound includes all eight vertices of a rotated cube. */
    fun verticalHalfExtent(rotated: FloatArray, scale: Float): Float {
        require(rotated.size == 16 && scale > 0f && scale.isFinite())
        val extent = (abs(rotated[1]) + abs(rotated[5]) + abs(rotated[9])) *
            (0.5f * scale)
        require(extent.isFinite())
        return extent
    }

    /** The 3D object's lowest point never passes below the shared floor. */
    fun restingCenterY(rotated: FloatArray, scale: Float): Float =
        FLOOR_Y + verticalHalfExtent(rotated, scale)

    /** Conservative bounding-sphere separation even during full 3-axis rotation. */
    fun footprintGap(a: Int, b: Int): Float {
        require(a != b)
        val p = pose(a)
        val q = pose(b)
        // Each cube fits within a sphere of radius scale*sqrt(3)/2.
        return hypot(p.x - q.x, p.z - q.z) -
            (p.scale + q.scale) * 0.8660254f
    }

    /**
     * Approximate on-screen size before factoring camera pitch/yaw.
     * Used to catch accidental front-cube over-scaling during review.
     */
    fun apparentScaleProxy(cube: Int): Float {
        val p = pose(cube)
        return p.scale / (CAMERA_Z - p.z)
    }

    /** Exactly 18 cube-major face textures, unchanged from P0.2. */
    fun textureIndex(cube: Int, face: Int): Int {
        require(cube in 0 until CUBE_COUNT)
        require(face in 0 until FACES_PER_CUBE)
        return cube * FACES_PER_CUBE + face
    }
}
