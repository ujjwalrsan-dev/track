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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.money.MoneyFormat
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.money.data.TransactionType
import com.uzxsan.dev.track.money.flowTotals
import com.uzxsan.dev.track.money.forRow
import com.uzxsan.dev.track.navigation.LaunchRequest
import com.uzxsan.dev.track.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One row's worth of display data, resolved outside the composition. */
private data class WidgetTransaction(val transaction: MoneyTransaction, val title: String, val subtitle: String)

private data class MoneyWidgetState(val rows: List<WidgetTransaction>, val spent: Long, val earned: Long)

/** Home-screen widget listing today's transactions with today's spend and income. */
class MoneyWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as TrackApplication).container.moneyRepository
        val today = LocalDate.now()
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val states = repository.observe().map { snapshot ->
            val names = snapshot.accounts.associate { it.id to it.name }
            val todays = snapshot.transactions.filter { it.localDate == today }
            val flow = flowTotals(todays)
            MoneyWidgetState(
                rows = todays.map { t ->
                    val title = when {
                        t.type == TransactionType.Transfer -> context.getString(R.string.budget_transfer_title)
                        t.category.isNotBlank() -> t.category
                        t.note.isNotBlank() -> t.note
                        t.type == TransactionType.Income -> context.getString(R.string.budget_income)
                        else -> context.getString(R.string.budget_expense)
                    }
                    val account = if (t.type == TransactionType.Transfer) {
                        "${names[t.accountId].orEmpty()} → ${names[t.toAccountId].orEmpty()}"
                    } else {
                        names[t.accountId].orEmpty()
                    }
                    WidgetTransaction(t, title, "$account · ${t.localTime.format(time)}")
                },
                spent = flow.expense,
                earned = flow.income,
            )
        }
        val initial = states.first()
        provideContent {
            val state by states.collectAsState(initial)
            GlanceTheme { MoneyWidgetContent(state) }
        }
    }
}

class MoneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoneyWidget()
}

@Composable
private fun MoneyWidgetContent(state: MoneyWidgetState) {
    val context = LocalContext.current
    val format = MoneyFormat()
    val openBudget = actionStartActivity(LaunchRequest(Routes.BUDGET, openAdd = false).toIntent(context))
    val addTransaction = actionStartActivity(LaunchRequest(Routes.BUDGET, openAdd = true).toIntent(context))

    GlassWidgetPanel(onClick = openBudget) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                // Widgets only offer Normal/Medium/Bold; Normal is the thinnest available.
                WidgetText(context.getString(R.string.widget_today), size = 20.sp, weight = FontWeight.Normal)
                WidgetText(
                    text = "−${format.formatWhole(state.spent)}  ·  +${format.formatWhole(state.earned)}",
                    size = 12.sp,
                    color = WidgetColors.secondaryText,
                )
            }
            AccentCircleButton(
                icon = R.drawable.ic_add,
                contentDescription = context.getString(R.string.budget_add_transaction),
                onClick = addTransaction,
            )
        }
        Spacer(GlanceModifier.height(12.dp))

        if (state.rows.isEmpty()) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WidgetText(context.getString(R.string.widget_no_transactions), color = WidgetColors.secondaryText)
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(state.rows, itemId = { it.transaction.id }) { row ->
                    Column {
                        TransactionWidgetRow(row, format)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionWidgetRow(row: WidgetTransaction, format: MoneyFormat) {
    val t = row.transaction
    val icon = when (t.type) {
        TransactionType.Income -> R.drawable.ic_income
        TransactionType.Expense -> R.drawable.ic_expense
        TransactionType.Transfer -> R.drawable.ic_transfer
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .glassTile()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(30.dp)
                .background(
                    ImageProvider(R.drawable.widget_circle),
                    alpha = 0.2f,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(16.dp),
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            WidgetText(row.title, size = 14.sp)
            WidgetText(row.subtitle, size = 11.sp, color = WidgetColors.secondaryText)
        }
        Spacer(GlanceModifier.width(8.dp))
        WidgetText(
            text = format.forRow(t),
            size = 15.sp,
            color = if (t.type == TransactionType.Income) GlanceTheme.colors.primary else WidgetColors.primaryText,
        )
    }
}
