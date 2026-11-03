package com.r2h.sirr.core

/**
 * Persisted countdown state for one challenge. Remaining time is restored
 * from a saved wall-clock timestamp; no timer depends on Compose state.
 */
data class ChallengeClock(
    val durationSeconds: Int = 300,
    val remainingSecondsOnPause: Int = 300,
    val runningSinceMillis: Long? = null,
    val started: Boolean = false
) {
    init {
        require(durationSeconds in 30..300)
        require(remainingSecondsOnPause in 0..durationSeconds)
        require(runningSinceMillis == null || runningSinceMillis > 0L)
        require(started || runningSinceMillis == null)
    }

    val running: Boolean get() = runningSinceMillis != null

    fun remainingSeconds(nowMillis: Long): Int {
        val since = runningSinceMillis ?: return remainingSecondsOnPause
        val elapsed = ((nowMillis - since).coerceAtLeast(0L) / 1_000L)
        return (remainingSecondsOnPause.toLong() - elapsed).coerceAtLeast(0L).toInt()
    }

    fun start(nowMillis: Long, challengeSeconds: Int = durationSeconds): ChallengeClock {
        require(nowMillis > 0L)
        require(challengeSeconds in 30..300)
        if (running) return this
        return if (!started) ChallengeClock(
            durationSeconds = challengeSeconds,
            remainingSecondsOnPause = challengeSeconds,
            runningSinceMillis = nowMillis,
            started = true
        ) else copy(runningSinceMillis = nowMillis)
    }

    fun pause(nowMillis: Long): ChallengeClock =
        if (!running) this else copy(
            remainingSecondsOnPause = remainingSeconds(nowMillis),
            runningSinceMillis = null
        )
}
