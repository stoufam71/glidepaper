package com.paperglide.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
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
import com.paperglide.game.GameEngine
import com.paperglide.game.Medal
import com.paperglide.game.Phase

@Composable
fun GameScreen(vm: GameViewModel, onMuteFocus: (Boolean) -> Unit = {}) {
    var aspect by remember { mutableFloatStateOf(0.5625f) }
    val plane = remember { PlaneShapes() }
    val stars = remember { Stars() }
    val traffic = remember { SkyTraffic() }
    val critters = remember { Critters() }
    val airliner = remember { AirlinerShapes() }
    val playFocus = remember { FocusRequester() }

    LaunchedEffect(vm) {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(GameEngine.MAX_DT)
                    val night = DayCycle.at(vm.engine.playTime).night
                    val before = vm.engine.distance
                    vm.tick(dt, aspect)
                    if (vm.phase != Phase.Paused) {
                        traffic.update(dt, aspect, night)
                        critters.update(dt, aspect, night, vm.engine.distance - before)
                    }
                }
                last = now
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Text scales with the screen (capped by width for narrow phones) so it reads in hand and across a room.
        val unit = with(LocalDensity.current) {
            minOf(maxHeight.toPx() * 0.032f, maxWidth.toPx() * 0.06f).toSp().value
        }
        val freeSky = minOf(maxWidth, maxHeight * FREE_SKY)

        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { if (it.height > 0) aspect = it.width.toFloat() / it.height }
                .pointerInput(vm) { awaitEachGesture { awaitFirstDown(); vm.flap() } },
        ) {
            vm.frame // redraw every tick
            drawWorld(vm.engine, plane, stars, traffic, airliner, critters)
        }

        // Dims the frozen world so the pause card reads as on top of it.
        if (vm.phase == Phase.Paused) Box(Modifier.fillMaxSize().background(Palette.Ink.copy(alpha = 0.3f)))

        // Holds D-pad focus during play so TV OK flaps instead of landing on the mute button.
        Box(Modifier.align(Alignment.CenterStart).size(1.dp).focusRequester(playFocus).focusable())
        LaunchedEffect(Unit) { playFocus.requestFocus() }

        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (vm.phase != Phase.Ready) {
                Label(
                    "${vm.score}", unit * 1.6f, Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    color = Color.White, shadow = true,
                )
            }
            when (vm.phase) {
                // Centre the hint in the open sky before the first pillar so it never covers one.
                Phase.Ready -> Box(Modifier.width(freeSky).fillMaxHeight()) {
                    Hint("Tap or press OK to fly", unit, Modifier.align(BelowPlane))
                }
                Phase.Paused -> PauseCard(unit, Modifier.align(BelowPlane))
                Phase.GameOver -> GameOverCard(
                    vm.score, vm.best, vm.newBest, Difficulty.medal(vm.score), unit,
                    Modifier.align(Alignment.Center),
                )
                Phase.Playing -> Unit
            }
            if (vm.autopilot) {
                Hint("Autopilot", unit * 0.7f, Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            MuteButton(vm.muted, vm::toggleMute, onMuteFocus, playFocus, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}

/** Hints sit under the plane's hover height so they never cover it. */
private val BelowPlane = BiasAlignment(0f, 0.35f)

/** Screen-height multiples free of pillars at the start of a round. */
private const val FREE_SKY = GameEngine.PLANE_X + GameEngine.FIRST_PILLAR_DISTANCE

@Composable
private fun Label(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    color: Color = Palette.Ink,
    shadow: Boolean = false,
) {
    BasicText(
        text,
        modifier,
        style = TextStyle(
            color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            shadow = if (shadow) Shadow(Palette.Ink, Offset(0f, 3f), 6f) else null,
        ),
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

/** White paper card shared by the pause and game over dialogs; pops in so its cause and effect read clearly. */
@Composable
private fun PopCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }
    Column(
        modifier
            .graphicsLayer {
                val s = 0.85f + 0.15f * pop.value
                scaleX = s; scaleY = s; alpha = pop.value.coerceIn(0f, 1f)
            }
            // Flat drop shadow drawn in the card's own layer, so it fades and scales with the pop-in.
            // An elevation shadow flickered as a dark box while the layer alpha animated.
            // While fading, the layer renders offscreen clipped to its bounds; this room keeps the shadow inside them.
            .padding(bottom = 6.dp)
            .drawBehind {
                drawRoundRect(
                    Palette.Ink.copy(alpha = 0.18f),
                    topLeft = Offset(0f, 6.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                )
            }
            .background(Color.White, CardShape)
            .border(2.dp, Palette.CloudShade, CardShape)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** The one thing to do next; tapping anywhere does it, so this is a label styled as the primary action. */
@Composable
private fun ActionPill(text: String, unit: Float) {
    Label(
        text, unit * 0.75f,
        Modifier.background(Palette.Ink, CircleShape).padding(horizontal = 24.dp, vertical = 10.dp),
        color = Color.White,
    )
}

@Composable
fun PauseCard(textUnit: Float, modifier: Modifier = Modifier) {
    PopCard(modifier) {
        // Pause glyph backs up the title so the state isn't carried by text alone.
        Row(
            Modifier
                .size((textUnit * 2.4f).dp)
                .background(Palette.CloudShade, CircleShape),
            horizontalArrangement = Arrangement.spacedBy((textUnit * 0.3f).dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) {
                Box(
                    Modifier
                        .size((textUnit * 0.32f).dp, (textUnit * 1.0f).dp)
                        .background(Palette.Ink, RoundedCornerShape(50)),
                )
            }
        }
        Label("Paused", textUnit * 1.5f)
        ActionPill("Tap or press OK to go on", textUnit)
        Label("Press Back again to quit", textUnit * 0.65f, color = Palette.Ink.copy(alpha = 0.8f))
    }
}

@Composable
fun GameOverCard(score: Int, best: Int, newBest: Boolean, medal: Medal, textUnit: Float, modifier: Modifier = Modifier) {
    PopCard(modifier) {
        Label("Game over", textUnit * 1.5f)
        if (medal != Medal.None) MedalBadge(medal, textUnit)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Score", score, textUnit)
            Stat("Best", best, textUnit)
        }
        if (newBest) {
            Label(
                "New best!", textUnit * 0.8f,
                Modifier.background(Palette.Highlight, CircleShape).padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color.White,
            )
        }
        ActionPill("Tap or press OK", textUnit)
    }
}

private val CardShape = RoundedCornerShape(28.dp)

/** One number with a small caption; read aloud as "Score 27". */
@Composable
private fun Stat(caption: String, value: Int, unit: Float) {
    Column(
        Modifier
            .background(Palette.CloudShade, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "$caption $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(caption.uppercase(), unit * 0.6f, color = Palette.Ink.copy(alpha = 0.8f))
        Label("$value", unit * 1.6f)
    }
}

@Composable
private fun MedalBadge(medal: Medal, unit: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size((unit * 1.8f).dp)
                .background(medal.color, CircleShape)
                .border(3.dp, Color(0x40000000), CircleShape)
                .padding(6.dp)
                .border(2.dp, Color(0x99FFFFFF), CircleShape),
        )
        Label("${medal.name} medal", unit)
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
