package com.uzxsan.dev.track

import android.app.Application
import com.uzxsan.dev.track.data.AppContainer
import com.uzxsan.dev.track.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    /** Lives as long as the process; used for app-wide background work like widget refreshes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // First run of the Budget tracker: its three grand totals and a default "Cash" account.
        appScope.launch { container.moneyRepository.ensureDefaults() }
        WidgetUpdater.start(this, container, appScope)
    }
}
