package com.paperglide

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.paperglide.game.Phase
import com.paperglide.input.KeyMapping
import com.paperglide.ui.GameScreen

class MainActivity : ComponentActivity() {

    internal val viewModel: GameViewModel by viewModels()
    private var muteFocused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One APK serves both: phones lock portrait, TV keeps its native landscape.
        if (!isTv()) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // Never let the plane fly unseen: backgrounding, calls and lost focus all pause.
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.phase == Phase.Playing) viewModel.pause() else finish()
            }
        })

        setContent { GameScreen(viewModel, onMuteFocus = { muteFocused = it }) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (!KeyMapping.isFlapKey(code) || (muteFocused && KeyMapping.isSelectKey(code))) {
            return super.dispatchKeyEvent(event)
        }
        // Flap keys are consumed on every action so a held D-pad Up never moves focus.
        if (KeyMapping.isFlap(event)) viewModel.flap()
        return true
    }

    private fun isTv(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
}
