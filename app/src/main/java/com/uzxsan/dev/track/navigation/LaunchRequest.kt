package com.uzxsan.dev.track.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uzxsan.dev.track.MainActivity

/**
 * A request to open a tracker screen from outside the app (e.g. a widget), optionally with its
 * "add" sheet already open. Encoded as `track://<route>?add=true` on the launching intent; the
 * distinct URI also keeps each widget button's PendingIntent unique.
 */
data class LaunchRequest(val route: String, val openAdd: Boolean) {

    fun toIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("$SCHEME://$route?$ADD=$openAdd"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    companion object {
        private const val SCHEME = "track"
        private const val ADD = "add"

        fun from(intent: Intent?): LaunchRequest? {
            val data = intent?.data ?: return null
            if (data.scheme != SCHEME) return null
            val route = data.host?.takeIf { it in setOf(Routes.DSA, Routes.TODO, Routes.BUDGET) } ?: return null
            return LaunchRequest(route, data.getQueryParameter(ADD) == "true")
        }
    }
}
