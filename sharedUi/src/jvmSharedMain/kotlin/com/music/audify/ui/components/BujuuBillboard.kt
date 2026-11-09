// Copyright (c) 2026 Sachit. All rights reserved.
// Project: AUDIFY
package com.music.audify.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.audify.ui.player.PlayerSettings
import com.music.audify.ui.rememberIsForeground
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Slide metadata for the BUJUU cinematic streaming billboard.
 */
private data class BujuuSlide(
    val tag: String,
    val title: String,
    val subtitle: String,
    val cta: String,
)

private val BujuuSlides = listOf(
    BujuuSlide(
        tag = "BUJUU • STREAMING",
        title = "Meet BUJUU.",
        subtitle = "Your entertainment starts here.",
        cta = "EXPLORE BUJUU →",
    ),
    BujuuSlide(
        tag = "BUJUU • MOVIES",
        title = "Your next movie awaits.",
        subtitle = "Discover movies worth watching.",
        cta = "DISCOVER MORE →",
    ),
    BujuuSlide(
        tag = "BUJUU • SERIES",
        title = "One episode is never enough.",
        subtitle = "Find your next favourite series.",
        cta = "WATCH WITH BUJUU →",
    ),
)

private const val SLIDE_DURATION_MS = 5_500L
private const val BUJUU_DESTINATION_URL = "https://buuju.lovable.app"

/**
 * BUJUU Cinematic Video Billboard.
 *
 * Designed with Apple TV / Netflix cinematic preview aesthetics:
 * - 170dp height, 18dp rounded corners
 * - Dark contrast scrim (#0D0F18 gradient at bottom and left)
 * - Subtle 2.5% film-grain texture overlay
 * - Lightweight looping ambient cinemagraph visual motion (drifting mist, rain reflections, anamorphic camera push)
 * - 100% silent with volume = 0f and zero audio focus footprint (never interrupts playback)
 * - Slim story-style 3-segment progress scrub bar along bottom edge
 * - Horizontal swipe for manual slide navigation
 * - Auto-pauses on touch or when backgrounded
 * - Respects Android Reduce Motion setting
 * - Launches https://buuju.lovable.app on tap
 */
