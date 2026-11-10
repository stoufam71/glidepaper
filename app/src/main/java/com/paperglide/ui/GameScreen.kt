package com.paperglide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperglide.GameViewModel
import com.paperglide.game.Difficulty
import com.paperglide.game.Medal
import com.paperglide.game.Phase

@Composable
fun GameScreen(vm: GameViewModel, onMuteFocus: (Boolean) -> Unit = {}) {
    var aspect by remember { mutableFloatStateOf(0.5625f) }
    val plane = remember { PlaneShapes() }
    val sky = remember { Brush.verticalGradient(listOf(Palette.SkyTop, Palette.SkyBottom)) }
    val playFocus = remember { FocusRequester() }

    LaunchedEffect(vm) {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) vm.tick((now - last) / 1e9f, aspect)
                last = now
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Text scales with screen height so it reads the same in hand and across a room.
        val unit = with(LocalDensity.current) { (maxHeight.toPx() * 0.045f).toSp().value }

        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { if (it.height > 0) aspect = it.width.toFloat() / it.height }
                .pointerInput(vm) { awaitEachGesture { awaitFirstDown(); vm.flap() } },
        ) {
            vm.frame // redraw every tick
            drawWorld(vm.engine, sky, plane)
        }

        // Holds D-pad focus during play so TV OK flaps instead of landing on the mute button.
        Box(Modifier.align(Alignment.CenterStart).size(1.dp).focusRequester(playFocus).focusable())
        LaunchedEffect(Unit) { playFocus.requestFocus() }

        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (vm.phase != Phase.Ready) {
                Label("${vm.score}", unit * 1.6f, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
            }
            when (vm.phase) {
                Phase.Ready -> Hint("Tap or press OK to fly", unit, Modifier.align(Alignment.Center))
                Phase.Paused -> Hint("Paused\nTap or press OK to go on\nBack again to quit", unit, Modifier.align(Alignment.Center))
                Phase.GameOver -> GameOverCard(
                    vm.score, vm.best, vm.newBest, Difficulty.medal(vm.score), unit,
                    Modifier.align(Alignment.Center),
                )
                Phase.Playing -> Unit
            }
            MuteButton(vm.muted, vm::toggleMute, onMuteFocus, playFocus, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}

@Composable
private fun Label(text: String, size: Float, modifier: Modifier = Modifier, color: Color = Palette.Ink) {
    BasicText(
        text,
        modifier,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
    )
}

@Composable
private fun Hint(text: String, unit: Float, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCCFFFFFF))
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) { Label(text, unit) }
}

@Composable
fun GameOverCard(score: Int, best: Int, newBest: Boolean, medal: Medal, textUnit: Float, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF2FFFFFF))
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label("Game over", textUnit * 1.3f)
        if (medal != Medal.None) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size((textUnit * 1.6f).dp)
                        .clip(CircleShape)
                        .background(medal.color)
                        .border(3.dp, Color(0x55000000), CircleShape),
                )
                Label("${medal.name} medal", textUnit)
            }
        }
        Label("Score $score", textUnit)
        Label("Best $best", textUnit)
        if (newBest) Label("New best!", textUnit, color = Color(0xFFD9466B))
        Label("Tap or press OK", textUnit * 0.7f)
    }
}

private val Medal.color
    get() = when (this) {
        Medal.Gold -> Color(0xFFF2C14E)
        Medal.Silver -> Color(0xFFC9D3DD)
        Medal.Bronze -> Color(0xFFD08B5B)
        Medal.None -> Color.Transparent
    }

@Composable
private fun MuteButton(
    muted: Boolean,
    onToggle: () -> Unit,
    onFocus: (Boolean) -> Unit,
    playFocus: FocusRequester,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .background(if (focused) Color(0xFFFFFFFF) else Color(0x99FFFFFF))
            .border(if (focused) 3.dp else 0.dp, Palette.Ink, CircleShape)
            .onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
            // D-pad Left/Down leaves the button so OK flaps again.
            .onKeyEvent {
                val leave = it.type == KeyEventType.KeyDown && (it.key == Key.DirectionLeft || it.key == Key.DirectionDown)
                if (leave) playFocus.requestFocus()
                leave
            }
            .clickable(role = Role.Switch, onClick = onToggle)
            .semantics { contentDescription = if (muted) "Unmute sound" else "Mute sound" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) { drawSpeaker(muted) }
    }
}
