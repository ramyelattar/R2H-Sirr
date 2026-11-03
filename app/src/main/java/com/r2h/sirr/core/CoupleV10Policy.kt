package com.r2h.sirr.core

/**
 * v10 is the owner's immutable approved text catalog.
 * A separate eligibility layer prevents unsafe/no-stop challenges from appearing
 * in automated sessions without rewriting, deleting, or reclassifying the source.
 */
internal object CoupleV10Policy {
    /** Source IDs from content/couple_challenges_v10_review.json (19 review flags). */
    val excludedIds: Set<String> = setOf(
        "c-SOFT-3",
        "c-SOFT-5",
        "c-SOFT-10",
        "c-SOFT-16",
        "c-HOT-4",
        "c-HOT-5",
        "c-HOT-17",
        "c-HARD-3",
        "c-HARD-6",
        "c-HARD-7",
        "c-HARD-8",
        "c-HARD-10",
        "c-HARD-11",
        "c-HARD-12",
        "c-HARD-14",
        "c-HARD-15",
        "c-HARD-17",
        "c-HARD-18",
        "c-HARD-20"
    )

    fun total(level: Intensity): Int = CoupleStyleV10Catalog.deck(level).size
    fun eligibleCount(level: Intensity): Int = CoupleStyleV10Catalog.deck(level)
        .count { it.id !in excludedIds }

    fun deck(level: Intensity): List<Challenge> =
        CoupleStyleV10Catalog.deck(level)
            .filterNot { it.id in excludedIds }
            .map { activity ->
                Challenge(
                    id = activity.id,
                    level = activity.level,
                    text = activity.text,
                    title = activity.title,
                    kind = displayKind(activity.kindCode),
                    durationSeconds = activity.durationSeconds,
                    actionCode = activity.kindCode
                )
            }

    /**
     * Broad UI categories for balanced selection only.
     * The exact original v10 action code remains in Challenge.actionCode.
     */
    private fun displayKind(code: String): CoupleChallengeKind = when {
        listOf("WHISPER", "VOICE", "TALK", "DIRTY", "SOUND", "MOAN", "DESCRIBE")
            .any { code.contains(it) } -> CoupleChallengeKind.WHISPER
        listOf("SIGHT", "EYES", "MIRROR", "POSE", "WAIT", "EYE")
            .any { code.contains(it) } -> CoupleChallengeKind.TALK
        listOf("GUIDE", "CONTROL", "COMMAND", "CHOICE")
            .any { code.contains(it) } -> CoupleChallengeKind.CHOICE
        listOf("SWITCH", "MULTI", "TWICE", "ALL")
            .any { code.contains(it) } -> CoupleChallengeKind.TOGETHER
        listOf("KISS", "BREATH", "SLOW", "TOUCH", "HOLD")
            .any { code.contains(it) } -> CoupleChallengeKind.CLOSENESS
        else -> CoupleChallengeKind.SURPRISE
    }
}
