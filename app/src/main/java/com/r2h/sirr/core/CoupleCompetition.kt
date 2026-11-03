package com.r2h.sirr.core

/**
 * Competition rules for new two-player local sessions only.
 * Existing v1 sessions and group games keep their original scoring.
 */
enum class CoupleOutcome { COMPLETED, NOT_COMPLETED, AGREED_SKIP }

enum class TieStage { SHARED_CHALLENGE, KNOWLEDGE, DECIDED }

data class KnowledgePrompt(val question: String, val expectedAnswer: String) {
    init {
        require(question.trim().length in 6..180)
        require(expectedAnswer.trim().length in 1..140)
    }
}

data class CoupleTieBreak(
    val stage: TieStage = TieStage.SHARED_CHALLENGE,
    val sharedCompleted: List<Boolean> = emptyList(),
    val questions: List<KnowledgePrompt> = emptyList(),
    val knowledgeRound: Int = 1,
    val winnerIndex: Int? = null
) {
    init {
        require(sharedCompleted.isEmpty() || sharedCompleted.size == 2)
        require(questions.isEmpty() || questions.size == 2)
        require(knowledgeRound in 1..1000)
        require(winnerIndex == null || winnerIndex in 0..1)
        require((stage == TieStage.DECIDED) == (winnerIndex != null))
    }
}

data class CoupleCompetition(
    val reward: String,
    val sessionMinutes: Int,
    val startedAtMillis: Long,
    val outcomes: List<CoupleOutcome> = emptyList(),
    val settledEarly: Boolean = false,
    val tieBreak: CoupleTieBreak? = null,
    val roundClock: ChallengeClock = ChallengeClock()
) {
    init {
        require(reward.trim().length in 1..200)
        require(sessionMinutes in listOf(0, 15, 30, 60))
        require(startedAtMillis > 0L)
        require(outcomes.size <= 18)
    }

    /** Zero means no session time limit. The limit is advisory, never an automatic penalty. */
    fun timeExpired(nowMillis: Long): Boolean =
        sessionMinutes != 0 &&
            nowMillis >= startedAtMillis + sessionMinutes.toLong() * 60_000L
}
