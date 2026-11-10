package com.paperglide

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/** Robolectric can carry SharedPreferences between test classes; start each test from a fresh install. */
class ClearPrefsRule : ExternalResource() {
    override fun before() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("paper_glide", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
