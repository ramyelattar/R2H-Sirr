package com.r2h.sirr.core

import kotlin.random.Random

/**
 * Local, descriptive quiz session. The questions themselves are snapshotted so
 * a content refresh cannot silently change questions belonging to a saved run.
 *
 * No psychology score, correctness label, or cloud/remote participant identity.
 */
enum class QuizStage { FIRST, HANDOFF, SECOND, RESULTS }

data class QuizSession(
    val packId: String,
    val packName: String,
    val questions: List<Question>,
    val paired: Boolean,
    val stage: QuizStage = QuizStage.FIRST,
    val position: Int = 0,
    val answersA: List<Int> = emptyList(),
    val answersB: List<Int> = emptyList(),
    val revealPosition: Int = 0
) {
    init {
        require(packId.matches(Regex("quiz_0[1-7]")))
        require(packName.isNotBlank() && packName.length <= 100)
        require(questions.size in listOf(8, 12, 20, 25))
        require(questions.all { q ->
            q.text.isNotBlank() && q.text.length <= 240 &&
                q.choices.size == 3 &&
                q.choices.all { it.isNotBlank() && it.length <= 240 }
        })
        require(answersA.size <= questions.size && answersB.size <= questions.size)
        require(answersA.all { it in -1..2 } && answersB.all { it in -1..2 })
        require(revealPosition in questions.indices)
        when (stage) {
            QuizStage.FIRST -> require(
                answersA.size == position && position in questions.indices &&
                    answersB.isEmpty() && revealPosition == 0
            )
            QuizStage.HANDOFF -> require(
                paired && answersA.size == questions.size && answersB.isEmpty() &&
                    position == 0 && revealPosition == 0
            )
            QuizStage.SECOND -> require(
                paired && answersA.size == questions.size &&
                    answersB.size == position && position in questions.indices &&
                    revealPosition == 0
            )
            QuizStage.RESULTS -> require(
                answersA.size == questions.size &&
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
                stage = if (completed) (if (paired) QuizStage.HANDOFF else QuizStage.RESULTS)
                    else QuizStage.FIRST
            )
        } else {
            copy(
                answersB = nextAnswers,
                position = if (completed) 0 else nextAnswers.size,
                stage = if (completed) QuizStage.RESULTS else QuizStage.SECOND
            )
        }
    }

    /** Undo belongs only to the current respondent; never exposes the other person's answers. */
    fun back(): QuizSession {
        require(stage in listOf(QuizStage.FIRST, QuizStage.SECOND) && position > 0)
        return if (stage == QuizStage.FIRST)
            copy(answersA = answersA.dropLast(1), position = position - 1)
        else copy(answersB = answersB.dropLast(1), position = position - 1)
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
        if (paired && stage == QuizStage.RESULTS)
            QuizInsights.compareAnswered(answersA, answersB) else null

    companion object {
        fun start(
            packId: String, quiz: Quiz, count: Int,
            paired: Boolean, random: Random = Random.Default
        ): QuizSession {
            require(count in listOf(8, 12, 20, 25))
            require(quiz.questions.size >= count)
            val indices = quiz.questions.indices.shuffled(random).take(count)
            return QuizSession(
                packId = packId,
                packName = quiz.name,
                questions = indices.map(quiz.questions::get),
                paired = paired
            )
        }
    }
}
