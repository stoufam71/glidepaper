package com.paperglide

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun freshInstallHasNoBestAndSoundOn() {
        val store = GameStore(context)
        assertEquals(0, store.best)
        assertFalse(store.muted)
    }

    @Test
    fun beatingBestPersistsAcrossInstances() {
        GameStore(context).record(12)
        assertTrue(GameStore(context).record(14))
        assertEquals(14, GameStore(context).best)
    }

    @Test
    fun lowerScoreKeepsBest() {
        GameStore(context).record(30)
        assertFalse(GameStore(context).record(27))
        assertEquals(30, GameStore(context).best)
    }

    @Test
    fun tyingBestIsNotNewBest() {
        GameStore(context).record(20)
        assertFalse(GameStore(context).record(20))
    }

    @Test
    fun mutePersists() {
        GameStore(context).muted = true
        assertTrue(GameStore(context).muted)
    }
}
