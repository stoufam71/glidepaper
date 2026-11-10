package com.paperglide.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.paperglide.game.GameEngine.Companion.GROUND_TOP
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

private val GrassTop = Color(0xFFC6F07E)
private val GrassDeep = Color(0xFFA6DC5E)
private val Blade = Color(0xFF86C24A)
private val NightTint = Color(0xFF1E3A2C)
private val PetalPink = Color(0xFFFFB3C7)
private val PetalWhite = Color(0xFFFFFFFF)
private val FlowerHeart = Color(0xFFFFD45C)
private val BunnyFur = Color(0xFFFAF6F2)
private val EarPink = Color(0xFFFFC4D2)
private val KittenFur = Color(0xFFF6B66E)
private val KittenStripe = Color(0xFFE39A4E)
private val ChickFeather = Color(0xFFFFDB4D)
private val ChickWing = Color(0xFFF7C52E)
private val Beak = Color(0xFFFF9F43)

/** Unit shapes, nose to the left (-x); built once. */
private val KittenEar = Path().apply { moveTo(-1f, 0f); lineTo(0f, -1.4f); lineTo(1f, 0f); close() }
private val ChickBeak = Path().apply { moveTo(0f, -0.5f); lineTo(-1f, 0f); lineTo(0f, 0.5f); close() }

/**
 * Rolling grass with tufts and little flowers that scroll with the world, and baby animals walking on it.
 * Everything sits on or below [GROUND_TOP] except the soft grass bumps, so a crash still looks like touching ground.
 */
fun DrawScope.drawMeadow(distance: Float, night: Float, time: Float, critters: Critters) {
    val s = size.height
    val g = GROUND_TOP * s
    val scroll = distance * s
    fun dusk(c: Color) = lerp(c, NightTint, 0.7f * night)

    drawRect(dusk(GrassDeep), Offset(0f, g), Size(size.width, size.height - g))
    // Lighter lip along the top with scalloped bumps.
    val bump = 0.03f * s
    drawRect(dusk(GrassTop), Offset(0f, g + bump * 0.1f), Size(size.width, bump * 0.6f))
    var x = -scroll.mod(bump) - bump
    while (x < size.width + bump) {
        drawCircle(dusk(GrassTop), bump * 0.55f, Offset(x, g + bump * 0.15f))
        x += bump
    }

    for (c in critters.critters) drawCritter(c, Offset(c.x * s, g + 0.012f * s), 0.03f * s, time, ::dusk)

    // Tufts and flowers sit in fixed world cells so the same ones scroll past, never reshuffled.
    val cell = 0.09f * s
    val first = floor(scroll / cell).toInt() - 1
    val last = first + (size.width / cell).toInt() + 3
    for (i in first..last) {
        val pick = hash(i * 7 + 3)
        if (pick > 0.8f) continue
        val p = Offset(i * cell - scroll + hash(i) * cell * 0.6f, g + 0.035f * s + hash(i * 13 + 1) * 0.035f * s)
        if (pick < 0.5f) drawTuft(p, 0.016f * s, dusk(Blade))
        else drawFlower(p, 0.006f * s, dusk(if (hash(i * 5) < 0.5f) PetalPink else PetalWhite), dusk(FlowerHeart))
    }
}

