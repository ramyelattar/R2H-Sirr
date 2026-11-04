package com.r2h.sirr.core

/** Immutable design-stage positions, shared by the renderer and JVM tests. */
internal data class TripleCubePose(
    val x: Float,
    val y: Float,
    val z: Float,
    val scale: Float,
    val shadowY: Float
)

internal object TripleDiceStageGeometry {
    const val CUBE_COUNT = 3
    const val FACES_PER_CUBE = 6

    private val poses = listOf(
        TripleCubePose(-0.81f, 0.48f, -0.23f, 0.90f, 0.02f),
        TripleCubePose( 0.81f, 0.48f, -0.23f, 0.90f, 0.02f),
        TripleCubePose( 0.00f,-0.67f,  0.33f, 1.07f,-0.83f)
    )

    fun pose(cube: Int): TripleCubePose {
        require(cube in 0 until CUBE_COUNT)
        return poses[cube]
    }

    /** 18 textures in exactly the same cube-major order as the UI tiles. */
    fun textureIndex(cube: Int, face: Int): Int {
        require(cube in 0 until CUBE_COUNT)
        require(face in 0 until FACES_PER_CUBE)
        return cube * FACES_PER_CUBE + face
    }
}
