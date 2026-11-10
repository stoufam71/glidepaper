package com.paperglide

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
class MusicPlayerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var music: MusicPlayer

    @Before
    fun setUp() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.bg_music_paper_glide}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo(33_000, 0))
        music = MusicPlayer(context)
        assertNotNull(music.player)
    }

    private val playing get() = music.player!!.isPlaying

    @Test
    fun playsInForegroundWithSoundOn() {
        music.update(foreground = true, muted = false)
        assertTrue(playing)
    }

    @Test
    fun finishedCopyHandsOverToTheQueuedOneForAGaplessLoop() {
        val first = music.player!!
        music.update(foreground = true, muted = false)
        shadowOf(first).invokeCompletionListener()
        val second = music.player!!
        assertNotSame(first, second)
        // The platform starts the queued copy itself; pausing must now reach it.
        second.start()
        music.update(foreground = false, muted = false)
        assertFalse(second.isPlaying)
    }

    @Test
    fun muteSilencesMusic() {
        music.update(foreground = true, muted = false)
        music.update(foreground = true, muted = true)
        assertFalse(playing)
    }

    @Test
    fun leavingTheAppStopsMusicAndReturningResumes() {
        music.update(foreground = true, muted = false)
        music.update(foreground = false, muted = false)
        assertFalse(playing)
        music.update(foreground = true, muted = false)
        assertTrue(playing)
    }

    @Test
    fun neverStartsInBackground() {
        music.update(foreground = false, muted = false)
        assertFalse(playing)
    }
}
