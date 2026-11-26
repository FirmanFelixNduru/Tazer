package com.tazer.taskorganizer.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Blue-white neumorphic tokens (docs/UI_NEUMORPHIC_SPEC.md). Light and dark keep text at 4.5:1 or more
 * and every interactive boundary at 3:1 or more; the shadows are decoration on top of that.
 */
@Immutable
internal class Neu(
    val canvas: Color, val canvasTop: Color, val canvasBottom: Color, val surface: Color, val recessed: Color,
    val highlight: Color, val shade: Color, val ink: Color, val muted: Color, val outline: Color,
    val accent: Color, val onAccent: Color,
) {
    val canvasBrush get() = Brush.verticalGradient(listOf(canvasTop, canvasBottom))

    companion object {
        val Light = Neu(
            canvas = Color(0xFFEDF2FB), canvasTop = Color(0xFFF3F7FF), canvasBottom = Color(0xFFE7EEFB),
            surface = Color(0xFFF4F7FD), recessed = Color(0xFFE1E8F5),
            highlight = Color(0xF2FFFFFF), shade = Color(0xB8A9B9D8),
            ink = Color(0xFF141A2E), muted = Color(0xFF535E77), outline = Color(0xFF76839F),
            accent = Color(0xFF2254E6), onAccent = Color.White,
        )
        val Dark = Neu(
            canvas = Color(0xFF141923), canvasTop = Color(0xFF19202C), canvasBottom = Color(0xFF10141C),
            surface = Color(0xFF1A2130), recessed = Color(0xFF0E1219),
            highlight = Color(0xFF222B3D), shade = Color(0xFF090C12),
            ink = Color(0xFFE6EAF2), muted = Color(0xFF9AA3B5), outline = Color(0xFF64708A),
            accent = Color(0xFF4F94FF), onAccent = Color(0xFF08142E),
        )

        /** Sign In fill: the white label sits on the #2254E6 middle stop (6.0:1). */
        val SignInGradient = Brush.horizontalGradient(listOf(Color(0xFF2B82FF), Color(0xFF2254E6), Color(0xFF3B38D6)))
    }
}

@Composable
internal fun neu(): Neu = if (LocalTazerDark.current) Neu.Dark else Neu.Light

/**
 * Extruded surface: a light shadow to the top-left and a cool dark one to the bottom-right, drawn with the
 * platform shadow layer. Below API 28 the shadows are skipped and the flat fill plus border stays readable.
 */
internal fun Modifier.neumorphicRaised(neu: Neu, radius: Dp, depth: Dp = 5.dp, fill: Color = neu.surface) = drawWithCache {
    val r = min(radius.toPx(), size.minDimension / 2)
    val d = depth.toPx()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = fill.toArgb() }
    val light = neu.highlight.toArgb()
    val dark = neu.shade.toArgb()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            paint.setShadowLayer(d * 1.8f, -d * .8f, -d * .8f, light)
            native.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
            paint.setShadowLayer(d * 2.2f, d, d, dark)
            native.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
            paint.clearShadowLayer()
        }
    }
}

/** Recessed track or field: the fill with a dark inner shadow on the top-left edge and a light one bottom-right. */
internal fun Modifier.neumorphicInset(neu: Neu, radius: Dp, depth: Dp = 3.dp, fill: Color = neu.recessed) = drawWithCache {
    val r = min(radius.toPx(), size.minDimension / 2)
    val d = depth.toPx()
    val edge = android.graphics.Path().apply {
        addRoundRect(0f, 0f, size.width, size.height, r, r, android.graphics.Path.Direction.CW)
    }
    val base = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = fill.toArgb() }
    val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = d * 2
        color = fill.toArgb()
    }
    val light = neu.highlight.toArgb()
    val dark = neu.shade.toArgb()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.drawPath(edge, base)
            native.save()
            native.clipPath(edge)
            stroke.setShadowLayer(d * 1.6f, d, d, dark)
            native.drawPath(edge, stroke)
            stroke.setShadowLayer(d * 1.6f, -d * .7f, -d * .7f, light)
            native.drawPath(edge, stroke)
            native.restore()
        }
    }
}

/** Raised card on the canvas. Content keeps Material colours, so text contrast follows the theme. */
@Composable
internal fun NeuCard(
    modifier: Modifier = Modifier, radius: Dp = 20.dp, fill: Color? = null, border: Color? = null,
    onClick: (() -> Unit)? = null, content: @Composable () -> Unit,
) {
    val neu = neu()
    val color = fill ?: neu.surface
    val shape = RoundedCornerShape(radius)
    val stroke = BorderStroke(1.dp, border ?: neu.outline.copy(alpha = .35f))
    if (onClick == null) {
        Surface(modifier.neumorphicRaised(neu, radius, fill = color), shape = shape, color = color, border = stroke, content = content)
    } else {
        Surface(onClick, modifier.neumorphicRaised(neu, radius, fill = color), shape = shape, color = color, border = stroke, content = content)
    }
}

/** Royal-blue squircle used for the add actions (task FAB, Kalender +). */
@Composable
internal fun NeuAccentSquircle(modifier: Modifier = Modifier, size: Dp = 56.dp, content: @Composable BoxScope.() -> Unit) {
    val neu = neu()
    Box(
        modifier.size(size).neumorphicRaised(neu, size * .32f, fill = neu.accent)
            .background(neu.accent, RoundedCornerShape(size * .32f)),
        contentAlignment = Alignment.Center, content = content,
    )
}

/** Two-way segmented control: a recessed track with the selected segment raised in the accent colour. */
@Composable
internal fun NeuSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val neu = neu()
    val track = RoundedCornerShape(26.dp)
    Row(
        modifier.fillMaxWidth().neumorphicInset(neu, 26.dp).border(1.dp, neu.outline, track).padding(5.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val segment = RoundedCornerShape(21.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp)
                    .then(if (active) Modifier.neumorphicRaised(neu, 21.dp, 3.dp, neu.accent).background(neu.accent, segment) else Modifier)
                    .clip(segment).selectable(active, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, color = if (active) neu.onAccent else neu.ink,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Round raised icon action (edit, check, delete, month arrows) with an outline that keeps 3:1 on its own. */
@Composable
internal fun NeuIconAction(glyph: TazerGlyph, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val neu = neu()
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(40.dp).neumorphicRaised(neu, 20.dp, 3.dp).background(neu.surface, CircleShape)
                .border(1.dp, neu.outline, CircleShape).clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }.alpha(if (enabled) 1f else .45f),
            contentAlignment = Alignment.Center,
        ) { CompositionLocalProvider(LocalContentColor provides neu.ink) { TazerIcon(glyph) } }
    }
}

/** Whole-screen canvas with the soft vertical gradient. */
internal fun Modifier.neuCanvas(neu: Neu, shape: Shape = RoundedCornerShape(0.dp)) = background(neu.canvasBrush, shape)
