package com.uzxsan.dev.track.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.navigation.Routes

// [Kotlin] `data class` = a Java POJO in one line. The compiler generates the constructor,
// getters, equals(), hashCode(), toString() and copy() for you. Java equivalent:
//   public final class TrackerInfo {
//       private final String id; private final int title; ...
//       public TrackerInfo(String id, int title, ...) { this.id = id; ... }
//       public String getId() { return id; } ...  + equals/hashCode/toString
//   }
// `val` = final field with a getter (read-only). `var` would add a setter too.
// Types come AFTER the name: `id: String` instead of Java's `String id`.
/** A utility listed on the home screen. Add an entry here (plus a route) to ship a new tracker. */
data class TrackerInfo(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val route: String,
)

// [Kotlin] `object` = a singleton. Java equivalent: a class with a private constructor and
// static members. You use it as `Trackers.all` from anywhere, like a static field.
object Trackers {
    // [Kotlin] `const val` = Java `public static final String DSA = "dsa";` (a compile-time constant).
    const val DSA = "dsa"
    const val TODO = "todo"
    const val BUDGET = "budget"

    // [Kotlin] listOf(...) builds a read-only List. No `new ArrayList<>()` + add() calls needed.
    // The type List<TrackerInfo> is inferred, so you don't have to write it.
    // Inside, `id = DSA, title = ...` are NAMED ARGUMENTS: you label each constructor parameter,
    // so order doesn't matter and you never mix up two Ints. Java has no equivalent (you'd use a Builder).
    val all = listOf(
        TrackerInfo(
            id = DSA,
            title = R.string.dsa_title,
            description = R.string.dsa_description,
            icon = R.drawable.ic_code,
            route = Routes.DSA,
        ),
        TrackerInfo(
            id = TODO,
            title = R.string.todo_title,
            description = R.string.todo_description,
            icon = R.drawable.ic_task,
            route = Routes.TODO,
        ),
        TrackerInfo(
            id = BUDGET,
            title = R.string.budget_title,
            description = R.string.budget_description,
            icon = R.drawable.ic_wallet,
            route = Routes.BUDGET,
        ),
    )
}