/** Stable 0..1 noise for a cell index. */
private fun hash(i: Int): Float {
    var h = i * 374761393
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private fun DrawScope.drawTuft(base: Offset, h: Float, color: Color) {
    val w = h * 0.22f
    for (dx in listOf(-0.45f, 0f, 0.45f)) {
        drawLine(color, base, base + Offset(dx * h, if (dx == 0f) -h else -h * 0.75f), w, StrokeCap.Round)
    }
}

private fun DrawScope.drawFlower(c: Offset, r: Float, petal: Color, heart: Color) {
    for (k in 0 until 5) {
        val a = k * 1.2566f
        drawCircle(petal, r, c + Offset(kotlin.math.cos(a) * r * 1.1f, sin(a) * r * 1.1f))
    }
    drawCircle(heart, r * 0.75f, c)
}

/** [feet] is the ground point under the animal; [a] its size unit in px. Drawn facing left, mirrored when walking right. */
private fun DrawScope.drawCritter(c: Critter, feet: Offset, a: Float, time: Float, dusk: (Color) -> Color) {
    val t = time + c.phase
    scale(if (c.vx > 0f) -1f else 1f, 1f, pivot = feet) {
        translate(feet.x, feet.y) {
            when (c.kind) {
                Critter.Kind.Bunny -> drawBunny(a, abs(sin(t * 6f)) * a * 0.7f, dusk)
                Critter.Kind.Kitten -> drawKitten(a, t, dusk)
                Critter.Kind.Chick -> drawChick(a, abs(sin(t * 9f)) * a * 0.25f, dusk)
            }
        }
    }
}

private fun DrawScope.drawBunny(a: Float, hop: Float, dusk: (Color) -> Color) {
    val fur = dusk(BunnyFur)
    val y = -hop
    drawOval(fur, Offset(-0.45f * a, y - 0.3f * a), Size(0.55f * a, 0.3f * a)) // hind foot
    drawOval(fur, Offset(-0.6f * a, y - 1.05f * a), Size(1.5f * a, 1.0f * a)) // body
    drawCircle(fur, 0.24f * a, Offset(0.92f * a, y - 0.75f * a)) // tail
    val head = Offset(-0.55f * a, y - 1.2f * a)
    for ((dx, tilt) in listOf(-0.22f to -0.08f, 0.08f to 0.1f)) {
        drawOval(fur, Offset(head.x + dx * a + tilt * a, head.y - 1.15f * a), Size(0.3f * a, 0.85f * a))
        drawOval(dusk(EarPink), Offset(head.x + (dx + 0.07f) * a + tilt * a, head.y - 1.02f * a), Size(0.16f * a, 0.6f * a))
    }
    drawCircle(fur, 0.48f * a, head)
    drawCuteFace(head + Offset(-0.08f * a, 0.02f * a), 0.62f * a, 1f, sleepy = false)
    drawCircle(dusk(EarPink), 0.06f * a, head + Offset(-0.42f * a, -0.02f * a)) // nose
}

private fun DrawScope.drawKitten(a: Float, t: Float, dusk: (Color) -> Color) {
    val fur = dusk(KittenFur)
    val stripe = dusk(KittenStripe)
    val step = sin(t * 8f) * 0.1f * a
    val bob = abs(sin(t * 8f)) * 0.06f * a
    for ((lx, d) in listOf(-0.5f to step, -0.25f to -step, 0.45f to -step, 0.7f to step)) {
        drawLine(fur, Offset(lx * a + d, -0.6f * a), Offset(lx * a + d, -0.08f * a), 0.2f * a, StrokeCap.Round)
    }
    // Tail curls up and sways.
    val tail = Path().apply {
        moveTo(0.85f * a, -0.85f * a - bob)
        quadraticTo(1.35f * a, -0.9f * a, 1.2f * a + step, -1.6f * a)
    }
    drawPath(tail, fur, style = Stroke(0.18f * a, cap = StrokeCap.Round))
    drawOval(fur, Offset(-0.7f * a, -1.2f * a - bob), Size(1.7f * a, 0.75f * a)) // body
    drawLine(stripe, Offset(0.1f * a, -1.15f * a - bob), Offset(0.1f * a, -0.9f * a - bob), 0.1f * a, StrokeCap.Round)
    drawLine(stripe, Offset(0.4f * a, -1.15f * a - bob), Offset(0.4f * a, -0.95f * a - bob), 0.1f * a, StrokeCap.Round)
    val head = Offset(-0.7f * a, -1.35f * a - bob)
    for (side in listOf(-1f, 1f)) {
        translate(head.x + side * 0.26f * a, head.y - 0.28f * a) { scale(0.3f * a, pivot = Offset.Zero) { drawPath(KittenEar, fur) } }
    }
    drawCircle(fur, 0.48f * a, head)
    drawCuteFace(head + Offset(0f, 0.04f * a), 0.62f * a, 1f, sleepy = false)
}

private fun DrawScope.drawChick(a: Float, hop: Float, dusk: (Color) -> Color) {
    val feather = dusk(ChickFeather)
    val y = -hop
    for (lx in listOf(-0.15f, 0.15f)) {
        drawLine(dusk(Beak), Offset(lx * a, y - 0.25f * a), Offset(lx * a, 0f), 0.08f * a, StrokeCap.Round)
    }
    drawCircle(feather, 0.5f * a, Offset(0.05f * a, y - 0.7f * a)) // body
    val head = Offset(-0.25f * a, y - 1.2f * a)
    drawCircle(feather, 0.36f * a, head)
    drawOval(dusk(ChickWing), Offset(0.05f * a, y - 0.8f * a), Size(0.45f * a, 0.28f * a))
    drawLine(feather, head + Offset(0f, -0.3f * a), head + Offset(0.08f * a, -0.5f * a), 0.07f * a, StrokeCap.Round) // tuft
    translate(head.x - 0.32f * a, head.y + 0.05f * a) { scale(0.2f * a, pivot = Offset.Zero) { drawPath(ChickBeak, dusk(Beak)) } }
    drawCuteFace(head + Offset(0.05f * a, 0f), 0.48f * a, 1f, sleepy = false)
}
