package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitude: Float, // 0.0 to 1.0
    primaryColor: Color,
    secondaryColor: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barCount: Int = 32
) {
    val transition = rememberInfiniteTransition(label = "WaveformPhase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val barSpacing = width / barCount
        val barWidth = barSpacing * 0.55f

        val gradient = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor)
        )

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            // Bell curve dampening towards edges
            val bellCurve = sin(normalizedX * Math.PI).toFloat()

            // Calculate animated height
            val wave1 = sin((normalizedX * 12f) + phase).toFloat()
            val wave2 = sin((normalizedX * 6f) - phase * 1.5f).toFloat()
            val combinedWave = ((wave1 + wave2) * 0.5f + 1f) * 0.5f

            val minHeightRatio = if (isActive) 0.15f else 0.06f
            val dynamicScale = if (isActive) {
                (amplitude * 0.75f + 0.25f)
            } else {
                0.1f
            }

            val finalHeightRatio = (minHeightRatio + combinedWave * dynamicScale * 0.85f * bellCurve).coerceIn(0.04f, 1f)
            val barH = canvasHeight * finalHeightRatio
            val xPos = i * barSpacing + (barSpacing - barWidth) / 2f
            val yPos = (canvasHeight - barH) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(xPos, yPos),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = if (isActive) 0.9f else 0.35f
            )
        }
    }
}
