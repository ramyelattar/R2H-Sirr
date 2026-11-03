package com.r2h.sirr.core

import kotlin.random.Random

/**
 * Exactly two owner-configured cubes. Intentionally contains NO preset
 * phrases, images, icons, or other authored game content.
 *
 * This is independent from DiceConfiguration (legacy 3-cube mode).
 */
internal data class PersonalDiceFace(
    val text: String = "",
    val weight: Int = 0,
    val imageId: String? = null
) {
    init {
        require(text.length <= 72)
        require(weight in 0..100)
        require(imageId == null || Regex("[0-9a-f-]{36}").matches(imageId))
    }

    val configured: Boolean get() = text.isNotBlank() || imageId != null
}

internal data class PersonalDiceConfiguration(
    val cubes: List<List<PersonalDiceFace>>,
    val durationSeconds: Int = 3
) {
    init { validateStructure() }

    fun validateStructure() {
        require(cubes.size == 2 && cubes.all { it.size == 6 })
        require(durationSeconds in listOf(2, 3, 5, 7, 10, 13, 20))
        // Draft weights may temporarily have an incomplete total while being edited.
    }

    /** Positive-weight faces must be authored before the dice can roll. */
    fun validatePlayable() {
        validateStructure()
        require(cubes.all { cube -> cube.sumOf(PersonalDiceFace::weight) == 100 &&
            cube.any { it.weight > 0 } &&
            cube.all { it.weight == 0 || it.configured }
        })
    }

    val playable: Boolean get() = runCatching { validatePlayable() }.isSuccess

    fun changeFace(cubeIndex: Int, faceIndex: Int, changed: PersonalDiceFace): PersonalDiceConfiguration {
        require(cubeIndex in 0..1 && faceIndex in 0..5)
        return copy(cubes = cubes.mapIndexed { cube, faces ->
            if (cube != cubeIndex) faces else faces.mapIndexed { face, original ->
                if (face == faceIndex) changed else original
            }
        })
    }

    fun roll(pick: (Int) -> Int = { Random.nextInt(it) }): List<Int> {
        validatePlayable()
        return cubes.map { cube -> GameLogic.weightedFace(cube.map { it.weight }, pick) }
    }

    companion object {
        fun blank(): PersonalDiceConfiguration = PersonalDiceConfiguration(
            cubes = List(2) { List(6) { index ->
                PersonalDiceFace(weight = GameLogic.evenWeights[index])
            } }
        )
    }
}
