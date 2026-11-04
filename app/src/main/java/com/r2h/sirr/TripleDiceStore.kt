package com.r2h.sirr

import com.r2h.sirr.core.PersonalDiceFace
import com.r2h.sirr.core.TripleDiceConfiguration
import org.json.JSONArray
import org.json.JSONObject

/**
 * New AES-GCM vault key. Old dice-pair-v1 and legacy dice data are never
 * overwritten or deleted by this component.
 */
internal data class TripleDiceRestore(
    val configuration: TripleDiceConfiguration?,
    val unreadable: Boolean,
    val fromOldPair: Boolean
)

internal object TripleDiceStore {
    private const val KEY = "dice-triple-v1"
    private const val VERSION = 1

    fun load(vault: LocalVault): TripleDiceRestore {
        if (vault.hasPrivate(KEY)) {
            val result = runCatching {
                val raw = requireNotNull(vault.readPrivate(KEY))
                require(raw.length <= 45_000)
                val json = JSONObject(raw)
                require(json.getInt("version") == VERSION)
                val all = json.getJSONArray("cubes")
                require(all.length() == TripleDiceConfiguration.CUBE_COUNT)
                val cubes = (0 until all.length()).map { cubeIndex ->
                    val six = all.getJSONArray(cubeIndex)
                    require(six.length() == TripleDiceConfiguration.FACES_PER_CUBE)
                    (0 until six.length()).map { faceIndex ->
                        val face = six.getJSONObject(faceIndex)
                        PersonalDiceFace(
                            text = face.getString("text"),
                            weight = face.getInt("weight"),
                            imageId = face.optString("image").takeUnless {
                                it.isBlank() || it == "null"
                            }
                        )
                    }
                }
                TripleDiceConfiguration(
                    cubes = cubes, durationSeconds = json.getInt("duration")
                )
            }.getOrNull()
            return TripleDiceRestore(result, unreadable = result == null, fromOldPair = false)
        }

        // Read-only migration: both authored pair cubes are copied exactly;
        // the third is blank. Explicit save creates the new independent key.
        val pair = PersonalDiceStore.load(vault)
        if (pair.unreadable) return TripleDiceRestore(null, true, true)
        val converted = pair.configuration?.let(TripleDiceConfiguration::fromPair)
        return TripleDiceRestore(converted, false, converted != null)
    }

    fun save(vault: LocalVault, config: TripleDiceConfiguration) {
        config.validateStructure()
        val cubes = JSONArray()
        config.cubes.forEach { six ->
            val faces = JSONArray()
            six.forEach { face ->
                faces.put(
                    JSONObject()
                        .put("text", face.text)
                        .put("weight", face.weight)
                        .put("image", face.imageId)
                )
            }
            cubes.put(faces)
        }
        vault.writePrivate(
            KEY,
            JSONObject().put("version", VERSION)
                .put("duration", config.durationSeconds)
                .put("cubes", cubes).toString()
        )
    }

    /** Keep a valid triple record even after reset, to avoid re-importing pair. */
    fun resetToBlank(vault: LocalVault) = save(vault, TripleDiceConfiguration.blank())
}
