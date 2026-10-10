package com.uzxsan.dev.track.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.uzxsan.dev.track.R

/*
 * Widget "glass": launchers draw widgets as RemoteViews, which can't blur the wallpaper behind
 * them. These parts recreate the look with translucent layered drawables instead: a frosted fill
 * with a top sheen and a bright rim (see res/drawable/widget_glass*.xml, with dark variants).
 */

/** Text colors that stay readable on the glass in light and dark mode. */
object WidgetColors {
    val primaryText = ColorProvider(day = Color(0xFF1B1B1F), night = Color(0xFFF2F0F4))
    val secondaryText = ColorProvider(day = Color(0xFF46464F), night = Color(0xFFC7C5D0))
}

/** Root frosted glass panel. Also marks the widget background so launchers round its corners. */
@Composable
fun GlassWidgetPanel(onClick: Action, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(28.dp)
            .background(ImageProvider(R.drawable.widget_glass))
            .clickable(onClick)
            .padding(16.dp),
        content = content,
    )
}

/** A smaller glass tile for rows and stats inside the panel. */
fun GlanceModifier.glassTile(): GlanceModifier =
    cornerRadius(18.dp).background(ImageProvider(R.drawable.widget_glass_tile))

/** Filled accent circle with an icon, e.g. the "+" button. */
@Composable
fun AccentCircleButton(icon: Int, contentDescription: String, onClick: Action, size: Dp = 36.dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(
                ImageProvider(R.drawable.widget_circle),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(size * 0.55f),
        )
    }
}

@Composable
fun WidgetText(
    text: String,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: ColorProvider = WidgetColors.primaryText,
    maxLines: Int = 1,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight),
        maxLines = maxLines,
        modifier = modifier,
    )
}

private const val ROW_CHUNK = 7

/**
 * Equal-width segments in a row (heat strips, stage bars). Glance renders at most 10 children per
 * Row, so segments are grouped into nested rows of [ROW_CHUNK], and gaps are padding, not spacers.
 */
@Composable
fun SegmentRow(count: Int, gap: Dp, modifier: GlanceModifier = GlanceModifier, segment: @Composable (Int) -> Unit) {
    Row(modifier = modifier.fillMaxWidth()) {
        (0 until count).chunked(ROW_CHUNK).forEach { chunk ->
            Row(modifier = GlanceModifier.defaultWeight()) {
                chunk.forEach { index ->
                    Box(modifier = GlanceModifier.defaultWeight().padding(horizontal = gap / 2)) {
                        segment(index)
                    }
                }
            }
        }
    }
}
