package com.example.hud

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisCore(
    state: AssistantState,
    audioRms: Float,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_core")

    val baseDuration = when (state) {
        AssistantState.THINKING, AssistantState.UNDERSTANDING -> 2000
        AssistantState.LISTENING -> 3000
        AssistantState.SPEAKING -> 2500
        AssistantState.EXECUTING, AssistantState.VERIFYING -> 1500
        AssistantState.ERROR -> 1000
        AssistantState.OFFLINE -> 10000
        else -> 6000
    }

    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceAnimations) baseDuration * 2 else baseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_slow"
    )

    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceAnimations) baseDuration * 3 else (baseDuration * 1.5).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_rev"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    // State Colors
    val primaryColor = when (state) {
        AssistantState.IDLE -> CyanPrimary
        AssistantState.WAKE_DETECTED, AssistantState.LISTENING -> Color(0xFF00E5FF)
        AssistantState.UNDERSTANDING, AssistantState.THINKING -> TechAmber
        AssistantState.EXECUTING, AssistantState.VERIFYING, AssistantState.SUCCESS -> NeonGreen
        AssistantState.SPEAKING -> CyanSecondary
        AssistantState.ERROR -> LaserCrimson
        AssistantState.OFFLINE -> TextMuted
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> BlueElectric
        AssistantState.WAKE_DETECTED, AssistantState.LISTENING -> CyanPrimary
        AssistantState.UNDERSTANDING, AssistantState.THINKING -> Color(0xFFFF9100)
        AssistantState.EXECUTING, AssistantState.VERIFYING, AssistantState.SUCCESS -> Color(0xFF00E676)
        AssistantState.SPEAKING -> BlueElectric
        AssistantState.ERROR -> Color(0xFFFF1744)
        AssistantState.OFFLINE -> Color(0xFF223344)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) * 0.88f
        if (radius <= 0f) return@Canvas

        // 1. Background Grid & Subtle Radar Radial Lines
        drawRadarBackground(center, radius, primaryColor)

        // 2. Outermost Reticle / Arc Segments
        drawOuterReticleArcs(center, radius, rotationSlow, primaryColor, secondaryColor)

        // 3. Middle Segmented Concentric Rings
        drawMiddleSegments(center, radius * 0.72f, rotationReverse, primaryColor)

        // 4. Inner Orbiting Node Ring
        drawOrbitingNodes(center, radius * 0.52f, rotationSlow * 1.5f, primaryColor, secondaryColor)

        // 5. Dynamic Audio Reactive Ring
        val audioBoost = (audioRms.coerceIn(0f, 15f) / 15f) * 0.25f
        val innerRadius = radius * (0.32f * corePulse + audioBoost)
        drawInnerCore(center, innerRadius, primaryColor, secondaryColor)

        // 6. Laser Scanline overlay
        if (state == AssistantState.THINKING || state == AssistantState.UNDERSTANDING || state == AssistantState.EXECUTING) {
            val scanY = center.y - radius + (scanLineY * radius * 2f)
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(center.x - radius * 0.8f, scanY),
                end = Offset(center.x + radius * 0.8f, scanY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawRadarBackground(center: Offset, radius: Float, color: Color) {
    // Faint concentric circles
    drawCircle(
        color = color.copy(alpha = 0.08f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = color.copy(alpha = 0.06f),
        radius = radius * 0.72f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = color.copy(alpha = 0.05f),
        radius = radius * 0.45f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Crosshairs
    drawLine(
        color = color.copy(alpha = 0.15f),
        start = Offset(center.x - radius * 1.05f, center.y),
        end = Offset(center.x + radius * 1.05f, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = color.copy(alpha = 0.15f),
        start = Offset(center.x, center.y - radius * 1.05f),
        end = Offset(center.x, center.y + radius * 1.05f),
        strokeWidth = 1.dp.toPx()
    )
}

private fun DrawScope.drawOuterReticleArcs(
    center: Offset,
    radius: Float,
    rotation: Float,
    primary: Color,
    secondary: Color
) {
    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)

    // 4 major quadrant arcs
    for (i in 0 until 4) {
        val startAngle = rotation + (i * 90f) + 10f
        drawArc(
            color = if (i % 2 == 0) primary.copy(alpha = 0.85f) else secondary.copy(alpha = 0.7f),
            startAngle = startAngle,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = stroke
        )
    }

    // Corner tick markers
    for (deg in 0 until 360 step 30) {
        val rad = Math.toRadians((deg + rotation * 0.5f).toDouble())
        val p1 = Offset(
            center.x + (radius * 0.95f * cos(rad)).toFloat(),
            center.y + (radius * 0.95f * sin(rad)).toFloat()
        )
        val p2 = Offset(
            center.x + (radius * 1.02f * cos(rad)).toFloat(),
            center.y + (radius * 1.02f * sin(rad)).toFloat()
        )
        drawLine(
            color = primary.copy(alpha = 0.4f),
            start = p1,
            end = p2,
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private fun DrawScope.drawMiddleSegments(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color
) {
    val stroke = Stroke(width = 3.5.dp.toPx())
    val numSegments = 6
    val segmentSweep = 360f / numSegments

    for (i in 0 until numSegments) {
        if (i % 2 == 0) {
            val startAngle = rotation + (i * segmentSweep)
            drawArc(
                color = color.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = segmentSweep * 0.65f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = stroke
            )
        }
    }
}

private fun DrawScope.drawOrbitingNodes(
    center: Offset,
    radius: Float,
    rotation: Float,
    primary: Color,
    secondary: Color
) {
    val nodeCount = 3
    for (i in 0 until nodeCount) {
        val angleDeg = rotation + (i * (360f / nodeCount))
        val rad = Math.toRadians(angleDeg.toDouble())
        val nodePos = Offset(
            center.x + (radius * cos(rad)).toFloat(),
            center.y + (radius * sin(rad)).toFloat()
        )

        // Outer glow
        drawCircle(
            color = primary.copy(alpha = 0.35f),
            radius = 6.dp.toPx(),
            center = nodePos
        )
        // Solid center
        drawCircle(
            color = secondary,
            radius = 3.dp.toPx(),
            center = nodePos
        )
    }
}

private fun DrawScope.drawInnerCore(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color
) {
    // Glowing central core gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.9f),
                secondary.copy(alpha = 0.5f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = center
    )

    // Inner bright diamond / circle
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = radius * 0.35f,
        center = center
    )

    // Core border
    drawCircle(
        color = primary,
        radius = radius * 0.75f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
fun AudioWaveform(
    isSpeaking: Boolean,
    isListening: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(44.dp)) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val numBars = 36
        val barWidth = (width / numBars) * 0.6f
        val spacing = width / numBars

        val active = isSpeaking || isListening
        val baseAmp = if (active) (audioRms.coerceIn(1f, 15f) / 15f) else 0.15f

        for (i in 0 until numBars) {
            val progress = i.toFloat() / numBars
            val sineVal = sin(progress * Math.PI * 3f + waveAnim)
            val barHeight = if (active) {
                ((midY * 0.85f * baseAmp * (0.4f + 0.6f * sineVal * sineVal).toFloat()) + 4.dp.toPx()).coerceAtLeast(3.dp.toPx())
            } else {
                3.dp.toPx()
            }

            val x = i * spacing + (spacing / 2f)
            val color = if (active) {
                if (isListening) CyanPrimary else CyanSecondary
            } else {
                TextMuted.copy(alpha = 0.3f)
            }

            drawLine(
                color = color,
                start = Offset(x, midY - barHeight),
                end = Offset(x, midY + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
