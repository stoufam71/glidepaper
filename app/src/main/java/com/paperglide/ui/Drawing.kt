package com.paperglide.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.paperglide.game.GameEngine
import com.paperglide.game.GameEngine.Companion.GROUND_TOP
import com.paperglide.game.GameEngine.Companion.PILLAR_WIDTH
import com.paperglide.game.GameEngine.Companion.PLANE_RADIUS
import com.paperglide.game.GameEngine.Companion.PLANE_X
import com.paperglide.game.Phase

object Palette {
    val SkyTop = Color(0xFF8FD0F5)
    val SkyBottom = Color(0xFFDDF3FF)
    val Cloud = Color(0xFFFFFFFF)
    val CloudShade = Color(0xFFDCEAF6)
    val FarCloud = Color(0x66FFFFFF)
    val Ground = Color(0xFFB7E86A)
    val GroundStripe = Color(0xFF9FD458)
    val PlaneLight = Color(0xFFFFFFFF)
    val PlaneShade = Color(0xFFD3E1EE)
    val PlaneFold = Color(0xFFF4B6C2)
    val Ink = Color(0xFF23415A)
}

/** Paper plane shapes in plane-radius units, centred on the hitbox; built once. */
class PlaneShapes {
    val wing = Path().apply { moveTo(-1.6f, -0.2f); lineTo(1.5f, -0.9f); lineTo(-0.2f, 0.5f); close() }
    val body = Path().apply { moveTo(-0.2f, 0.5f); lineTo(1.5f, -0.9f); lineTo(0.2f, 1.3f); close() }
    val fold = Path().apply { moveTo(-0.2f, 0.5f); lineTo(0.2f, 1.3f); lineTo(-0.25f, 0.95f); close() }
}

fun DrawScope.drawWorld(engine: GameEngine, sky: Brush, plane: PlaneShapes) {
    val s = size.height
    drawRect(sky)

    // Far clouds drift slowly for depth.
    val worldWidth = size.width / s
    for (i in 0 until 4) {
        val span = worldWidth + 0.5f
        val x = ((i * 0.47f + 0.2f) - engine.time * 0.03f).mod(span) - 0.25f
        val y = 0.12f + (i % 3) * 0.16f
        drawCircle(Palette.FarCloud, 0.07f * s, Offset(x * s, y * s))
        drawCircle(Palette.FarCloud, 0.05f * s, Offset((x + 0.07f) * s, (y + 0.015f) * s))
    }

    // Pillars stay inside their collision rectangle so every hit looks fair.
    for (p in engine.pillars) {
        val left = p.x * s
        val w = PILLAR_WIDTH * s
        val top = (p.gapCenter - p.gapHeight / 2) * s
        val bottom = (p.gapCenter + p.gapHeight / 2) * s
        val ground = GROUND_TOP * s
        val r = CornerRadius(w * 0.18f, w * 0.18f)
        drawRoundRect(Palette.Cloud, Offset(left, -w), Size(w, top + w), r)
        drawRoundRect(Palette.Cloud, Offset(left, bottom), Size(w, ground - bottom + w), r)
        drawRect(Palette.CloudShade, Offset(left + w * 0.78f, 0f), Size(w * 0.22f, top - w * 0.18f))
        drawRect(Palette.CloudShade, Offset(left + w * 0.78f, bottom + w * 0.18f), Size(w * 0.22f, ground - bottom))
        // Scalloped gap edges read as cloud puffs.
        val puff = w / 6
        for (i in 0 until 3) {
            val cx = left + puff * (1 + 2 * i)
            drawCircle(Palette.CloudShade, puff * 0.55f, Offset(cx, top - puff))
            drawCircle(Palette.CloudShade, puff * 0.55f, Offset(cx, bottom + puff))
        }
    }

    drawRect(Palette.Ground, Offset(0f, GROUND_TOP * s), Size(size.width, size.height - GROUND_TOP * s))
    val stripe = 0.08f * s
    val shift = (engine.pillars.firstOrNull()?.x ?: 0f) * s
    var x = shift.mod(stripe * 2) - stripe * 2
    while (x < size.width) {
        drawRect(Palette.GroundStripe, Offset(x, GROUND_TOP * s), Size(stripe, 0.012f * s))
        x += stripe * 2
    }

    val tilt = if (engine.phase == Phase.Ready) 0f else (engine.velocity * 30f).coerceIn(-25f, 60f)
    val k = PLANE_RADIUS * s
    translate(PLANE_X * s, engine.planeY * s) {
        rotate(tilt, pivot = Offset.Zero) {
            scale(k, pivot = Offset.Zero) {
                drawPath(plane.wing, Palette.PlaneLight)
                drawPath(plane.body, Palette.PlaneShade)
                drawPath(plane.fold, Palette.PlaneFold)
            }
        }
    }
}

/** Speaker glyph for the mute button; a slash marks muted. */
fun DrawScope.drawSpeaker(muted: Boolean) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(0.1f * w, 0.38f * h); lineTo(0.3f * w, 0.38f * h); lineTo(0.55f * w, 0.15f * h)
        lineTo(0.55f * w, 0.85f * h); lineTo(0.3f * w, 0.62f * h); lineTo(0.1f * w, 0.62f * h); close()
    }
    drawPath(body, Palette.Ink)
    val stroke = 0.09f * w
    if (muted) {
        drawLine(Palette.Ink, Offset(0.65f * w, 0.32f * h), Offset(0.95f * w, 0.68f * h), stroke)
        drawLine(Palette.Ink, Offset(0.95f * w, 0.32f * h), Offset(0.65f * w, 0.68f * h), stroke)
    } else {
        drawArc(
            Palette.Ink, -50f, 100f, false, Offset(0.42f * w, 0.25f * h), Size(0.4f * w, 0.5f * h),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}
