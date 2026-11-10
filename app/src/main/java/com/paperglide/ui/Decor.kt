package com.paperglide.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin

private val Blush = Color(0xFFFFA9B8)
private val PlaneStripe = Color(0xFF6FA8DC)
private val NightPlane = Color(0xFF3B4766)
private val Beacon = Color(0xFFFF5A5A)

/**
 * A fluffy background cloud: flat base, three puffs, softer underside. [u] is the cloud's half-width in px.
 * Cute ones get a face that closes its eyes at night.
 */
fun DrawScope.drawFluffyCloud(center: Offset, u: Float, color: Color, shade: Color, face: Boolean, sleepy: Boolean) {
    val x = center.x
    val y = center.y
    for ((c, dy) in listOf(shade to 0.07f * u, color to 0f)) {
        drawRoundRect(c, Offset(x - u, y + dy), Size(2 * u, 0.36f * u), CornerRadius(0.18f * u))
        drawCircle(c, 0.40f * u, Offset(x - 0.55f * u, y + 0.06f * u + dy))
        drawCircle(c, 0.56f * u, Offset(x, y - 0.12f * u + dy))
        drawCircle(c, 0.42f * u, Offset(x + 0.55f * u, y + 0.02f * u + dy))
    }
    if (face) drawCuteFace(Offset(x, y + 0.08f * u), 0.5f * u, color.alpha, sleepy)
}

/** Two eyes, blush and a small smile, sized by [w] (face width in px). */
fun DrawScope.drawCuteFace(center: Offset, w: Float, alpha: Float, sleepy: Boolean) {
    val ink = Palette.Ink.copy(alpha = 0.75f * alpha)
    val eyeDx = 0.32f * w
    val eye = 0.07f * w
    for (side in listOf(-1f, 1f)) {
        val e = center + Offset(side * eyeDx, 0f)
        if (sleepy) {
            drawArc(ink, 20f, 140f, false, e - Offset(eye * 1.4f, eye * 1.4f), Size(eye * 2.8f, eye * 2.8f), style = Stroke(eye * 0.8f))
        } else {
            drawCircle(ink, eye, e)
        }
        drawOval(Blush.copy(alpha = 0.55f * alpha), center + Offset(side * eyeDx * 1.55f - eye * 1.6f, eye * 1.3f), Size(eye * 3.2f, eye * 1.8f))
    }
    drawArc(
        ink, 20f, 140f, false, center + Offset(-eye * 1.6f, eye * 0.2f), Size(eye * 3.2f, eye * 2.4f),
        style = Stroke(eye * 0.7f),
    )
}

/** Small airliner and its contrail in unit coordinates (nose at +x); built once. */
class AirlinerShapes {
    val body = Path().apply {
        moveTo(-1f, -0.12f); lineTo(0.75f, -0.12f); quadraticTo(1.05f, -0.1f, 1.05f, 0f)
        quadraticTo(1.05f, 0.1f, 0.75f, 0.12f); lineTo(-0.9f, 0.12f); close()
    }
    val tail = Path().apply { moveTo(-1f, -0.1f); lineTo(-1.05f, -0.55f); lineTo(-0.75f, -0.55f); lineTo(-0.55f, -0.1f); close() }
    val wing = Path().apply { moveTo(-0.1f, 0.02f); lineTo(0.25f, 0.02f); lineTo(-0.25f, 0.45f); lineTo(-0.4f, 0.45f); close() }
}

fun DrawScope.drawFlyer(f: Flyer, s: Float, time: Float, night: Float, airliner: AirlinerShapes) {
    val center = Offset(f.x * s, f.y * s)
    when (f.kind) {
        Flyer.Kind.Birds -> {
            val ink = Palette.Ink.copy(alpha = 0.55f * (1f - night))
            val span = 0.024f * s
            repeat(f.count) { i ->
                // Loose V: each extra bird trails behind and below the leader.
                val back = if (f.vx > 0) -1f else 1f
                val c = center + Offset(back * i * span * 2.2f, (if (i % 2 == 0) 1f else -1f) * i * span * 0.9f)
                val flap = sin(time * 11f + f.phase + i) * 0.6f * span
                drawLine(ink, c, c + Offset(-span, -flap), strokeWidth = 0.18f * span)
                drawLine(ink, c, c + Offset(span, -flap), strokeWidth = 0.18f * span)
            }
        }
        Flyer.Kind.Plane -> {
            val k = 0.035f * s
            val dir = if (f.vx > 0) 1f else -1f
            val body = lerp(Color.White, NightPlane, night)
            // Contrail fades away behind the tail.
            for (i in 0 until 6) {
                val from = center + Offset(-dir * (1.0f + i * 0.9f) * k, 0f)
                val to = center + Offset(-dir * (1.9f + i * 0.9f) * k, 0f)
                drawLine(Color.White.copy(alpha = (0.45f - i * 0.07f) * (1f - 0.6f * night)), from, to, strokeWidth = 0.16f * k)
            }
            translate(center.x, center.y) {
                scale(dir * k, k, pivot = Offset.Zero) {
                    drawPath(airliner.tail, lerp(PlaneStripe, NightPlane, night))
                    drawPath(airliner.body, body)
                    drawPath(airliner.wing, lerp(Palette.CloudShade, NightPlane, night))
                    for (w in 0 until 4) drawCircle(lerp(PlaneStripe, Color(0xFFFFE9A6), night), 0.045f, Offset(0.55f - w * 0.22f, -0.03f))
                }
            }
            if (night > 0f && sin(time * 6f + f.phase) > 0.6f) {
                drawCircle(Beacon.copy(alpha = night), 0.08f * k, center + Offset(-dir * 0.95f * k, -0.5f * k))
            }
        }
    }
}
