package com.example.scene3d

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Point3D(val x: Float, val y: Float, val z: Float)

@Composable
fun Character3DView(
    state: CharacterState,
    audioAmplitude: Float,
    modelInfo: GlbModelInfo?,
    activeAnimation: String,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {}
) {
    var rotationY by remember { mutableFloatStateOf(0f) }
    var rotationX by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "3d_character_loop")

    // Breathing & idle natural oscillation
    val breathProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Holographic thinking/orbit particle rotation
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Dynamic wave pulse for speaking
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationY = (rotationY + dragAmount.x * 0.5f) % 360f
                    rotationX = (rotationX - dragAmount.y * 0.3f).coerceIn(-45f, 45f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        rotationY = 0f
                        rotationX = 0f
                    },
                    onTap = {
                        onAvatarClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = size.width.coerceAtMost(size.height) * 0.28f

            // 1. Draw glowing backdrop aura based on state
            drawAuraBackdrop(cx, cy, baseRadius, state, breathProgress, audioAmplitude, orbitAngle)

            // 2. Render 3D geometric / holographic avatar mesh with 3D projection
            val effectiveJawOffset = if (state == CharacterState.SPEAKING) {
                (audioAmplitude * 24f * wavePulse).coerceIn(4f, 32f)
            } else 0f

            val attentivePitch = if (state == CharacterState.LISTENING) -8f else 0f
            val totalRotX = rotationX + attentivePitch
            val totalRotY = rotationY + (sin(orbitAngle * PI / 180f) * if (state == CharacterState.THINKING) 15f else 3f).toFloat()

            render3DAvatar(
                cx = cx,
                cy = cy + (breathProgress * 12f) - 6f,
                scale = baseRadius,
                rotX = totalRotX,
                rotY = totalRotY,
                state = state,
                jawOffset = effectiveJawOffset,
                amplitude = audioAmplitude,
                hasCustomModel = modelInfo != null
            )

            // 3. Render state-specific orbiting holographic rings
            renderHolographicRings(cx, cy, baseRadius, state, orbitAngle, breathProgress)
        }

        // Active State Badge & Animation Status
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xCC0A0E1A)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0x3300F2FE),
                                Color(0x337F00FF)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                val modelLabel = if (modelInfo != null) {
                    "MODEL: ${modelInfo.fileName.take(14)} • $activeAnimation"
                } else {
                    "MYRAA 3D • $activeAnimation"
                }
                Text(
                    text = modelLabel,
                    color = Color(0xFF00F2FE),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private fun DrawScope.drawAuraBackdrop(
    cx: Float,
    cy: Float,
    radius: Float,
    state: CharacterState,
    breath: Float,
    amplitude: Float,
    orbit: Float
) {
    val auraColor = when (state) {
        CharacterState.IDLE -> Color(0xFF00F2FE)
        CharacterState.LISTENING -> Color(0xFF38BDF8)
        CharacterState.THINKING -> Color(0xFFA855F7)
        CharacterState.SPEAKING -> Color(0xFF00F2FE)
        CharacterState.PROCESSING -> Color(0xFF7F00FF)
        CharacterState.SUCCESS -> Color(0xFF10B981)
        CharacterState.ERROR -> Color(0xFFEF4444)
        CharacterState.OFFLINE -> Color(0xFF64748B)
    }

    val dynamicPulse = radius * (1.1f + breath * 0.15f + amplitude * 0.25f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                auraColor.copy(alpha = if (state == CharacterState.OFFLINE) 0.08f else 0.22f),
                auraColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = dynamicPulse * 1.5f
        ),
        radius = dynamicPulse * 1.5f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.render3DAvatar(
    cx: Float,
    cy: Float,
    scale: Float,
    rotX: Float,
    rotY: Float,
    state: CharacterState,
    jawOffset: Float,
    amplitude: Float,
    hasCustomModel: Boolean
) {
    val primaryColor = when (state) {
        CharacterState.IDLE -> Color(0xFF00F2FE)
        CharacterState.LISTENING -> Color(0xFF38BDF8)
        CharacterState.THINKING -> Color(0xFFA855F7)
        CharacterState.SPEAKING -> Color(0xFF00F2FE)
        CharacterState.PROCESSING -> Color(0xFF818CF8)
        CharacterState.SUCCESS -> Color(0xFF34D399)
        CharacterState.ERROR -> Color(0xFFF87171)
        CharacterState.OFFLINE -> Color(0xFF94A3B8)
    }

    val secondaryColor = if (state == CharacterState.OFFLINE) Color(0xFF475569) else Color(0xFF7F00FF)

    // Polyhedral 3D facial/head mesh node coordinates (normalized -1..1)
    val headNodes = listOf(
        Point3D(0f, -0.9f, 0f),       // Crown top
        Point3D(-0.45f, -0.6f, 0.4f), // Left temple
        Point3D(0.45f, -0.6f, 0.4f),  // Right temple
        Point3D(-0.55f, 0.0f, 0.5f),  // Left cheek
        Point3D(0.55f, 0.0f, 0.5f),   // Right cheek
        Point3D(-0.35f, 0.5f + (jawOffset / scale), 0.4f), // Left jaw
        Point3D(0.35f, 0.5f + (jawOffset / scale), 0.4f),  // Right jaw
        Point3D(0f, 0.75f + (jawOffset / scale), 0.35f),   // Chin
        Point3D(0f, -0.1f, 0.75f),    // Nose / Core apex
        Point3D(-0.25f, -0.2f, 0.6f), // Left eye node
        Point3D(0.25f, -0.2f, 0.6f),  // Right eye node
        Point3D(0f, 0.3f + (jawOffset * 0.5f / scale), 0.6f), // Viseme mouth node
        Point3D(0f, -0.6f, -0.5f),    // Rear skull top
        Point3D(-0.5f, 0.1f, -0.4f),  // Rear skull left
        Point3D(0.5f, 0.1f, -0.4f),   // Rear skull right
        Point3D(0f, 0.6f, -0.3f)      // Rear skull base
    )

    // Project 3D nodes to 2D screen coordinates
    val radX = (rotX * PI / 180.0).toFloat()
    val radY = (rotY * PI / 180.0).toFloat()

    val projectedNodes = headNodes.map { node ->
        // Y-axis rotation
        val x1 = node.x * cos(radY) + node.z * sin(radY)
        val z1 = -node.x * sin(radY) + node.z * cos(radY)

        // X-axis rotation
        val y2 = node.y * cos(radX) - z1 * sin(radX)
        val z2 = node.y * sin(radX) + z1 * cos(radX)

        // Perspective projection
        val fov = 3.2f
        val distance = 3.5f + z2
        val projX = cx + (x1 * scale / distance) * 2.2f
        val projY = cy + (y2 * scale / distance) * 2.2f

        Triple(Offset(projX, projY), z2, node)
    }

    // Connect nodes into 3D facial facets
    val edges = listOf(
        Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4),
        Pair(3, 5), Pair(4, 6), Pair(5, 7), Pair(6, 7),
        Pair(1, 8), Pair(2, 8), Pair(3, 8), Pair(4, 8),
        Pair(8, 11), Pair(11, 7), Pair(9, 10),
        Pair(1, 9), Pair(2, 10), Pair(3, 9), Pair(4, 10),
        Pair(0, 12), Pair(12, 13), Pair(12, 14), Pair(13, 15), Pair(14, 15)
    )

    // Draw connecting polygon wireframe edges with depth shading
    for ((startIndex, endIndex) in edges) {
        val start = projectedNodes[startIndex]
        val end = projectedNodes[endIndex]

        val avgZ = (start.second + end.second) / 2f
        val alpha = if (state == CharacterState.OFFLINE) 0.35f else ((avgZ + 1.2f) / 2.4f).coerceIn(0.2f, 0.95f)

        val strokeWidth = if (avgZ > 0) 2.2f else 1.2f

        drawLine(
            color = if (avgZ > 0) primaryColor.copy(alpha = alpha) else secondaryColor.copy(alpha = alpha * 0.7f),
            start = start.first,
            end = end.first,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    // Draw eye nodes with blinking/reactive shimmer
    val leftEye = projectedNodes[9]
    val rightEye = projectedNodes[10]

    val eyeGlowRadius = 6.dp.toPx() * (1f + amplitude * 0.5f)
    drawCircle(
        color = if (state == CharacterState.ERROR) Color(0xFFEF4444) else Color.White,
        radius = eyeGlowRadius * 0.7f,
        center = leftEye.first
    )
    drawCircle(
        color = if (state == CharacterState.ERROR) Color(0xFFEF4444) else Color.White,
        radius = eyeGlowRadius * 0.7f,
        center = rightEye.first
    )

    // Viseme / Mouth node dynamic voice pulse
    val mouthNode = projectedNodes[11]
    if (jawOffset > 1f || state == CharacterState.SPEAKING) {
        drawCircle(
            color = primaryColor,
            radius = (4f + jawOffset * 0.3f),
            center = mouthNode.first
        )
    }
}

private fun DrawScope.renderHolographicRings(
    cx: Float,
    cy: Float,
    radius: Float,
    state: CharacterState,
    orbitAngle: Float,
    breath: Float
) {
    if (state == CharacterState.OFFLINE) return

    val ringColor = when (state) {
        CharacterState.THINKING -> Color(0xFFA855F7)
        CharacterState.SUCCESS -> Color(0xFF10B981)
        CharacterState.ERROR -> Color(0xFFEF4444)
        else -> Color(0xFF00F2FE)
    }

    val speedMult = when (state) {
        CharacterState.THINKING -> 2.5f
        CharacterState.PROCESSING -> 3.0f
        else -> 1.0f
    }

    // Outer gyroscopic orbital ring
    val currentAngleRad = (orbitAngle * speedMult * PI / 180.0).toFloat()
    val ringWidth = radius * 1.35f
    val ringHeight = radius * 0.42f

    val path = Path()
    var first = true
    for (i in 0..60) {
        val theta = (i * 6.0 * PI / 180.0).toFloat()
        // Tilt the ellipse in 3D
        val x0 = ringWidth * cos(theta)
        val y0 = ringHeight * sin(theta)

        // Rotate by currentAngleRad
        val rx = cx + (x0 * cos(currentAngleRad * 0.4f) - y0 * sin(currentAngleRad * 0.4f))
        val ry = cy + (x0 * sin(currentAngleRad * 0.4f) + y0 * cos(currentAngleRad * 0.4f))

        if (first) {
            path.moveTo(rx, ry)
            first = false
        } else {
            path.lineTo(rx, ry)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = ringColor.copy(alpha = if (state == CharacterState.THINKING) 0.7f else 0.35f),
        style = Stroke(
            width = if (state == CharacterState.THINKING) 2.5f else 1.2f,
            cap = StrokeCap.Round
        )
    )

    // Orbital spark particle
    val sparkX = cx + ringWidth * cos(currentAngleRad)
    val sparkY = cy + ringHeight * sin(currentAngleRad)
    drawCircle(
        color = Color.White,
        radius = 4.dp.toPx(),
        center = Offset(sparkX, sparkY)
    )
}
