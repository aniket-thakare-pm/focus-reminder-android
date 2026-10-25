package com.example.focusreminder.ui.components

import android.content.Context
import android.graphics.BlurMaskFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import kotlinx.coroutines.launch

// ============================================================
// Neumorphic (Soft UI) Components & Surfaces
// Specs: border-radius 14-16dp, box-shadow -5px -5px 15px, 5px 5px 15px
// Solid #E0E5EC base surface (NO random gradients)
// ============================================================

val NeuBevelBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x90FFFFFF), // Top-left light reflection
        Color(0x209BAAC0)  // Bottom-right subtle edge
    )
)

val NeuInsetBevelBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x22708096), // Subtle soft dark crease at top-left edge
        Color(0x06708096),
        Color.Transparent  // Bottom-right is completely transparent (ZERO white border)
    ),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

/**
 * Extruded Neumorphic Card with clean dual-directional shadows
 */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 5.dp,
    isInset: Boolean = false,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val shadowModifier = modifier.drawBehind {
        val r = cornerRadius.toPx()
        val blurPx = (elevation.toPx() * 2.5f).coerceAtLeast(1f)
        val offsetPx = elevation.toPx()

        drawIntoCanvas { canvas ->
            if (!isInset) {
                // Top-left white highlight shadow (-5px -5px 15px #FFFFFF)
                val paintLight = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.White.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawRoundRect(
                    -offsetPx, -offsetPx, size.width - offsetPx, size.height - offsetPx,
                    r, r, paintLight
                )

                // Bottom-right dark shadow (5px 5px 15px #9BAAC0)
                val paintDark = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color(0x667D8FA6).toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawRoundRect(
                    offsetPx, offsetPx, size.width + offsetPx, size.height + offsetPx,
                    r, r, paintDark
                )
            }
        }
    }

    var baseModifier = shadowModifier
        .clip(shape)
        .background(
            if (isInset) Color(0xFFDCE2E9) else (backgroundColor ?: NeuSurface)
        )
        .border(
            width = 0.8.dp,
            brush = borderColor?.let { Brush.linearGradient(listOf(it, it)) } ?: (if (isInset) NeuInsetBevelBorder else NeuBevelBorder),
            shape = shape
        )

    if (onClick != null) {
        baseModifier = baseModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Tactile Neumorphic Pill / Button (border-radius: 14-16dp)
 */
@Composable
fun NeuPill(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    NeuCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = elevation,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        onClick = onClick,
        content = content
    )
}

/**
 * Sunken Debossed Inset Well with natural directional lighting.
 * Light source: Fixed at Top-Left.
 * - Soft inner shadows along Top and Left edges.
 * - Clean porcelain tone (never dirty gray).
 * - ZERO stark white borders or highlights on bottom/right (eliminates fake embossing).
 */
@Composable
fun NeuInsetWell(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    shadowDepth: Dp = 3.5.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseBg = backgroundColor ?: Color(0xFFDCE2E9)

    var boxModifier = modifier
        .clip(shape)
        .background(baseBg)
        .drawBehind {
            val w = size.width
            val h = size.height
            // Constrain shadow depth so smaller pills (like metrics or lock icon) retain clean porcelain interiors
            val depth = shadowDepth.toPx().coerceAtMost(h / 3.5f).coerceAtMost(w / 3.5f)

            // 1. Top inner shadow (light from top-left blocked by top rim -> soft shadow downward)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x2C708096),
                        Color(0x0E708096),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = depth
                ),
                size = size
            )

            // 2. Left inner shadow (light from top-left blocked by left rim -> soft shadow rightward)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x2C708096),
                        Color(0x0E708096),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = depth
                ),
                size = size
            )
        }
        .border(
            width = 0.8.dp,
            brush = borderColor?.let { Brush.linearGradient(listOf(it, it)) } ?: NeuInsetBevelBorder,
            shape = shape
        )

    if (onClick != null) {
        boxModifier = boxModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = boxModifier,
        content = content
    )
}

/**
 * Slide-to-Acknowledge Slider with Progressive Green Fill & Haptic Vibration Feedback
 */
