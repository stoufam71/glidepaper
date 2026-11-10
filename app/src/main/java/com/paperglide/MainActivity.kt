package com.paperglide

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One APK serves both: phones lock portrait, TV keeps its native landscape.
        if (!isTv()) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent { Box {} }
    }

    private fun isTv(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
}
