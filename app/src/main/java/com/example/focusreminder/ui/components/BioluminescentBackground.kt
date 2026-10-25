package com.example.focusreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusreminder.theme.NeuBackground

/**
 * Clean Neumorphic Porcelain Background Canvas (Matches reference images).
 */
@Composable
fun BioluminescentBackground(
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBackground) // Clean off-white / porcelain canvas
    ) {
        content()
    }
}

/**
 * Neumorphic Card alias (replaces old glass card)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    NeuCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        onClick = onClick,
        content = content
    )
}

/**
 * Neumorphic Pill alias (replaces old glass pill)
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    NeuPill(
        modifier = modifier,
        cornerRadius = 28.dp,
        onClick = onClick,
        content = content
    )
}
