package com.r2h.sirr.core

import kotlin.random.Random

/**
 * Local, descriptive quiz session. The questions themselves are snapshotted so
 * a content refresh cannot silently change questions belonging to a saved run.
 *
 * No psychology score, correctness label, or cloud/remote participant identity.
 */
enum class QuizStage { FIRST, PREDICT, HANDOFF, SECOND, RESULTS }

/** Matching partner answers, not a relationship or psychological compatibility score. */
data class QuizPredictionSummary(val matched: Int, val comparable: Int) {
    init { require(comparable > 0 && matched in 0..comparable) }
}

data class QuizSession(
    val packId: String,
    val packName: String,
    val questions: List<Question>,
    val paired: Boolean,
    val stage: QuizStage = QuizStage.FIRST,
    val position: Int = 0,
    val answersA: List<Int> = emptyList(),
    val answersB: List<Int> = emptyList(),
    val revealPosition: Int = 0,
    val guessPartner: Boolean = false,
    val predictionsA: List<Int> = emptyList()
) {
    init {
        require(packId.matches(Regex("quiz_0[1-7]")))
        require(packName.isNotBlank() && packName.length <= 100)
        require(questions.size in listOf(8, 12, 20, 25))
        require(questions.map { it.text }.distinct().size == questions.size)
        require(questions.all { q ->
            q.text.isNotBlank() && q.text.length <= 240 &&
                q.choices.size == 3 &&
                q.choices.all { it.isNotBlank() && it.length <= 240 }
        })
        require(answersA.size <= questions.size && answersB.size <= questions.size)
        require(answersA.all { it in -1..2 } && answersB.all { it in -1..2 })
        require(!guessPartner || paired)
        require(predictionsA.size <= questions.size && predictionsA.all { it in -1..2 })
        if (!guessPartner) require(predictionsA.isEmpty())
        require(revealPosition in questions.indices)
        when (stage) {
            QuizStage.FIRST -> require(
                answersA.size == position && position in questions.indices &&
                    answersB.isEmpty() && predictionsA.isEmpty() && revealPosition == 0
            )
            QuizStage.PREDICT -> require(
                paired && guessPartner && answersA.size == questions.size &&
                    answersB.isEmpty() && predictionsA.size == position &&
                    position in questions.indices && revealPosition == 0
            )
            QuizStage.HANDOFF -> require(
                paired && answersA.size == questions.size && answersB.isEmpty() &&
                    predictionsA.size == (if (guessPartner) questions.size else 0) &&
                    position == 0 && revealPosition == 0
            )
            QuizStage.SECOND -> require(
                paired && answersA.size == questions.size &&
                    predictionsA.size == (if (guessPartner) questions.size else 0) &&
                    answersB.size == position && position in questions.indices &&
                    revealPosition == 0
            )
            QuizStage.RESULTS -> require(
                answersA.size == questions.size &&
                    predictionsA.size == (if (guessPartner) questions.size else 0) &&
                    (!paired || answersB.size == questions.size) &&
                    (paired || answersB.isEmpty()) && position == 0
            )
        }
    }

    val currentQuestion: Question
        get() = questions[if (stage == QuizStage.RESULTS) revealPosition else position]

    fun answer(choice: Int): QuizSession {
        require(choice in -1..2)
        require(stage == QuizStage.FIRST || stage == QuizStage.SECOND)
        val isFirst = stage == QuizStage.FIRST
        val nextAnswers = (if (isFirst) answersA else answersB) + choice
        val completed = nextAnswers.size == questions.size
        return if (isFirst) {
            copy(
                answersA = nextAnswers,
                position = if (completed) 0 else nextAnswers.size,
                stage = if (completed) {
                    when {
                        guessPartner -> QuizStage.PREDICT
                        paired -> QuizStage.HANDOFF
                        else -> QuizStage.RESULTS
                    }
                } else QuizStage.FIRST
            )
        } else {
            copy(
                answersB = nextAnswers,
                position = if (completed) 0 else nextAnswers.size,
                stage = if (completed) QuizStage.RESULTS else QuizStage.SECOND
            )
        }
    }

    /** Record one forecast without exposing the partner's actual choices. */
    fun predict(choice: Int): QuizSession {
        require(stage == QuizStage.PREDICT && choice in -1..2)
        val next = predictionsA + choice
        val done = next.size == questions.size
        return copy(
            predictionsA = next,
            position = if (done) 0 else next.size,
            stage = if (done) QuizStage.HANDOFF else QuizStage.PREDICT
        )
    }

    /** Undo is limited to the current stage and never changes the partner's answers. */
    fun back(): QuizSession {
        require(stage in listOf(QuizStage.FIRST, QuizStage.PREDICT, QuizStage.SECOND) && position > 0)
        return when (stage) {
            QuizStage.FIRST -> copy(answersA = answersA.dropLast(1), position = position - 1)
            QuizStage.PREDICT -> copy(predictionsA = predictionsA.dropLast(1), position = position - 1)
            QuizStage.SECOND -> copy(answersB = answersB.dropLast(1), position = position - 1)
            else -> error("Unreachable quiz stage")
        }
    }

    fun startPartner(): QuizSession {
        require(stage == QuizStage.HANDOFF && paired)
        return copy(stage = QuizStage.SECOND)
    }

    fun revealNext(): QuizSession {
        require(stage == QuizStage.RESULTS && revealPosition < questions.lastIndex)
        return copy(revealPosition = revealPosition + 1)
    }

    fun revealPrevious(): QuizSession {
        require(stage == QuizStage.RESULTS && revealPosition > 0)
        return copy(revealPosition = revealPosition - 1)
    }

    fun comparison(): ComparisonSummary? =
        if (paired && !guessPartner && stage == QuizStage.RESULTS)
            QuizInsights.compareAnswered(answersA, answersB) else null

    /** Ignores both skipped forecasts and unanswered partner choices. */
    fun predictionSummary(): QuizPredictionSummary? {
        if (!guessPartner || stage != QuizStage.RESULTS) return null
        val comparable = predictionsA.indices.filter { predictionsA[it] >= 0 && answersB[it] >= 0 }
        if (comparable.isEmpty()) return null
        return QuizPredictionSummary(
            matched = comparable.count { predictionsA[it] == answersB[it] },
            comparable = comparable.size
        )
    }

    companion object {
        fun start(
            packId: String, quiz: Quiz, count: Int,
            paired: Boolean, random: Random = Random.Default,
            guessPartner: Boolean = false
        ): QuizSession {
            require(count in listOf(8, 12, 20, 25))
            require(quiz.questions.size == 25)
            require(quiz.questions.map { it.text }.distinct().size == 25)
            val indices = quiz.questions.indices.shuffled(random).take(count)
            return QuizSession(
                packId = packId,
                packName = quiz.name,
                questions = indices.map { index ->
                    val q = quiz.questions[index]
                    Question(q.text, q.choices.toList())
                },
                paired = paired,
                guessPartner = guessPartner
            )
        }
    }
}
