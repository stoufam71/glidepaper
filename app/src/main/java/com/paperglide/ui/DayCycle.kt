package com.paperglide.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Sky look at one moment. Y values are in world units (height 1); [sunY] >= [DayCycle.SUN_GONE] means set. */
data class Sky(
    val top: Color,
    val bottom: Color,
    val cloud: Color,
    val night: Float,
    val sunY: Float,
    val sunColor: Color,
    val moonY: Float,
)

/**
 * One round travels cloudy day, sunset (sun goes down), night (moon and stars), sunrise (sun comes up),
 * then day again, looping every [LENGTH] seconds of play.
 */
object DayCycle {

    data class Palette(val top: Color, val bottom: Color, val cloud: Color)

    val DAY = Palette(Color(0xFF8FD0F5), Color(0xFFDDF3FF), Color(0xFFFFFFFF))
    val SUNSET = Palette(Color(0xFFF2826A), Color(0xFFFFD39A), Color(0xFFFFE3D6))
    val NIGHT = Palette(Color(0xFF0E1A3A), Color(0xFF2E3F70), Color(0xFF8A97B8))
    val SUNRISE = Palette(Color(0xFF7FB8F0), Color(0xFFFFC9A8), Color(0xFFFFEDE3))

    const val LENGTH = 80f
    const val SUN_HIGH = 0.16f
    const val SUN_GONE = 1f
    const val MOON_HIGH = 0.18f

    private val sunHigh = Color(0xFFFFE27A)
    private val sunLow = Color(0xFFFF8C42)

    // Seconds into the cycle, palette at that moment; colours blend between neighbours.
    private val keys = listOf(
        0f to DAY, 18f to DAY, 24f to SUNSET, 32f to NIGHT, 46f to NIGHT, 54f to SUNRISE, 62f to DAY, LENGTH to DAY,
    )

    fun at(playTime: Float): Sky {
        val t = playTime.mod(LENGTH)
        val i = keys.indexOfLast { it.first <= t }.coerceAtMost(keys.size - 2)
        val (t0, a) = keys[i]
        val (t1, b) = keys[i + 1]
        val f = ((t - t0) / (t1 - t0)).coerceIn(0f, 1f)

        val night = when {
            t < 24f -> 0f
            t < 32f -> (t - 24f) / 8f
            t < 46f -> 1f
            t < 54f -> 1f - (t - 46f) / 8f
            else -> 0f
        }
        val sunY = when {
            t < 18f -> SUN_HIGH
            t < 30f -> SUN_HIGH + (SUN_GONE - SUN_HIGH) * (t - 18f) / 12f
            t < 50f -> SUN_GONE
            t < 62f -> SUN_GONE - (SUN_GONE - SUN_HIGH) * (t - 50f) / 12f
            else -> SUN_HIGH
        }
        val low = (sunY - SUN_HIGH) / (SUN_GONE - SUN_HIGH)
        return Sky(
            top = lerp(a.top, b.top, f),
            bottom = lerp(a.bottom, b.bottom, f),
            cloud = lerp(a.cloud, b.cloud, f),
            night = night,
            sunY = sunY,
            sunColor = lerp(sunHigh, sunLow, low),
            moonY = 1f - (1f - MOON_HIGH) * night,
        )
    }
}
