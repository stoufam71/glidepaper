package com.paperglide

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ManifestTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun resolves(category: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(context.packageName)
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    @Test
    fun phoneLauncherListsTheGame() = assertTrue(resolves(Intent.CATEGORY_LAUNCHER))

    @Test
    fun tvLauncherListsTheGame() = assertTrue(resolves(Intent.CATEGORY_LEANBACK_LAUNCHER))

    @Test
    fun appHasTvBanner() = assertNotEquals(0, context.applicationInfo.banner)

    @Test
    fun phoneLocksPortrait() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "television")
    fun tvDoesNotLockPortrait() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        assertNotEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }
}
