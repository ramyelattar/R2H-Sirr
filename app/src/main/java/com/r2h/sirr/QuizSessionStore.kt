package com.r2h.sirr

import com.r2h.sirr.core.Question
import com.r2h.sirr.core.QuizSession
import com.r2h.sirr.core.QuizStage
import org.json.JSONArray
import org.json.JSONObject

/**
 * The only persistence path for user quiz answers.
 * Data is encrypted by the existing Android Keystore-backed LocalVault.
 * No quiz answers are copied into savedInstanceState or sent to a server.
 */
internal object QuizSessionStore {
    private const val KEY = "quiz-local-session-v1"
    private const val VERSION = 1

    fun save(vault: LocalVault, session: QuizSession) {
        val selected = JSONArray()
        session.questions.forEach { q ->
            selected.put(
                JSONObject().put("text", q.text)
                    .put("choices", JSONArray(q.choices))
            )
        }
        val payload = JSONObject()
            .put("version", VERSION)
            .put("packId", session.packId)
            .put("packName", session.packName)
            .put("paired", session.paired)
            .put("stage", session.stage.name)
            .put("position", session.position)
            .put("revealPosition", session.revealPosition)
            .put("questions", selected)
            .put("answersA", JSONArray(session.answersA))
            .put("answersB", JSONArray(session.answersB))
        vault.writePrivate(KEY, payload.toString())
    }

    fun load(vault: LocalVault): QuizSession? = runCatching {
        val raw = vault.readPrivate(KEY) ?: return null
        require(raw.length <= 100_000)
        val json = JSONObject(raw)
        require(json.getInt("version") == VERSION)
        val selected = json.getJSONArray("questions")
        require(selected.length() in listOf(8, 12, 20, 25))
        val questions = (0 until selected.length()).map { i ->
            val q = selected.getJSONObject(i)
            val choices = q.getJSONArray("choices")
            require(choices.length() == 3)
            Question(q.getString("text"), (0 until 3).map(choices::getString))
        }
        fun answers(name: String): List<Int> {
            val values = json.getJSONArray(name)
            require(values.length() <= selected.length())
            return (0 until values.length()).map(values::getInt)
        }
        QuizSession(
            packId = json.getString("packId"),
            packName = json.getString("packName"),
            questions = questions,
            paired = json.getBoolean("paired"),
            stage = QuizStage.valueOf(json.getString("stage")),
            position = json.getInt("position"),
            revealPosition = json.getInt("revealPosition"),
            answersA = answers("answersA"),
            answersB = answers("answersB")
        )
    }.getOrNull()

    fun clear(vault: LocalVault) = vault.deletePrivate(KEY)
}
