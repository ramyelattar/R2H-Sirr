package com.r2h.sirr.core

import kotlin.random.Random

/**
 * Independent 3-cube contract: 18 owner-authored faces, no predefined phrases.
 * The existing two-cube PersonalDiceConfiguration and its encrypted storage
 * are preserved without schema changes.
 */
internal data class TripleDiceConfiguration(
    val cubes: List<List<PersonalDiceFace>>,
    val durationSeconds: Int = 3
) {
    init { validateStructure() }

    fun validateStructure() {
        require(cubes.size == CUBE_COUNT && cubes.all { it.size == FACES_PER_CUBE }) {
            "Triple Dice requires exactly 3 cubes with 6 faces each"
        }
        require(durationSeconds in DURATIONS) { "Unsupported roll duration" }
    }

    fun validatePlayable() {
        validateStructure()
        require(cubes.all { cube ->
            cube.sumOf { it.weight } == 100 &&
                cube.any { it.weight > 0 } &&
                cube.all { it.weight == 0 || it.configured }
        }) { "Each cube needs a 100% distribution and authored, positive-weight faces" }
    }

    val playable: Boolean get() = runCatching { validatePlayable() }.isSuccess

    fun changeFace(cubeIndex: Int, faceIndex: Int, changed: PersonalDiceFace): TripleDiceConfiguration {
        require(cubeIndex in 0 until CUBE_COUNT && faceIndex in 0 until FACES_PER_CUBE)
        return copy(cubes = cubes.mapIndexed { cube, faces ->
            if (cube != cubeIndex) faces else faces.mapIndexed { index, previous ->
                if (index == faceIndex) changed else previous
            }
        })
    }

    fun roll(pick: (Int) -> Int = { Random.nextInt(it) }): List<Int> {
        validatePlayable()
        return cubes.map { cube ->
            GameLogic.weightedFace(cube.map { it.weight }, pick)
        }
    }

    companion object {
        const val CUBE_COUNT = 3
        const val FACES_PER_CUBE = 6
        val DURATIONS = listOf(2, 3, 5, 7, 10, 13, 20)

        fun blank(): TripleDiceConfiguration = TripleDiceConfiguration(
            cubes = List(CUBE_COUNT) {
                List(FACES_PER_CUBE) { face ->
                    PersonalDiceFace(weight = GameLogic.evenWeights[face])
                }
            }
        )

        /** In-memory copy, never deletes or rewrites the old two-cube vault. */
        fun fromPair(pair: PersonalDiceConfiguration): TripleDiceConfiguration {
            pair.validateStructure()
            return TripleDiceConfiguration(
                cubes = pair.cubes.map { it.toList() } + listOf(blank().cubes[2]),
                durationSeconds = pair.durationSeconds
            )
        }
    }
}