@Composable
fun BujuuBillboard(
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val isForeground = rememberIsForeground()

    var currentSlide by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "bujuuCardScale",
    )

    // Progress and slide rotation timer:
    // Automatically pauses when touched (pressed/dragged) or when backgrounded.
    LaunchedEffect(currentSlide, isPressed, isDragging, isForeground) {
        if (!isForeground || isPressed || isDragging) return@LaunchedEffect

        val tickInterval = 32L
        while (isActive) {
            delay(tickInterval)
            if (!isForeground || isPressed || isDragging) break
            val increment = tickInterval.toFloat() / SLIDE_DURATION_MS.toFloat()
            val newProgress = slideProgress + increment
            if (newProgress >= 1f) {
                slideProgress = 0f
                currentSlide = (currentSlide + 1) % BujuuSlides.size
                break
            } else {
                slideProgress = newProgress
            }
        }
    }

    val slide = BujuuSlides[currentSlide]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = PAGE_GUTTER)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    runCatching { uriHandler.openUri(BUJUU_DESTINATION_URL) }
                },
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                val dragThreshold = 45.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        totalDrag = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        totalDrag = 0f
                    },
                    onDragEnd = {
                        if (totalDrag <= -dragThreshold) {
                            // Swiped left: advance to next slide
                            currentSlide = (currentSlide + 1) % BujuuSlides.size
                            slideProgress = 0f
                        } else if (totalDrag >= dragThreshold) {
                            // Swiped right: return to previous slide
                            currentSlide = (currentSlide + BujuuSlides.size - 1) % BujuuSlides.size
                            slideProgress = 0f
                        }
                        isDragging = false
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                )
            },
    ) {
        // --- 1. Cinematic Ambient Cinemagraph Visual Motion ---
        CinematicMotionBackdrop(
            slideIndex = currentSlide,
            reduceMotion = reduceAnimation,
            isAnimating = isForeground && !isPressed,
            modifier = Modifier.fillMaxSize(),
        )

        // --- 2. 2.5% Film Grain Texture Layer ---
        FilmGrainOverlay(
            modifier = Modifier.fillMaxSize(),
        )

        // --- 3. Dark Contrast Scrim (#0D0F18 gradient at bottom and left) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xF20D0F18),
                        0.48f to Color(0xCC0D0F18),
                        0.82f to Color(0x550D0F18),
                        1.0f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.40f to Color(0x330D0F18),
                        0.75f to Color(0xBB0D0F18),
                        1.0f to Color(0xF80D0F18),
                    ),
                ),
        )

        // --- 4. Rotating Slide Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                // Category Tag
                AnimatedContent(
                    targetState = slide.tag,
                    transitionSpec = {
                        if (reduceAnimation) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                        }
                    },
                    label = "bujuuTagAnim",
                ) { tagText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = tagText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFFF1F5F9),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Title
                AnimatedContent(
                    targetState = slide.title,
                    transitionSpec = {
                        if (reduceAnimation) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(400)) togetherWith fadeOut(tween(250))
                        }
                    },
                    label = "bujuuTitleAnim",
                ) { titleText ->
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Subtitle
                AnimatedContent(
                    targetState = slide.subtitle,
                    transitionSpec = {
                        if (reduceAnimation) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(400, delayMillis = 60)) togetherWith fadeOut(tween(200))
                        }
                    },
                    label = "bujuuSubtitleAnim",
                ) { subText ->
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFCAD0E2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom CTA and Scrub Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AnimatedContent(
                        targetState = slide.cta,
                        transitionSpec = {
                            if (reduceAnimation) {
                                fadeIn(snap()) togetherWith fadeOut(snap())
                            } else {
                                fadeIn(tween(350)) togetherWith fadeOut(tween(200))
                            }
                        },
                        label = "bujuuCtaAnim",
                    ) { ctaText ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = ctaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Color.White,
                            )
                        }
                    }

                    // BUJUU Watermark / Brand Indicator
                    Text(
                        text = "BUJUU",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // --- 5. Story-Style 3-Segment Progress Scrub Bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (i in BujuuSlides.indices) {
                        val segmentProgress = when {
                            i < currentSlide -> 1f
                            i == currentSlide -> slideProgress.coerceIn(0f, 1f)
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                        ) {
                            if (segmentProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(segmentProgress)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color.White.copy(alpha = 0.92f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2.5% Film Grain Overlay.
 * Subtle 35mm film grain texture rendered cleanly over the card.
 */
@Composable
private fun FilmGrainOverlay(
    modifier: Modifier = Modifier,
) {
    // 120 deterministic micro grain points
    val grainPoints = remember {
        val random = Random(42)
        FloatArray(240) { random.nextFloat() }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val grainColorLight = Color.White.copy(alpha = 0.025f)
        val grainColorDark = Color.Black.copy(alpha = 0.025f)

        for (i in 0 until grainPoints.size step 2) {
            val px = grainPoints[i] * w
            val py = grainPoints[i + 1] * h
            val color = if (i % 4 == 0) grainColorLight else grainColorDark
            drawCircle(
                color = color,
                radius = 1.2f,
                center = Offset(px, py),
            )
        }
    }
}

/**
 * Multi-layer cinematic background cinemagraph.
 * Silent, uses 0 audio focus, smooth 60fps ambient drift.
 */
@Composable
private fun CinematicMotionBackdrop(
    slideIndex: Int,
    reduceMotion: Boolean,
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bujuuMotion")

    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (!reduceMotion && isAnimating) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bujuuLoopTime",
    )

    val cameraScale = if (reduceMotion) 1f else 1f + 0.045f * sin(animTime * 2f * Math.PI.toFloat())

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                scaleX = cameraScale
                scaleY = cameraScale
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val phase = animTime * 2f * Math.PI.toFloat()

            when (slideIndex) {
                0 -> {
                    // Slide 1: Deep cosmic streaming obsidian with drifting luminous mist
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF18153A), Color(0xFF0F1023), Color(0xFF07080E)),
                            center = Offset(w * 0.75f + cos(phase) * 30f, h * 0.35f + sin(phase) * 20f),
                            radius = w * 0.85f,
                        ),
                    )
                    // Ambient drifting mist cloud 1 (Teal/Cyan)
                    val mistX1 = w * (0.65f + 0.12f * sin(phase))
                    val mistY1 = h * (0.50f + 0.10f * cos(phase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3800E5FF), Color(0x1800B0FF), Color.Transparent),
                            center = Offset(mistX1, mistY1),
                            radius = w * 0.42f,
                        ),
                        radius = w * 0.42f,
                        center = Offset(mistX1, mistY1),
                    )
                    // Ambient drifting mist cloud 2 (Electric Violet)
                    val mistX2 = w * (0.85f - 0.10f * cos(phase))
                    val mistY2 = h * (0.25f + 0.15f * sin(phase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x448A2BE2), Color(0x1A6A0DAD), Color.Transparent),
                            center = Offset(mistX2, mistY2),
                            radius = w * 0.45f,
                        ),
                        radius = w * 0.45f,
                        center = Offset(mistX2, mistY2),
                    )
                    // Anamorphic horizontal light streak
                    val streakY = h * (0.35f + 0.05f * sin(phase))
                    drawLine(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.45f to Color(0x2800E5FF),
                            0.75f to Color(0x608A2BE2),
                            1f to Color.Transparent,
                        ),
                        start = Offset(0f, streakY),
                        end = Offset(w, streakY),
                        strokeWidth = 3f,
                    )
                }
                1 -> {
                    // Slide 2: Cinematic noir rain reflections & amber anamorphic lens glow
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0C1B24), Color(0xFF071118), Color(0xFF040A0F)),
                            center = Offset(w * 0.65f, h * 0.5f),
                            radius = w * 0.9f,
                        ),
                    )
                    // Warm amber cinema light flare on the right
                    val flareX = w * (0.88f + 0.05f * cos(phase))
                    val flareY = h * (0.30f + 0.08f * sin(phase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x55FF9E0B), Color(0x22F59E0B), Color.Transparent),
                            center = Offset(flareX, flareY),
                            radius = w * 0.48f,
                        ),
                        radius = w * 0.48f,
                        center = Offset(flareX, flareY),
                    )
                    // Vertical rain reflections / glistening streaks
                    val rainColumns = 7
                    for (c in 0 until rainColumns) {
                        val rx = w * (0.50f + c * 0.07f)
                        val rPhase = phase + c * 0.9f
                        val ryTop = h * (0.2f + 0.1f * sin(rPhase))
                        val ryBottom = h * (0.85f + 0.08f * cos(rPhase))
                        val alpha = (0.08f + 0.06f * sin(rPhase)).coerceIn(0.02f, 0.18f)
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = alpha),
                            start = Offset(rx, ryTop),
                            end = Offset(rx, ryBottom),
                            strokeWidth = 2.2f,
                        )
                    }
                }
                2 -> {
                    // Slide 3: Midnight neon emerald & indigo cinematic series aura
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF140C28), Color(0xFF0C071C), Color(0xFF060310)),
                            center = Offset(w * 0.70f, h * 0.40f),
                            radius = w * 0.85f,
                        ),
                    )
                    // Sweeping electric emerald aura
                    val auroraX = w * (0.72f + 0.08f * sin(phase))
                    val auroraY = h * (0.65f - 0.10f * cos(phase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x4010B981), Color(0x18059669), Color.Transparent),
                            center = Offset(auroraX, auroraY),
                            radius = w * 0.46f,
                        ),
                        radius = w * 0.46f,
                        center = Offset(auroraX, auroraY),
                    )
                    // Sweeping neon indigo flare
                    val auroraX2 = w * (0.85f - 0.06f * cos(phase))
                    val auroraY2 = h * (0.30f + 0.10f * sin(phase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x486366F1), Color(0x1E4F46E5), Color.Transparent),
                            center = Offset(auroraX2, auroraY2),
                            radius = w * 0.42f,
                        ),
                        radius = w * 0.42f,
                        center = Offset(auroraX2, auroraY2),
                    )
                }
            }
        }
    }
}
