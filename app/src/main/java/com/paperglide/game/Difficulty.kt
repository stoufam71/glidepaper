package com.paperglide.game

enum class Medal { None, Bronze, Silver, Gold }

/** Score-driven tuning. All numbers are playtest starting values. */
object Difficulty {
    const val START_GAP = 0.32f
    const val MIN_GAP = 0.24f
    const val START_SPEED = 0.45f
    const val MAX_SPEED = 0.65f
    const val PLATEAU_SCORE = 60

    const val BRONZE = 10
    const val SILVER = 25
    const val GOLD = 50

    private fun progress(score: Int) = score.coerceIn(0, PLATEAU_SCORE).toFloat() / PLATEAU_SCORE

    /** Gap height in world units (world height is 1). */
    fun gap(score: Int) = START_GAP + (MIN_GAP - START_GAP) * progress(score)

    /** Scroll speed in world units per second. */
    fun speed(score: Int) = START_SPEED + (MAX_SPEED - START_SPEED) * progress(score)

    fun medal(score: Int) = when {
        score >= GOLD -> Medal.Gold
        score >= SILVER -> Medal.Silver
        score >= BRONZE -> Medal.Bronze
        else -> Medal.None
    }
}
