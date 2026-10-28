package com.example.focusreminder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Minimalist Vector Icons for the Bottom Navigation Bar (Zero Emojis).
 */

@Composable
fun NavTimerIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f + 1.5.dp.toPx()
        val radius = size.minDimension / 2.5f

        // Top stem & button
        drawLine(
            color = color,
            start = Offset(cx, cy - radius),
            end = Offset(cx, cy - radius - 3.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx - 3.dp.toPx(), cy - radius - 3.dp.toPx()),
            end = Offset(cx + 3.dp.toPx(), cy - radius - 3.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Watch circular ring
        drawCircle(
            color = color,
            center = Offset(cx, cy),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Watch hands (showing 10:10)
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx, cy - radius * 0.55f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx + radius * 0.45f, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun NavAppsIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val boxSize = size.width * 0.36f
        val gap = size.width * 0.14f
        val startX = (size.width - (boxSize * 2 + gap)) / 2f
        val startY = (size.height - (boxSize * 2 + gap)) / 2f
        val corner = 3.dp.toPx()

        val positions = listOf(
            Offset(startX, startY),
            Offset(startX + boxSize + gap, startY),
            Offset(startX, startY + boxSize + gap),
            Offset(startX + boxSize + gap, startY + boxSize + gap)
        )

        positions.forEach { pos ->
            drawRoundRect(
                color = color,
                topLeft = pos,
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun NavAlertsIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val cx = size.width / 2f
        val path = Path().apply {
            // Bell dome
            moveTo(cx, size.height * 0.16f)
            cubicTo(
                cx + size.width * 0.32f, size.height * 0.22f,
                cx + size.width * 0.36f, size.height * 0.58f,
                cx + size.width * 0.44f, size.height * 0.72f
            )
            lineTo(cx - size.width * 0.44f, size.height * 0.72f)
            cubicTo(
                cx - size.width * 0.36f, size.height * 0.58f,
                cx - size.width * 0.32f, size.height * 0.22f,
                cx, size.height * 0.16f
            )
            close()
        }

        drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Clapper at bottom
        drawLine(
            color = color,
            start = Offset(cx - 2.5.dp.toPx(), size.height * 0.82f),
            end = Offset(cx + 2.5.dp.toPx(), size.height * 0.82f),
            strokeWidth = strokeWidth * 1.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun NavJournalIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val w = size.width * 0.72f
        val h = size.height * 0.82f
        val x = (size.width - w) / 2f
        val y = (size.height - h) / 2f

        // Document / Notebook body
        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Internal note lines
        val lineMargin = w * 0.22f
        val lineStartX = x + lineMargin
        val lineEndX = x + w - lineMargin
        val lineSpacing = h * 0.22f

        for (i in 1..3) {
            val lineY = y + h * 0.26f + (i - 1) * lineSpacing
            drawLine(
                color = color,
                start = Offset(lineStartX, lineY),
                end = Offset(if (i == 3) lineStartX + (lineEndX - lineStartX) * 0.6f else lineEndX, lineY),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun NavAnalyticsIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val bottomY = size.height * 0.85f
        val startX = size.width * 0.14f
        val endX = size.width * 0.86f

        // Baseline
        drawLine(
            color = color,
            start = Offset(startX, bottomY),
            end = Offset(endX, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val barWidth = 3.2.dp.toPx()

        // Bar 1 (Short)
        val x1 = size.width * 0.28f
        drawLine(
            color = color,
            start = Offset(x1, bottomY),
            end = Offset(x1, size.height * 0.54f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )

        // Bar 2 (Tall)
        val x2 = size.width * 0.50f
        drawLine(
            color = color,
            start = Offset(x2, bottomY),
            end = Offset(x2, size.height * 0.20f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )

        // Bar 3 (Medium)
        val x3 = size.width * 0.72f
        drawLine(
            color = color,
            start = Offset(x3, bottomY),
            end = Offset(x3, size.height * 0.36f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Lock Icon for Task Locks & Security (Zero Emojis).
 */
@Composable
fun SimpleLockIcon(
    color: Color,
    modifier: Modifier = Modifier.size(18.dp),
    isLocked: Boolean = true
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height

        val bodyW = w * 0.76f
        val bodyH = h * 0.50f
        val bodyX = (w - bodyW) / 2f
        val bodyY = h * 0.44f
        val corner = 3.dp.toPx()

        // Shackle arch
        val shackleRadius = bodyW * 0.30f
        val shackleCx = w / 2f
        val shackleBottomY = bodyY

        val shacklePath = Path().apply {
            if (isLocked) {
                moveTo(shackleCx - shackleRadius, shackleBottomY)
                lineTo(shackleCx - shackleRadius, h * 0.24f)
                cubicTo(
                    shackleCx - shackleRadius, h * 0.06f,
                    shackleCx + shackleRadius, h * 0.06f,
                    shackleCx + shackleRadius, h * 0.24f
                )
                lineTo(shackleCx + shackleRadius, shackleBottomY)
            } else {
                // Open shackle (turned slightly up)
                moveTo(shackleCx - shackleRadius, shackleBottomY)
                lineTo(shackleCx - shackleRadius, h * 0.16f)
                cubicTo(
                    shackleCx - shackleRadius, h * 0.02f,
                    shackleCx + shackleRadius, h * 0.02f,
                    shackleCx + shackleRadius, h * 0.16f
                )
                lineTo(shackleCx + shackleRadius, h * 0.24f)
            }
        }
        drawPath(shacklePath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Lock Body
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyX, bodyY),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Center Keyhole dot & stem
        drawCircle(
            color = color,
            center = Offset(w / 2f, bodyY + bodyH * 0.42f),
            radius = 1.8.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w / 2f, bodyY + bodyH * 0.46f),
            end = Offset(w / 2f, bodyY + bodyH * 0.72f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Clean Vector Magnifying Glass Search Icon (Zero Emojis).
 */
@Composable
fun SearchVectorIcon(
    color: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val r = size.minDimension * 0.32f
        val cx = size.width * 0.40f
        val cy = size.height * 0.40f

        drawCircle(
            color = color,
            center = Offset(cx, cy),
            radius = r,
            style = Stroke(width = strokeWidth)
        )

        // Handle
        val cos45 = 0.7071f
        val startX = cx + r * cos45
        val startY = cy + r * cos45
        val endX = size.width * 0.88f
        val endY = size.height * 0.88f

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth * 1.2f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Theme Toggle Icon (Sun / Crescent Moon, Zero Emojis).
 */
@Composable
fun ThemeToggleIcon(
    isDark: Boolean,
    color: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeWidth = 1.6.dp.toPx()

        if (isDark) {
            // Sun icon (click to switch to light)
            val sunRadius = size.minDimension * 0.22f
            drawCircle(
                color = color,
                center = Offset(cx, cy),
                radius = sunRadius,
                style = Stroke(width = strokeWidth)
            )
            // 8 delicate sun rays
            val rayInner = sunRadius + 2.dp.toPx()
            val rayOuter = sunRadius + 4.5.dp.toPx()
            for (i in 0 until 8) {
                val angle = (i * 45.0) * (Math.PI / 180.0)
                val x1 = cx + (rayInner * Math.cos(angle)).toFloat()
                val y1 = cy + (rayInner * Math.sin(angle)).toFloat()
                val x2 = cx + (rayOuter * Math.cos(angle)).toFloat()
                val y2 = cy + (rayOuter * Math.sin(angle)).toFloat()
                drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeWidth * 0.9f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            // Crescent Moon icon (click to switch to dark)
            val moonPath = Path().apply {
                val r = size.minDimension * 0.38f
                moveTo(cx - r * 0.2f, cy - r)
                cubicTo(
                    cx + r * 0.9f, cy - r * 0.75f,
                    cx + r * 0.9f, cy + r * 0.75f,
                    cx - r * 0.2f, cy + r
                )
                cubicTo(
                    cx + r * 0.32f, cy + r * 0.5f,
                    cx + r * 0.32f, cy - r * 0.5f,
                    cx - r * 0.2f, cy - r
                )
                close()
            }
            drawPath(
                path = moonPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Minimalist Vector Trash Can Icon for deleting reminders (Zero Emojis).
 */
@Composable
fun TrashCanIcon(
    color: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.6.dp.toPx()

        // Lid horizontal bar
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.28f),
            end = Offset(w * 0.85f, h * 0.28f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Handle on top of lid
        val handlePath = Path().apply {
            moveTo(w * 0.38f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.16f)
            lineTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.62f, h * 0.28f)
        }
        drawPath(
            path = handlePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bin body
        val binPath = Path().apply {
            moveTo(w * 0.24f, h * 0.34f)
            lineTo(w * 0.30f, h * 0.88f)
            lineTo(w * 0.70f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.34f)
        }
        drawPath(
            path = binPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Interior vertical slats
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.44f),
            end = Offset(w * 0.42f, h * 0.78f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.44f),
            end = Offset(w * 0.58f, h * 0.78f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Clock Face Icon (Zero Emojis).
 */
@Composable
fun ClockFaceIcon(
    color: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.6.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = (size.minDimension / 2f) - strokeWidth

        drawCircle(
            color = color,
            center = Offset(cx, cy),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        // Hour hand pointing to 10
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx - radius * 0.35f, cy - radius * 0.4f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Minute hand pointing to 2
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(cx + radius * 0.45f, cy - radius * 0.25f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Calendar Icon (Zero Emojis).
 */
@Composable
fun CalendarEventIcon(
    color: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.6.dp.toPx()

        // Calendar body
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.22f),
            size = Size(w * 0.70f, h * 0.68f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Top binder pins
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.12f),
            end = Offset(w * 0.35f, h * 0.26f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.65f, h * 0.12f),
            end = Offset(w * 0.65f, h * 0.26f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Header divider
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.42f),
            end = Offset(w * 0.85f, h * 0.42f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Warning Triangle Icon (Zero Emojis).
 */
@Composable
fun WarningTriangleIcon(
    color: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.6.dp.toPx()

        val trianglePath = Path().apply {
            moveTo(w / 2f, h * 0.16f)
            lineTo(w * 0.88f, h * 0.84f)
            lineTo(w * 0.12f, h * 0.84f)
            close()
        }
        drawPath(
            path = trianglePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Exclamation vertical line
        drawLine(
            color = color,
            start = Offset(w / 2f, h * 0.40f),
            end = Offset(w / 2f, h * 0.60f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Exclamation dot
        drawCircle(
            color = color,
            center = Offset(w / 2f, h * 0.73f),
            radius = strokeWidth * 0.7f
        )
    }
}

/**
 * Minimalist Vector Hamburger Menu Icon (3 horizontal bars, Zero Emojis).
 */
@Composable
fun HamburgerVectorIcon(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val lineSpacing = h * 0.32f
        val startY = h * 0.18f

        drawLine(
            color = color,
            start = Offset(w * 0.12f, startY),
            end = Offset(w * 0.88f, startY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.12f, startY + lineSpacing),
            end = Offset(w * 0.88f, startY + lineSpacing),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.12f, startY + lineSpacing * 2f),
            end = Offset(w * 0.88f, startY + lineSpacing * 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Back Arrow Icon (Zero Emojis).
 */
@Composable
fun BackArrowVectorIcon(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        // Arrow head
        val path = Path().apply {
            moveTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.25f, h * 0.50f)
            lineTo(w * 0.55f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Arrow body
        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.50f),
            end = Offset(w * 0.85f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Minimalist Vector Squad / Friends Group Icon (Zero Emojis).
 */
@Composable
fun NavSquadIcon(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        // Center Person (Leader) Head
        drawCircle(
            color = color,
            radius = w * 0.14f,
            center = Offset(w * 0.5f, h * 0.26f),
            style = Stroke(width = stroke)
        )
        // Center Person Body Arc
        val centerBodyPath = Path().apply {
            moveTo(w * 0.28f, h * 0.65f)
            quadraticTo(w * 0.5f, h * 0.44f, w * 0.72f, h * 0.65f)
        }
        drawPath(
            path = centerBodyPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Left Person Head
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = w * 0.10f,
            center = Offset(w * 0.22f, h * 0.38f),
            style = Stroke(width = stroke)
        )
        // Left Person Body Arc
        val leftBodyPath = Path().apply {
            moveTo(w * 0.08f, h * 0.76f)
            quadraticTo(w * 0.22f, h * 0.56f, w * 0.36f, h * 0.76f)
        }
        drawPath(
            path = leftBodyPath,
            color = color.copy(alpha = 0.7f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Right Person Head
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = w * 0.10f,
            center = Offset(w * 0.78f, h * 0.38f),
            style = Stroke(width = stroke)
        )
        // Right Person Body Arc
        val rightBodyPath = Path().apply {
            moveTo(w * 0.64f, h * 0.76f)
            quadraticTo(w * 0.78f, h * 0.56f, w * 0.92f, h * 0.76f)
        }
        drawPath(
            path = rightBodyPath,
            color = color.copy(alpha = 0.7f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
