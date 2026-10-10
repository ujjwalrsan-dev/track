package com.uzxsan.dev.track.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.dsa.DsaStats
import com.uzxsan.dev.track.dsa.computeStats
import com.uzxsan.dev.track.dsa.countsByDay
import com.uzxsan.dev.track.navigation.LaunchRequest
import com.uzxsan.dev.track.navigation.Routes
import com.uzxsan.dev.track.ui.components.heatLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private data class DsaWidgetState(val stats: DsaStats, val today: Int, val recent: List<Int>)

private val heatColors = intArrayOf(
    R.color.widget_heat_0, R.color.widget_heat_1, R.color.widget_heat_2, R.color.widget_heat_3, R.color.widget_heat_4,
)

/** Home-screen widget with DSA stats and recent activity; + opens the Log Problem sheet. */
class DsaWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as TrackApplication).container.dsaRepository
        val states = repository.observeAll().map { problems ->
            val today = LocalDate.now()
            val counts = countsByDay(problems)
            DsaWidgetState(
                stats = computeStats(problems, today),
                today = counts[today] ?: 0,
                recent = (13 downTo 0).map { counts[today.minusDays(it.toLong())] ?: 0 },
            )
        }
        val initial = states.first()
        provideContent {
            val state by states.collectAsState(initial)
            GlanceTheme { DsaWidgetContent(state) }
        }
    }
}

class DsaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DsaWidget()
}

@Composable
private fun DsaWidgetContent(state: DsaWidgetState) {
    val context = LocalContext.current
    val openDsa = actionStartActivity(LaunchRequest(Routes.DSA, openAdd = false).toIntent(context))
    val logProblem = actionStartActivity(LaunchRequest(Routes.DSA, openAdd = true).toIntent(context))
    // Hide the activity strip when the widget is resized to a single short row.
    val showStrip = LocalSize.current.height >= 150.dp

    GlassWidgetPanel(onClick = openDsa) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                WidgetText(context.getString(R.string.dsa_title), size = 20.sp, weight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_fire),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(14.dp),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    WidgetText(
                        text = "${state.stats.currentStreak} ${context.getString(R.string.dsa_stat_streak)}",
                        size = 12.sp,
                        color = WidgetColors.secondaryText,
                    )
                }
            }
            AccentCircleButton(
                icon = R.drawable.ic_add,
                contentDescription = context.getString(R.string.dsa_log_problem),
                onClick = logProblem,
            )
        }
        Spacer(GlanceModifier.height(12.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatTile(state.stats.total.toString(), context.getString(R.string.dsa_stat_solved), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            StatTile(state.today.toString(), context.getString(R.string.widget_today_solved), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            StatTile(
                state.stats.activeDays.toString(),
                context.getString(R.string.dsa_stat_active_days),
                GlanceModifier.defaultWeight(),
            )
        }

        if (showStrip) {
            Spacer(GlanceModifier.height(12.dp))
            SegmentRow(count = state.recent.size, gap = 3.dp) { index ->
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(heatColors[heatLevel(state.recent[index])]),
                ) {}
            }
            Spacer(GlanceModifier.height(4.dp))
            WidgetText(context.getString(R.string.home_last_14_days), size = 11.sp, color = WidgetColors.secondaryText)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: GlanceModifier) {
    Column(modifier = modifier.glassTile().padding(horizontal = 12.dp, vertical = 8.dp)) {
        WidgetText(value, size = 20.sp, weight = FontWeight.Bold)
        WidgetText(label, size = 11.sp, color = WidgetColors.secondaryText)
    }
}
