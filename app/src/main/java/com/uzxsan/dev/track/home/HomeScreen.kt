package com.uzxsan.dev.track.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.components.GlassTopBar
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.components.heatColor
import com.uzxsan.dev.track.ui.theme.accentTrack

// =====================================================================================
// [Compose] THE BIG IDEA, compared to Java + XML:
//
//   Java/XML: you write layout XML, inflate it in an Activity/Fragment, findViewById() each view,
//             then imperatively CHANGE them later: textView.setText(...), view.setVisibility(...).
//
//   Compose:  the UI is a plain Kotlin function annotated with @Composable. It DESCRIBES what the
//             screen should look like for the current data. When the data changes, Compose calls
//             the function again ("recomposition") and updates only what changed.
//             There is no XML, no findViewById, no setText. To change the UI, you change the data.
// =====================================================================================
//
// [Kotlin] `fun` declares a function. A function can live at the top level of a file,
// with no class around it (Java requires every method to be inside a class).
//
// [Kotlin] Parameters:
//   onOpenTracker: (route: String) -> Unit
//       A FUNCTION TYPE: "a function taking a String and returning nothing" (Unit = Java's void).
//       In Java you'd write a callback interface: interface OnOpenTracker { void open(String route); }
//   viewModel: HomeViewModel = viewModel(...)
//       A DEFAULT ARGUMENT. Callers can just write HomeScreen(onOpenTracker = ...) and Android
//       supplies the ViewModel, scoped to this screen like ViewModelProvider(this).get(...) in Java.
//
@Composable
fun HomeScreen(
    onOpenTracker: (route: String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    // [Compose] Observe the ViewModel's StateFlow, like observing LiveData in Java:
    //   viewModel.getUiState().observe(this, state -> render(state));
    // When a new value arrives, Compose re-runs this function with the new `state`.
    // "WithLifecycle" = stops observing while the app is in the background (like LiveData does).
    // [Kotlin] `by` is DELEGATION: reading `state` actually reads the observed value's `.value`.
    // Without `by` you'd write `stateHolder.value.summaries` everywhere.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // [Compose] rememberLazyListState() = the scroll position of the list. In Java you'd query
    // recyclerView.computeVerticalScrollOffset() inside an OnScrollListener.
    // [Compose] `remember { }` keeps a value across recompositions (otherwise it would be recreated
    // every time this function re-runs). derivedStateOf { } recalculates only when the scroll
    // position changes, so the pill flips only when the big title reaches the status bar.
    val listState = rememberLazyListState()
    // The list starts 24dp below the status bar, so once the title has scrolled past that gap it is
    // sliding under the clock: show the pill from then on.
    val titleThreshold = with(LocalDensity.current) { 24.dp.roundToPx() }
    val titleScrolledAway by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleThreshold }
    }

    // The pill floats over the cards, so its glass blurs the cards as well as the background.
    val contentBackdrop = rememberLayerBackdrop()
    val glassBackdrop = rememberCombinedBackdrop(LocalBackdrop.current, contentBackdrop)

    // [Compose] Scaffold = the standard screen skeleton (top bar, content, FAB, snackbar slots),
    // roughly what CoordinatorLayout + AppBarLayout gave you in XML.
    // Each `name = value` below is a NAMED ARGUMENT; most parameters are optional with defaults.
    // There's no `topBar` here: the large title is a list row, and the pill is drawn on top in a Box.
    Scaffold(
        containerColor = Color.Transparent,
    // [Kotlin] TRAILING LAMBDA: when a function's last parameter is a lambda, it can go outside the
    // parentheses. So `Scaffold(...) { padding -> ... }` is really `Scaffold(..., content = { padding -> ... })`.
    // `padding` = space taken by the system bars (status/navigation), so content can avoid them.
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        // [Compose] Box stacks its children on top of each other, like FrameLayout:
        // first the list, then the pill drawn over it.
        Box(Modifier.fillMaxSize()) {
            // [Compose] LazyColumn = RecyclerView, without the Adapter, ViewHolder or LayoutManager.
            // Like RecyclerView, it only composes the rows that are visible on screen.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop),
                contentPadding = PaddingValues(
                    // [Kotlin] `16.dp` is an EXTENSION PROPERTY: Compose adds a `.dp` property to Int.
                    // It replaces "16dp" in XML / TypedValue.applyDimension(...) in Java code.
                    // `+` works on Dp values because Kotlin allows operator overloading (Java doesn't).
                    start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = padding.calculateTopPadding() + 24.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // [Compose] Inside LazyColumn { } you declare rows (a small DSL, i.e. a mini-language of function calls):
                //   item { ... }               -> one row
                //   items(list) { element -> } -> one row per element (what onBindViewHolder did)
                // [Compose] Text(...) = TextView. stringResource(...) = getString(R.string...).
                item(key = "title") {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                // [Compose] `key = { it.id }` gives each row a stable identity, like RecyclerView's stable IDs.
                items(Trackers.all, key = { it.id }) { tracker ->
                    TrackerCard(
                        tracker = tracker,
                        // [Kotlin] map[key] = map.get(key). The result is NULLABLE (TrackerSummary?) because the key might be missing.
                        summary = state.summaries[tracker.id],
                        // [Kotlin] A lambda used as a click listener. Java equivalent:
                        //   card.setOnClickListener(v -> onOpenTracker.open(tracker.getRoute()));
                        // Lambdas can capture any local value; no "effectively final" rule to worry about.
                        onClick = { onOpenTracker(tracker.route) },
                    )
                }
                item { ComingSoonCard() }
            }

            // [Compose] Showing the pill is just passing a Boolean; GlassTopBar animates in/out itself.
            // In Java you'd call pill.animate().alpha(1f) from an OnScrollListener.
            GlassTopBar(
                title = stringResource(R.string.app_name),
                visible = titleScrolledAway,
                backdrop = glassBackdrop,
            )
        }
    }
}

