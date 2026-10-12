package com.uzxsan.dev.track.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/*
 * Component colors built from the theme's primary family (the accent that tints the glass cards),
 * replacing Material's default secondary/outline grays. They follow the dynamic wallpaper color.
 */

/** Soft accent outline used where Material would draw a gray `outline`. */
@Composable
fun accentOutline(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

/** Opaque, very light accent for empty tracks and unfilled cells. */
@Composable
fun accentTrack(): Color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.12f)

@Composable
fun accentSegmentedButtonColors(): SegmentedButtonColors {
    val scheme = MaterialTheme.colorScheme
    return SegmentedButtonDefaults.colors(
        activeContainerColor = scheme.primaryContainer,
        activeContentColor = scheme.onPrimaryContainer,
        activeBorderColor = accentOutline(),
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = scheme.onSurface,
        inactiveBorderColor = accentOutline(),
    )
}

@Composable
fun accentFilterChipColors(): SelectableChipColors {
    val scheme = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
        selectedLeadingIconColor = scheme.onPrimaryContainer,
    )
}

@Composable
fun accentFilterChipBorder(selected: Boolean): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = accentOutline(),
        selectedBorderColor = Color.Transparent,
    )

@Composable
fun accentSuggestionChipBorder(): BorderStroke =
    SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = accentOutline())

@Composable
fun accentAssistChipColors(): ChipColors =
    AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary)

@Composable
fun accentAssistChipBorder(): BorderStroke =
    AssistChipDefaults.assistChipBorder(enabled = true, borderColor = accentOutline())

/** Filled button whose disabled state is a faint accent instead of gray. */
@Composable
fun accentButtonColors(): ButtonColors {
    val primary = MaterialTheme.colorScheme.primary
    return ButtonDefaults.buttonColors(
        disabledContainerColor = primary.copy(alpha = 0.12f),
        disabledContentColor = primary.copy(alpha = 0.5f),
    )
}

private val ColorScheme.isDark: Boolean get() = surface.luminance() < 0.5f

/** Deep accent that stays readable under white content in both themes (dark primary is pale). */
@Composable
fun accentStrong(): Color = MaterialTheme.colorScheme.let { if (it.isDark) it.inversePrimary else it.primary }

/** Deep error tone for destructive glass buttons with white content. */
@Composable
fun errorStrong(): Color = MaterialTheme.colorScheme.let { if (it.isDark) it.errorContainer else it.error }
