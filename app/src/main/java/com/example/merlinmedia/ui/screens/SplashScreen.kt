package com.example.merlinmedia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

// ---------------------------------------------------------------------------
// Wizard-flavoured loading messages
// ---------------------------------------------------------------------------
private val LOADING_MESSAGES = listOf(
    "Summoning the channels of the realm...",
    "Casting the playlist incantation...",
    "Consulting the iptv-org grimoire...",
    "Weaving the broadcast streams...",
    "Aligning the satellite constellations...",
    "Enchanting the signal crystals...",
    "Conjuring live feeds from the ether...",
    "Deciphering the M3U scrolls..."
)

// ---------------------------------------------------------------------------
// Particle data — stable randomised values computed once
// ---------------------------------------------------------------------------
private data class SparkParticle(
    val baseAngleDeg: Float,
    val radius: Float,
    val size: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float
)

private val PARTICLES: List<SparkParticle> = run {
    val rng = java.util.Random(0xDEAD_BEEF)
    (0 until 18).map {
        SparkParticle(
            baseAngleDeg  = rng.nextFloat() * 360f,
            radius        = 160f + rng.nextFloat() * 140f,
            size          = 3f + rng.nextFloat() * 7f,
            speedMultiplier = 0.4f + rng.nextFloat() * 0.6f,
            phaseOffset   = rng.nextFloat() * (2f * PI.toFloat())
        )
    }
}