@Composable
fun NeuSlideToAcknowledge(
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    label: String = "Slide to Acknowledge"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizeDp = 52.dp
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val trackPaddingPx = with(density) { 6.dp.toPx() }

    val maxDragPx = remember(trackWidthPx, thumbSizePx, trackPaddingPx) {
        (trackWidthPx - thumbSizePx - trackPaddingPx * 2).coerceAtLeast(1f)
    }

    val dragOffset = remember { Animatable(0f) }
    var isCompleted by remember { mutableStateOf(false) }

    fun triggerVibration() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    val progress = (dragOffset.value / maxDragPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
    ) {
        // 1. Sunken Inset Track
        NeuInsetWell(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 20.dp
        ) {
            // 2. Dynamic Progressive Green Fill Track
            val fillWidthFraction = if (trackWidthPx > 0f) {
                ((dragOffset.value + thumbSizePx + trackPaddingPx) / trackWidthPx).coerceIn(0f, 1f)
            } else 0f

            if (fillWidthFraction > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillWidthFraction)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF047857),
                                    Color(0xFF059669)
                                )
                            )
                        )
                )
            }

            // 3. Track Label (Centered)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = if (progress > 0.45f) Color.White.copy(alpha = 0.95f) else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Draggable Tactile Thumb (Knob)
        if (maxDragPx > 1f) {
            Box(
                modifier = Modifier
                    .padding(start = with(density) { (trackPaddingPx + dragOffset.value).toDp() })
                    .align(Alignment.CenterStart)
                    .size(thumbSizeDp)
                    .pointerInput(maxDragPx, isCompleted) {
                        if (isCompleted) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                                change.consume()
                                coroutineScope.launch {
                                    val target = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                    dragOffset.snapTo(target)
                                }
                            },
                            onDragEnd = {
                                if (dragOffset.value >= maxDragPx * 0.82f) {
                                    // Successfully completed slider
                                    isCompleted = true
                                    coroutineScope.launch {
                                        dragOffset.animateTo(maxDragPx, tween(150))
                                        triggerVibration()
                                        onAcknowledge()
                                    }
                                } else {
                                    // Snap back with spring
                                    coroutineScope.launch {
                                        dragOffset.animateTo(
                                            0f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, tween(150))
                                }
                            }
                        )
                    }
            ) {
                // Tactile Knob Surface
                NeuPill(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 16.dp,
                    elevation = 4.dp,
                    backgroundColor = if (progress > 0.8f) Color(0xFF047857) else EmeraldPrimary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        SlideChevronIcon(color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Tactile Double Chevron Icon for the Slider Knob
 */
@Composable
fun SlideChevronIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val path1 = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.25f)
            lineTo(size.width * 0.50f, size.height * 0.50f)
            lineTo(size.width * 0.25f, size.height * 0.75f)
        }
        val path2 = Path().apply {
            moveTo(size.width * 0.52f, size.height * 0.25f)
            lineTo(size.width * 0.77f, size.height * 0.50f)
            lineTo(size.width * 0.52f, size.height * 0.75f)
        }
        drawPath(path1, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path2, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Neumorphic Circular Dial / Button
 */
@Composable
fun NeuCircle(
    modifier: Modifier = Modifier,
    elevation: Dp = 5.dp,
    isInset: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shadowModifier = modifier.drawBehind {
        val blurPx = (elevation.toPx() * 2.5f).coerceAtLeast(1f)
        val offsetPx = elevation.toPx()

        if (isInset) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val shadowPx = 8.dp.toPx()

            // Directional Top-Left inner shadow (consistent light from top-left)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x2E708096),
                        Color(0x0C708096),
                        Color.Transparent
                    ),
                    start = androidx.compose.ui.geometry.Offset(cx - r, cy - r),
                    end = androidx.compose.ui.geometry.Offset(cx - r + shadowPx * 2f, cy - r + shadowPx * 2f)
                )
            )
        } else {
            drawIntoCanvas { canvas ->
                val paintLight = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.White.toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(
                    size.width / 2f - offsetPx, size.height / 2f - offsetPx,
                    size.minDimension / 2f, paintLight
                )

                val paintDark = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color(0x667D8FA6).toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.nativeCanvas.drawCircle(
                    size.width / 2f + offsetPx, size.height / 2f + offsetPx,
                    size.minDimension / 2f, paintDark
                )
            }
        }
    }

    var baseModifier = shadowModifier
        .clip(CircleShape)
        .background(if (isInset) Color(0xFFDCE2E9) else NeuSurface)
        .border(width = 0.8.dp, brush = if (isInset) NeuInsetBevelBorder else NeuBevelBorder, shape = CircleShape)

    if (onClick != null) {
        baseModifier = baseModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center,
        content = content
    )
}
