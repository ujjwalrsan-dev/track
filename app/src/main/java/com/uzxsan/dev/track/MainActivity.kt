package com.uzxsan.dev.track

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.uzxsan.dev.track.navigation.LaunchRequest
import com.uzxsan.dev.track.navigation.TrackNavHost
import com.uzxsan.dev.track.ui.components.GlassBackground
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.theme.TrackTheme

class MainActivity : ComponentActivity() {

    /** Pending deep link from a widget; cleared once the nav host has handled it. */
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. Holds the brand splash for a short, fixed beat.
        val splashStart = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - splashStart < SPLASH_MILLIS
        }
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the intent is the same and was already handled.
        if (savedInstanceState == null) launchRequest = LaunchRequest.from(intent)
        // The app is always light (glass on a bright backdrop), so system bar icons stay dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TrackTheme(darkTheme = false) {
                // One static background shared by every screen; screens slide over it.
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    GlassBackground(
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                    )
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        TrackNavHost(
                            launchRequest = launchRequest,
                            onLaunchHandled = { launchRequest = null },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val SPLASH_MILLIS = 2_000L
    }

    /** The activity is singleTask, so widget taps while the app is open arrive here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LaunchRequest.from(intent)?.let { launchRequest = it }
    }
}
