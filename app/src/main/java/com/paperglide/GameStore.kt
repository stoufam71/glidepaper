package com.paperglide

import android.content.Context
import androidx.core.content.edit

/** Best score and the mute switch, kept on this device only. Writes commit at once so a kill right after a crash keeps the best. */
class GameStore(context: Context) {

    private val prefs = context.getSharedPreferences("paper_glide", Context.MODE_PRIVATE)

    val best: Int get() = prefs.getInt(KEY_BEST, 0)

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTED, false)
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_MUTED, value) }

    /** Saves [score] if it beats the best; returns whether it did. */
    fun record(score: Int): Boolean {
        if (score <= best) return false
        prefs.edit(commit = true) { putInt(KEY_BEST, score) }
        return true
    }

    private companion object {
        const val KEY_BEST = "best"
        const val KEY_MUTED = "muted"
    }
}
