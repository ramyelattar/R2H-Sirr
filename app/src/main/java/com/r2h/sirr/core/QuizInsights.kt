package com.r2h.sirr.core

/** Descriptive response counts only. Not a psychological assessment or compatibility score. */
data class ResponseSummary(val counts: List<Int>, val total: Int) {
    init { require(counts.size == 3 && counts.all { it >= 0 } && counts.sum() == total && total > 0) }
    fun percentage(option: Int): Int {
        require(option in 0..2)
        return (counts[option] * 100 + total / 2) / total
    }
    /** Null on tie; there is no single most commonly chosen option. */
    val dominantOption: Int? get() {
        val max = counts.max()
        return counts.indexOf(max).takeIf { counts.count { it == max } == 1 }
    }
}

data class ComparisonSummary(
    val first: ResponseSummary, val second: ResponseSummary,
    val agreement: Int, val questions: Int
) {
    val agreementPercent: Int get() = (agreement * 100 + questions / 2) / questions
}

object QuizInsights {
    fun summary(answers: List<Int>): ResponseSummary {
        require(answers.isNotEmpty() && answers.all { it in 0..2 })
        return ResponseSummary((0..2).map { option -> answers.count { it == option } }, answers.size)
    }
    fun compare(a: List<Int>, b: List<Int>): ComparisonSummary {
        require(a.isNotEmpty() && a.size == b.size)
        return ComparisonSummary(summary(a), summary(b), a.zip(b).count { it.first == it.second }, a.size)
    }
}
