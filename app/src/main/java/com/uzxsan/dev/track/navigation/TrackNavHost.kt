package com.uzxsan.dev.track.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uzxsan.dev.track.dsa.DsaScreen
import com.uzxsan.dev.track.home.HomeScreen
import com.uzxsan.dev.track.money.MoneyScreen
import com.uzxsan.dev.track.todo.TodoScreen

object Routes {
    const val HOME = "home"
    const val DSA = "dsa"
    const val TODO = "todo"
    const val BUDGET = "budget"
}

// Material 3 shared-axis X motion: short slide + quick fade-through.
private const val SLIDE_MILLIS = 300
private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private typealias Transitions = AnimatedContentTransitionScope<NavBackStackEntry>

private fun sharedAxisEnter(forward: Boolean): Transitions.() -> EnterTransition = {
    slideInHorizontally(tween(SLIDE_MILLIS, easing = EmphasizedDecelerate)) {
        (if (forward) it else -it) / 5
    } + fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
}

private fun sharedAxisExit(forward: Boolean): Transitions.() -> ExitTransition = {
    slideOutHorizontally(tween(SLIDE_MILLIS, easing = EmphasizedDecelerate)) {
        (if (forward) -it else it) / 5
    } + fadeOut(tween(FADE_OUT_MILLIS))
}

private const val ARG_ADD = "add"

/** Route pattern with an optional `add` flag that opens the screen's add sheet on arrival. */
private fun withAddArg(route: String) = "$route?$ARG_ADD={$ARG_ADD}"

private val addArgument = listOf(navArgument(ARG_ADD) {
    type = NavType.BoolType
    defaultValue = false
})

/**
 * @param launchRequest set when the app is opened from a widget; the host navigates there (on top
 *   of Home, so Back returns Home) and then calls [onLaunchHandled].
 */
@Composable
fun TrackNavHost(launchRequest: LaunchRequest? = null, onLaunchHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        navController.navigate("${request.route}?$ARG_ADD=${request.openAdd}") {
            popUpTo(Routes.HOME)
        }
        onLaunchHandled()
    }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = sharedAxisEnter(forward = true),
        exitTransition = sharedAxisExit(forward = true),
        popEnterTransition = sharedAxisEnter(forward = false),
        popExitTransition = sharedAxisExit(forward = false),
    ) {
        composable(Routes.HOME) {
            HomeScreen(onOpenTracker = { route -> navController.navigate(route) })
        }
        composable(withAddArg(Routes.DSA), arguments = addArgument) { entry ->
            DsaScreen(
                onBack = { navController.popBackStack() },
                openAddOnStart = entry.arguments?.getBoolean(ARG_ADD) == true,
            )
        }
        composable(withAddArg(Routes.BUDGET), arguments = addArgument) { entry ->
            MoneyScreen(
                onBack = { navController.popBackStack() },
                openAddOnStart = entry.arguments?.getBoolean(ARG_ADD) == true,
            )
        }
        composable(withAddArg(Routes.TODO), arguments = addArgument) { entry ->
            TodoScreen(
                onBack = { navController.popBackStack() },
                openAddOnStart = entry.arguments?.getBoolean(ARG_ADD) == true,
            )
        }
    }
}