// ---------------------------------------------------------------------------
// Main composable
// ---------------------------------------------------------------------------
@Composable
fun SplashScreen() {
    // ── infinite animation clock ────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "splashInf")

    // Global clock 0→1 over 6 seconds
    val clock by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clock"
    )

    // Orb pulse 0.85→1.15
    val orbPulse by inf.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    // Runic ring rotation 0→360°
    val ringRotation by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRot"
    )

    // Orbiting dot 0→360°
    val orbitAngle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    // Wand ray blink 0→1→0 (4 pairs of rays staggered)
    val rayAlpha by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rayAlpha"
    )

    // ── loading message cycling ─────────────────────────────────────────────
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            messageIndex = (messageIndex + 1) % LOADING_MESSAGES.size
        }
    }

    // ── progress dot animation 0/1/2 ───────────────────────────────────────
    var dotIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotIndex = (dotIndex + 1) % 3
        }
    }

    // ── title fade-in ───────────────────────────────────────────────────────
    var titleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(400); titleVisible = true }

    // ── layout ──────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.Center
    ) {
        // Full-screen Canvas for all particle/geometry drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawParticles(clock, cx, cy)
            drawGlowOrb(orbPulse, cx, cy)
            drawRunicRing(ringRotation, cx, cy)
            drawOrbitDot(orbitAngle, cx, cy)
            drawWandRays(rayAlpha, cx, cy)
        }

        // Central UI column layered on top of the canvas
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Spacer to push content to upper-half-centre
            Spacer(modifier = Modifier.weight(0.35f))

            // ── Merlin Logo Badge ──────────────────────────────────────────
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(700))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Glowing "M" wand icon (text-based, no asset needed)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF1A3060), Color(0xFF0C0E14))
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🧙",
                            fontSize = 46.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "MERLIN TV",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your magical streaming portal",
                        fontSize = 14.sp,
                        color = AccentSky,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // ── Loading message + progress dots ───────────────────────────
            AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(animationSpec = tween(1000))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Text(
                        text = LOADING_MESSAGES[messageIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp
                    )

                    // Animated progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == dotIndex) 10.dp else 7.dp)
                                    .background(
                                        color = if (i == dotIndex) AccentSky else TextMuted,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas drawing helpers
// ---------------------------------------------------------------------------

private fun DrawScope.drawParticles(clock: Float, cx: Float, cy: Float) {
    val t = clock * 2f * PI.toFloat()
    for (p in PARTICLES) {
        val angle = Math.toRadians(p.baseAngleDeg.toDouble()).toFloat() +
                (t * p.speedMultiplier) + p.phaseOffset
        val wobble = sin(t * 1.7f + p.phaseOffset) * 20f
        val r = p.radius + wobble
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r * 0.55f  // elliptical — feels more TV-like
        val alpha = (0.35f + 0.65f * ((sin(t * 1.3f + p.phaseOffset) + 1f) / 2f))

        // Outer glow
        drawCircle(
            color = AccentSky.copy(alpha = alpha * 0.25f),
            radius = p.size * 2.2f,
            center = Offset(x, y)
        )
        // Core
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = p.size * 0.6f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawGlowOrb(pulse: Float, cx: Float, cy: Float) {
    val baseRadius = 90f * pulse

    // Outermost soft halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                PrimaryBlue.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = baseRadius * 2.6f
        ),
        radius = baseRadius * 2.6f,
        center = Offset(cx, cy)
    )

    // Mid glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentSky.copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = baseRadius * 1.8f
        ),
        radius = baseRadius * 1.8f,
        center = Offset(cx, cy)
    )

    // Inner core glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                AccentSky.copy(alpha = 0.55f),
                PrimaryBlue.copy(alpha = 0.3f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = baseRadius
        ),
        radius = baseRadius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawRunicRing(rotation: Float, cx: Float, cy: Float) {
    val ringRadius = 120f

    rotate(degrees = rotation, pivot = Offset(cx, cy)) {
        // Main ring outline
        drawCircle(
            color = FocusRingColor.copy(alpha = 0.35f),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // 12 tick marks evenly spaced (rune notches)
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0).toFloat()
            val innerR = if (i % 3 == 0) ringRadius - 14f else ringRadius - 7f
            val alpha = if (i % 3 == 0) 0.85f else 0.45f
            drawLine(
                color = AccentSky.copy(alpha = alpha),
                start = Offset(cx + cos(a) * innerR, cy + sin(a) * innerR),
                end   = Offset(cx + cos(a) * ringRadius, cy + sin(a) * ringRadius),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f,
                cap = StrokeCap.Round
            )
        }
    }

    // Outer dashed accent ring (counter-rotation)
    rotate(degrees = -rotation * 0.4f, pivot = Offset(cx, cy)) {
        for (i in 0 until 24) {
            val a = Math.toRadians(i * 15.0).toFloat()
            val r = 142f
            if (i % 2 == 0) {
                drawLine(
                    color = PrimaryBlue.copy(alpha = 0.3f),
                    start = Offset(cx + cos(a) * (r - 6f), cy + sin(a) * (r - 6f)),
                    end   = Offset(cx + cos(a) * r,         cy + sin(a) * r),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawOrbitDot(orbitAngle: Float, cx: Float, cy: Float) {
    val a = Math.toRadians(orbitAngle.toDouble()).toFloat()
    val r = 120f
    val dotX = cx + cos(a) * r
    val dotY = cy + sin(a) * r

    // Trailing glow
    drawCircle(
        color = AccentSky.copy(alpha = 0.3f),
        radius = 14f,
        center = Offset(dotX, dotY)
    )
    // Bright core
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = Offset(dotX, dotY)
    )
    // Tiny inner highlight
    drawCircle(
        color = AccentSky,
        radius = 3f,
        center = Offset(dotX, dotY)
    )
}

private fun DrawScope.drawWandRays(rayAlpha: Float, cx: Float, cy: Float) {
    val numRays = 8
    val innerR = 18f
    val outerR = 60f

    for (i in 0 until numRays) {
        val a = Math.toRadians(i * (360.0 / numRays)).toFloat()
        // Even/odd rays alternate opacity
        val alpha = if (i % 2 == 0) rayAlpha * 0.6f else (1f - rayAlpha) * 0.6f
        drawLine(
            color = AccentSky.copy(alpha = alpha),
            start = Offset(cx + cos(a) * innerR, cy + sin(a) * innerR),
            end   = Offset(cx + cos(a) * outerR, cy + sin(a) * outerR),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}
