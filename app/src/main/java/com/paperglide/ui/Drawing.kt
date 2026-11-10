package com.paperglide.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val CloudShade = Color(0xFFDCEAF6)
    val Moon = Color(0xFFFFF6D8)
    val MoonCrater = Color(0xFFEDE2BC)
    val Star = Color(0xFFFFFFFF)
    val PlaneLight = Color(0xFFFFFFFF)
    val PlaneShade = Color(0xFFD3E1EE)
    val PlaneFold = Color(0xFFF4B6C2)
    val Ink = Color(0xFF23415A)
    val Highlight = Color(0xFFC2365A) // white text on it passes 4.5:1
}

/** Paper plane shapes in plane-radius units, centred on the hitbox; built once. */
class PlaneShapes {
    val wing = Path().apply { moveTo(-1.6f, -0.2f); lineTo(1.5f, -0.9f); lineTo(-0.2f, 0.5f); close() }
    val body = Path().apply { moveTo(-0.2f, 0.5f); lineTo(1.5f, -0.9f); lineTo(0.2f, 1.3f); close() }
    val fold = Path().apply { moveTo(-0.2f, 0.5f); lineTo(0.2f, 1.3f); lineTo(-0.25f, 0.95f); close() }
}

/** Fixed star field as screen fractions (x of width, y of height), with a twinkle phase each. */
class Stars {
    val points: List<Triple<Float, Float, Float>> = kotlin.random.Random(42).let { r ->
        List(40) { Triple(r.nextFloat(), r.nextFloat() * 0.7f, r.nextFloat() * 6.28f) }
    }
}

fun DrawScope.drawWorld(engine: GameEngine, plane: PlaneShapes, stars: Stars, traffic: SkyTraffic, airliner: AirlinerShapes, critters: Critters) {
    val s = size.height
    val sky = DayCycle.at(engine.playTime)
    // shortcut: one small gradient object per frame; cache by colour if GC shows up on low-end TV boxes
    drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)))

    if (sky.night > 0f) {
        for ((fx, fy, phase) in stars.points) {
            val twinkle = 0.6f + 0.4f * kotlin.math.sin(engine.time * 2.5f + phase)
            drawCircle(Palette.Star.copy(alpha = sky.night * twinkle), 0.004f * s, Offset(fx * size.width, fy * GROUND_TOP * s))
        }
        val moon = Offset(size.width * 0.72f, sky.moonY * s)
        drawCircle(Palette.Moon.copy(alpha = 0.25f * sky.night), 0.085f * s, moon)
        drawCircle(Palette.Moon.copy(alpha = sky.night), 0.055f * s, moon)
        drawCircle(Palette.MoonCrater.copy(alpha = sky.night), 0.012f * s, moon + Offset(-0.018f * s, -0.012f * s))
        drawCircle(Palette.MoonCrater.copy(alpha = sky.night), 0.008f * s, moon + Offset(0.017f * s, 0.016f * s))
    }
    if (sky.sunY < DayCycle.SUN_GONE + 0.07f) {
        val sun = Offset(size.width * 0.78f, sky.sunY * s)
        drawCircle(sky.sunColor.copy(alpha = 0.3f), 0.1f * s, sun)
        drawCircle(sky.sunColor, 0.065f * s, sun)
    }

    // Far clouds drift slowly for depth; they take the sky's tint and every other one has a face.
    // Opaque blends of cloud and sky, so overlapping puffs show no seams.
    val farCloud = lerp(sky.top, sky.cloud, 0.8f - 0.3f * sky.night)
    val farShade = lerp(farCloud, sky.top, 0.3f)
    val worldWidth = size.width / s
    for (i in 0 until 4) {
        val span = worldWidth + 0.5f
        val x = ((i * 0.47f + 0.2f) - engine.time * 0.03f).mod(span) - 0.25f
        val y = 0.12f + (i % 3) * 0.16f
        val u = (0.075f + 0.02f * (i % 2)) * s
        drawFluffyCloud(Offset(x * s, y * s), u, farCloud, farShade, face = i % 2 == 0, sleepy = sky.night > 0.5f)
    }

    for (f in traffic.flyers) drawFlyer(f, s, engine.time, sky.night, airliner)

    // Pillars stay inside their collision rectangle so every hit looks fair: puffy ends are drawn within it.
    val pillarCloud = lerp(Color.White, sky.cloud, 0.6f)
    val pillarShade = lerp(Palette.CloudShade, sky.cloud, 0.5f).copy(alpha = 1f)
    val highlight = Color.White.copy(alpha = 0.55f * (1f - 0.6f * sky.night))
    for (p in engine.pillars) {
        val left = p.x * s
        val w = PILLAR_WIDTH * s
        val r = w / 4
        val top = (p.gapCenter - p.gapHeight / 2) * s
        val bottom = (p.gapCenter + p.gapHeight / 2) * s
        val ground = GROUND_TOP * s
        // Body, a shaded right side, then three puffs whose outer edge sits exactly on the gap edge.
        drawRect(pillarCloud, Offset(left, 0f), Size(w, top - r))
        drawRect(pillarCloud, Offset(left, bottom + r), Size(w, ground - bottom - r))
        drawRect(pillarShade, Offset(left + w * 0.8f, 0f), Size(w * 0.2f, top - r))
        drawRect(pillarShade, Offset(left + w * 0.8f, bottom + r), Size(w * 0.2f, ground - bottom - r))
        for (k in 0 until 3) {
            val cx = left + r * (1 + k)
            drawCircle(pillarCloud, r, Offset(cx, top - r))
            drawCircle(pillarCloud, r, Offset(cx, bottom + r))
        }
        drawCircle(highlight, r * 0.35f, Offset(left + r * 0.8f, bottom + r * 0.7f))
        drawCuteFace(Offset(left + w / 2, top - r * 0.9f), w * 0.55f, 1f, sleepy = sky.night > 0.5f)
        drawCuteFace(Offset(left + w / 2, bottom + r * 1.1f), w * 0.55f, 1f, sleepy = sky.night > 0.5f)
    }

    drawMeadow(engine.distance, sky.night, engine.time, critters)

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
            style = Stroke(stroke),
        )
    }
}