// [Kotlin] `private` at the top level = visible only inside this file.
// Note `summary: TrackerSummary?`: the `?` means the value MAY be null. A type WITHOUT `?` can never be
// null; the compiler enforces it. This is Kotlin's answer to NullPointerException: null-safety is part of the
// type system instead of @Nullable annotations you hope everyone respects.
@Composable
private fun TrackerCard(tracker: TrackerInfo, summary: TrackerSummary?, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        onClick = onClick,
    ) {
        // [Compose] Column = vertical LinearLayout, Row = horizontal LinearLayout, Box = FrameLayout.
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackerIcon(tracker.icon)
                Spacer(Modifier.size(16.dp))
                // [Compose] Modifier.weight(1f) = android:layout_weight="1" (take the remaining space).
                Column(Modifier.weight(1f)) {
                    Text(stringResource(tracker.title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(tracker.description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // [Compose] Showing/hiding UI is just an `if`. There's no setVisibility(View.GONE):
            // if the condition is false, the views inside are simply not part of the screen.
            // [Kotlin] SMART CAST: after `summary != null`, the compiler knows summary isn't null inside
            // this block, so `summary.stats` compiles without `!!` or an extra check.
            if (summary != null) {
                Spacer(Modifier.height(20.dp))
                Row {
                    // [Kotlin] forEach { stat -> } = Java `for (TrackerStat stat : summary.getStats()) { ... }`.
                    // [Kotlin] `summary.stats` calls the generated getter: Java's summary.getStats().
                    summary.stats.forEach { stat ->
                        Column(Modifier.weight(1f)) {
                            Text(stat.value, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                stringResource(stat.label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.recent.forEach { count ->
                        // [Compose] MODIFIERS replace XML attributes (size, padding, background, click...).
                        // They're chained like a builder, and ORDER MATTERS: here clip() comes before
                        // background(), so the color is drawn inside the rounded shape.
                        Box(
                            Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(heatColor(count))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.home_last_14_days),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackerIcon(icon: Int) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            // [Kotlin] Passing `null` is only allowed because the parameter's type is `String?` (nullable).
            // contentDescription = null means "decorative icon" for accessibility (TalkBack skips it).
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ComingSoonCard() {
    OutlinedCard(
        shape = RoundedCornerShape(32.dp),
        // [Compose] MaterialTheme.colorScheme.* = theme attributes like ?attr/colorPrimary in XML.
        // Color.White.copy(alpha = 0.4f) = white at 40% opacity (copy with one value changed).
        // `0.4f` is a Float literal, the same as in Java.
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentTrack()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(stringResource(R.string.home_coming_soon_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.home_coming_soon_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
