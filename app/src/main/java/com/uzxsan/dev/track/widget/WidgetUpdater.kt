package com.uzxsan.dev.track.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.uzxsan.dev.track.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Keeps home-screen widgets in sync: whenever a tracker's data changes (from the app or from a
 * widget action), the matching widgets are re-rendered. The first emission is skipped because it is
 * just the current state at startup.
 */
object WidgetUpdater {
    fun start(context: Context, container: AppContainer, scope: CoroutineScope) {
        val appContext = context.applicationContext
        scope.launch {
            container.todoRepository.observeAll().drop(1).collect { TodoWidget().updateAll(appContext) }
        }
        scope.launch {
            container.moneyRepository.observeTransactions().drop(1).collect { MoneyWidget().updateAll(appContext) }
        }
        scope.launch {
            container.dsaRepository.observeAll().drop(1).collect { DsaWidget().updateAll(appContext) }
        }
    }
}
