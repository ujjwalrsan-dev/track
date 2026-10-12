package com.uzxsan.dev.track.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.ui.theme.accentStrong

/** The app-wide background layer that every glass surface refracts. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

val GlassTint = Color.White.copy(alpha = 0.35f)

/**
 * Liquid Glass surface: blurs and refracts [LocalBackdrop] behind it, with a rim highlight and shadow.
 * Blur needs API 31+ and lens refraction API 33+; older devices get the frosted tint only.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    tint: Color = GlassTint,
    blurRadius: Dp = 6.dp,
    refractionHeight: Dp = 20.dp,
    refractionAmount: Dp = 40.dp,
    chromaticAberration: Boolean = true,
    backdrop: Backdrop = LocalBackdrop.current,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(refractionHeight.toPx(), refractionAmount.toPx(), chromaticAberration = chromaticAberration)
                },
                onDrawSurface = { drawRect(tint) },
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/**
 * Clear (unfrosted) glass extended FAB. Pass a [backdrop] that includes the content scrolling
 * beneath it so the lens refracts that content, not just the app background.
 */
@Composable
fun GlassExtendedFab(
    text: String,
    icon: Painter,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val color = MaterialTheme.colorScheme.primary
    GlassSurface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(50),
        // No blur keeps it clear; a light tint and gentler lens keep the label readable
        // over busy content.
        tint = Color.White.copy(alpha = 0.3f),
        blurRadius = 0.dp,
        refractionHeight = 12.dp,
        refractionAmount = 24.dp,
        chromaticAberration = false,
        backdrop = backdrop,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = text.takeUnless { expanded }, tint = color)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                )
            }
        }
    }
}

/**
 * Floating top bar: a frosted glass pill with an optional back chevron and the screen title,
 * over a soft fade that keeps the status bar readable when content scrolls beneath it.
 * [visible] animates the whole bar in and out (Home shows it only once the large title scrolls away).
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    onBack: (() -> Unit)? = null,
    backdrop: Backdrop = LocalBackdrop.current,
    actions: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it / 3 },
        exit = fadeOut() + slideOutVertically { -it / 3 },
    ) {
        val surface = MaterialTheme.colorScheme.surface
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.9f), Color.Transparent)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassSurface(
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(50),
                    tint = Color.White.copy(alpha = 0.5f),
                    blurRadius = 16.dp,
                    refractionHeight = 12.dp,
                    refractionAmount = 24.dp,
                    chromaticAberration = false,
                    backdrop = backdrop,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = if (onBack == null) 20.dp else 0.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.back))
                            }
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                actions?.invoke()
            }
        }
    }
}

/** Round frosted glass button, sized to sit beside a [GlassTopBar] pill. */
@Composable
fun GlassIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    GlassSurface(
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(50),
        tint = Color.White.copy(alpha = 0.5f),
        blurRadius = 16.dp,
        refractionHeight = 12.dp,
        refractionAmount = 24.dp,
        chromaticAberration = false,
        backdrop = backdrop,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * Frosted glass button tinted with [color] and white content, for actions inside glass cards.
 * Shows [icon] and, when given, [text]; without text it is a round icon button.
 */
@Composable
fun GlassTintedButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    contentDescription: String? = text,
    color: Color = accentStrong(),
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val shape = RoundedCornerShape(50)
    GlassSurface(
        modifier = modifier
            .height(40.dp)
            .then(if (text == null) Modifier.size(40.dp) else Modifier)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape),
        shape = shape,
        tint = color.copy(alpha = 0.8f),
        blurRadius = 12.dp,
        refractionHeight = 8.dp,
        refractionAmount = 16.dp,
        chromaticAberration = false,
        backdrop = backdrop,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (text == null) Modifier.fillMaxWidth() else Modifier.padding(horizontal = 14.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription.takeIf { text == null }, Modifier.size(18.dp), tint = Color.White)
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/**
 * Bottom sheet whose visible body is a frosted glass card inset from the screen edges. The sheet's
 * own surface is transparent; pass a [backdrop] that includes the screen content so it shows
 * through blurred.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    backdrop: Backdrop = LocalBackdrop.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.2f),
        dragHandle = null,
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(32.dp),
            tint = Color.White.copy(alpha = 0.55f),
            blurRadius = 16.dp,
            backdrop = backdrop,
        ) {
            Column(Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                content()
            }
        }
    }
}

/**
 * Bright backdrop the glass refracts: a soft gradient, colored blobs and a faint dot grid
 * (the dots make the lens distortion at glass edges visible).
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier.background(scheme.surface)) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    scheme.primaryContainer.copy(alpha = 0.55f),
                    scheme.surface,
                    scheme.tertiaryContainer.copy(alpha = 0.5f),
                )
            )
        )
        blob(scheme.primary.copy(alpha = 0.45f), Offset(size.width * 0.15f, size.height * 0.22f), size.width * 0.55f)
        blob(scheme.tertiary.copy(alpha = 0.40f), Offset(size.width * 0.95f, size.height * 0.30f), size.width * 0.5f)
        blob(scheme.secondary.copy(alpha = 0.30f), Offset(size.width * 0.55f, size.height * 0.48f), size.width * 0.45f)
        blob(scheme.primary.copy(alpha = 0.25f), Offset(size.width * 0.2f, size.height * 0.85f), size.width * 0.6f)

        val step = 22.dp.toPx()
        val dot = scheme.onSurface.copy(alpha = 0.07f)
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(dot, radius = 1.5.dp.toPx(), center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
}
