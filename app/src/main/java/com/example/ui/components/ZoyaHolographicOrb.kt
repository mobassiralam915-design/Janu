package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.persona.ZoyaMood
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun ZoyaHolographicOrb(
    state: AssistantVisualState,
    mood: ZoyaMood,
    audioAmplitude: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Idle breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Continuous rotation for holographic rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantVisualState.THINKING -> 1500
                    AssistantVisualState.LISTENING -> 4000
                    AssistantVisualState.SPEAKING -> 3000
                    AssistantVisualState.IDLE -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Shimmer wave
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerPhase"
    )

    // Pre-calculate particle angles
    val particleOffsets = remember {
        List(18) { index ->
            val angle = (index * 20f * (PI / 180f)).toFloat()
            val radiusFactor = 0.55f + (index % 4) * 0.12f
            Triple(angle, radiusFactor, (index % 3))
        }
    }

    val primaryColor = mood.primaryColor
    val secondaryColor = mood.secondaryColor

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension * 0.30f

            // Dynamic scale factor based on audio amplitude or state
            val amplitudeBoost = (audioAmplitude * 0.35f)
            val stateScale = when (state) {
                AssistantVisualState.IDLE -> idlePulse
                AssistantVisualState.LISTENING -> 1.05f + amplitudeBoost
                AssistantVisualState.THINKING -> 1.02f
                AssistantVisualState.SPEAKING -> 1.08f + amplitudeBoost * 1.2f
            }
            val activeRadius = baseRadius * stateScale

            // 1. Outermost Glowing Ambient Aura
            val auraColor = when (state) {
                AssistantVisualState.LISTENING -> secondaryColor.copy(alpha = 0.35f + audioAmplitude * 0.3f)
                AssistantVisualState.SPEAKING -> primaryColor.copy(alpha = 0.40f + audioAmplitude * 0.35f)
                AssistantVisualState.THINKING -> primaryColor.copy(alpha = 0.30f)
                AssistantVisualState.IDLE -> primaryColor.copy(alpha = 0.18f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraColor,
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = activeRadius * 1.8f
                ),
                radius = activeRadius * 1.8f,
                center = center
            )

            // 2. Ripple Rings for Speaking / Listening
            if (state == AssistantVisualState.SPEAKING || state == AssistantVisualState.LISTENING) {
                val rippleCount = 3
                for (i in 1..rippleCount) {
                    val rippleProgress = ((shimmerPhase / (2 * PI).toFloat() + i * 0.33f) % 1f)
                    val rippleRadius = activeRadius * (1.1f + rippleProgress * 0.55f)
                    val rippleAlpha = ((1f - rippleProgress) * 0.45f * (0.5f + audioAmplitude)).coerceIn(0f, 0.7f)
                    drawCircle(
                        color = if (i % 2 == 0) primaryColor.copy(alpha = rippleAlpha) else secondaryColor.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx() * (1f - rippleProgress * 0.5f))
                    )
                }
            }

            // 3. Rotating Holographic Arc Rings
            rotate(rotationAngle, pivot = center) {
                // Ring 1 (Cyan/Primary)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.8f),
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.8f),
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    radius = activeRadius * 1.25f,
                    center = center,
                    style = Stroke(
                        width = if (state == AssistantVisualState.THINKING) 3.5.dp.toPx() else 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Orbiting Segment Arcs
                drawArc(
                    color = primaryColor.copy(alpha = 0.9f),
                    startAngle = 15f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(activeRadius * 2.5f, activeRadius * 2.5f),
                    topLeft = Offset(center.x - activeRadius * 1.25f, center.y - activeRadius * 1.25f)
                )
                drawArc(
                    color = secondaryColor.copy(alpha = 0.9f),
                    startAngle = 195f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(activeRadius * 2.5f, activeRadius * 2.5f),
                    topLeft = Offset(center.x - activeRadius * 1.25f, center.y - activeRadius * 1.25f)
                )
            }

            // Reverse Rotating Inner Ring
            rotate(-rotationAngle * 1.35f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.6f),
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = activeRadius * 1.1f,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Center Holographic Core Orb
            val coreGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.85f),
                    secondaryColor.copy(alpha = 0.70f),
                    primaryColor.copy(alpha = 0.40f)
                ),
                center = Offset(center.x - activeRadius * 0.2f, center.y - activeRadius * 0.2f),
                radius = activeRadius
            )
            drawCircle(
                brush = coreGradient,
                radius = activeRadius,
                center = center
            )

            // 5. Floating Quantum Particles
            val particleRotationRad = (rotationAngle * (PI / 180f)).toFloat()
            for (p in particleOffsets) {
                val currentAngle = p.first + (if (p.third == 0) particleRotationRad else -particleRotationRad * 0.8f)
                val pRadius = activeRadius * (p.second + (audioAmplitude * 0.2f))
                val px = center.x + cos(currentAngle) * pRadius
                val py = center.y + sin(currentAngle) * pRadius
                val particleColor = when (p.third) {
                    0 -> primaryColor
                    1 -> secondaryColor
                    else -> Color.White
                }
                drawCircle(
                    color = particleColor.copy(alpha = 0.75f),
                    radius = (2.2.dp.toPx() + (p.third * 0.8.dp.toPx())),
                    center = Offset(px, py)
                )
            }

            // 6. Center Specular Shimmer Highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    center = Offset(center.x - activeRadius * 0.35f, center.y - activeRadius * 0.35f),
                    radius = activeRadius * 0.45f
                ),
                radius = activeRadius * 0.45f,
                center = Offset(center.x - activeRadius * 0.35f, center.y - activeRadius * 0.35f)
            )
        }
    }
}
