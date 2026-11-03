package com.r2h.sirr

import com.r2h.sirr.core.PersonalDiceConfiguration
import com.r2h.sirr.core.PersonalDiceFace
import org.json.JSONArray
import org.json.JSONObject

internal data class PersonalDiceRestore(
    val configuration: PersonalDiceConfiguration?,
    val unreadable: Boolean
)

/** Separate AES-GCM / AtomicFile key: never alters the existing three-dice vault. */
internal object PersonalDiceStore {
    private const val KEY = "dice-pair-v1"
    private const val VERSION = 1

    fun save(vault: LocalVault, config: PersonalDiceConfiguration) {
        config.validateStructure()
        val cubes = JSONArray()
        config.cubes.forEach { cube ->
            val faces = JSONArray()
            cube.forEach { face ->
                faces.put(
                    JSONObject()
                        .put("text", face.text)
                        .put("weight", face.weight)
                        .put("image", face.imageId)
                )
            }
            cubes.put(faces)
        }
        val json = JSONObject()
            .put("version", VERSION)
            .put("duration", config.durationSeconds)
            .put("cubes", cubes)
        vault.writePrivate(KEY, json.toString())
    }

    fun load(vault: LocalVault): PersonalDiceRestore {
        if (!vault.hasPrivate(KEY)) return PersonalDiceRestore(null, false)
        val restored = runCatching {
            val raw = requireNotNull(vault.readPrivate(KEY))
            require(raw.length <= 30_000)
            val json = JSONObject(raw)
            require(json.getInt("version") == VERSION)
            val cubes = json.getJSONArray("cubes")
            require(cubes.length() == 2)
            val config = PersonalDiceConfiguration(
                cubes = (0 until cubes.length()).map { cubeIndex ->
                    val faces = cubes.getJSONArray(cubeIndex)
                    require(faces.length() == 6)
                    (0 until faces.length()).map { faceIndex ->
                        val face = faces.getJSONObject(faceIndex)
                        PersonalDiceFace(
                            text = face.getString("text"),
                            weight = face.getInt("weight"),
                            imageId = face.optString("image").takeUnless {
                                it.isBlank() || it == "null"
                            }
                        )
                    }
                },
                durationSeconds = json.getInt("duration")
            )
            config
        }.getOrNull()
        return PersonalDiceRestore(restored, restored == null)
    }

    fun clear(vault: LocalVault) {
        vault.deletePrivate(KEY)
    }
}
