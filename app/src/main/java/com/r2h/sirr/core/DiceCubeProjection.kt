package com.r2h.sirr.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Testable 3D cube geometry; does not draw or author any face imagery. */
internal data class DicePoint2(val x: Float, val y: Float)
internal data class DiceProjectedFace(
    val index: Int,
    val corners: List<DicePoint2>, // top-left, top-right, bottom-right, bottom-left
    val averageDepth: Float,
    val facing: Float
)

internal object DiceCubeProjection {
    private data class Point3(val x: Float, val y: Float, val z: Float)
    private val faceCorners: List<List<Point3>> = listOf(
        // FRONT (+z), RIGHT (+x), BACK (-z), LEFT (-x), TOP (+y), BOTTOM (-y)
        listOf(Point3(-.5f,.5f,.5f), Point3(.5f,.5f,.5f), Point3(.5f,-.5f,.5f), Point3(-.5f,-.5f,.5f)),
        listOf(Point3(.5f,.5f,.5f), Point3(.5f,.5f,-.5f), Point3(.5f,-.5f,-.5f), Point3(.5f,-.5f,.5f)),
        listOf(Point3(.5f,.5f,-.5f), Point3(-.5f,.5f,-.5f), Point3(-.5f,-.5f,-.5f), Point3(.5f,-.5f,-.5f)),
        listOf(Point3(-.5f,.5f,-.5f), Point3(-.5f,.5f,.5f), Point3(-.5f,-.5f,.5f), Point3(-.5f,-.5f,-.5f)),
        listOf(Point3(-.5f,.5f,-.5f), Point3(.5f,.5f,-.5f), Point3(.5f,.5f,.5f), Point3(-.5f,.5f,.5f)),
        listOf(Point3(-.5f,-.5f,.5f), Point3(.5f,-.5f,.5f), Point3(.5f,-.5f,-.5f), Point3(-.5f,-.5f,-.5f))
    )
    private val normals = listOf(
        Point3(0f,0f,1f), Point3(1f,0f,0f), Point3(0f,0f,-1f),
        Point3(-1f,0f,0f), Point3(0f,1f,0f), Point3(0f,-1f,0f)
    )

    /** Shared canonical face-to-camera angles for GLES2 and the legacy projection tests. */
    fun baseRotation(selectedIndex: Int): Pair<Float, Float> {
        require(selectedIndex in 0..5)
        val x = when (selectedIndex) { 4 -> 90f; 5 -> -90f; else -> 0f }
        val y = when (selectedIndex) { 1 -> -90f; 2 -> 180f; 3 -> 90f; else -> 0f }
        return x to y
    }

    private fun rad(degrees: Float): Double = degrees.toDouble() * PI / 180.0
    private fun rotateX(p: Point3, degrees: Float): Point3 {
        val c = cos(rad(degrees)).toFloat()
        val s = sin(rad(degrees)).toFloat()
        return Point3(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
    }
    private fun rotateY(p: Point3, degrees: Float): Point3 {
        val c = cos(rad(degrees)).toFloat()
        val s = sin(rad(degrees)).toFloat()
        return Point3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
    }

    /**
     * A face's INDEX, not its text, determines the final cube orientation.
     * Additional spinning angles can make full turns and settle back to zero.
     */
    fun faces(
        selectedIndex: Int,
        spinX: Float,
        spinY: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<DiceProjectedFace> {
        require(selectedIndex in 0..5)
        require(canvasWidth > 0f && canvasHeight > 0f)
        require(spinX.isFinite() && spinY.isFinite())
        val (baseX, baseY) = baseRotation(selectedIndex)
        fun transform(p: Point3): Point3 {
            val facing = rotateY(rotateX(p, baseX), baseY)
            val spinning = rotateY(rotateX(facing, spinX), spinY)
            return rotateY(rotateX(spinning, 23f), -28f)
        }
        val diameter = min(canvasWidth, canvasHeight) * .54f
        fun project(p: Point3): DicePoint2 {
            val perspective = 3.5f / (3.5f - p.z)
            return DicePoint2(
                canvasWidth * .5f + p.x * diameter * perspective,
                canvasHeight * .5f - p.y * diameter * perspective
            )
        }
        return faceCorners.mapIndexedNotNull { index, quad ->
            val visibility = transform(normals[index]).z
            if (visibility <= .015f) null
            else {
                val transformed = quad.map(::transform)
                DiceProjectedFace(
                    index = index,
                    corners = transformed.map(::project),
                    averageDepth = transformed.sumOf { it.z.toDouble() }.toFloat() / 4f,
                    facing = visibility
                )
            }
        }.sortedBy { it.averageDepth }
    }
}
