package com.github.droidworksstudio.mlauncher.data

import android.health.connect.datatypes.AppInfo
import android.os.UserHandle
import java.text.Collator

val collator: Collator = Collator.getInstance()

// TODO: rename fields: cut off the `app` prefix
// TODO make difference between data from the system, and from the prefs (extract object?)
/**
 * @property appLabel
 * label of the activity (`LauncherActivityInfo.label`)
 *
 * @property appPackage
 * Package name of the activity (`LauncherActivityInfo.applicationInfo.packageName`)
 *
 * @property appActivityName
 * (`LauncherActivityInfo.componentName.className`)
 *
 * @property priority user-defined value which affects ordering.
 * Items with higher priority appear higher in the list.
 * Default priority is 0.
 * You can playfully fine-tune the order by adding a fractional part to priority: 6.5, 10.125.
 *
 * @property user
 * userHandle is needed to resolve and start an activity.
 * And also we mark with a special icon the apps which belong to a managed user.
 *
 * @property appAlias
 * When user renames an app, we store the rename as `alias`
 *
 * @property name
 * The title of the app item. Either the original name (`appLabel`) or a user-defined alias (``appAlias`).
 *
 * TODO we create instances in 3 different places:
 *      1. app drawer, 2. recent apps, 3. home screen (for the list and for swipes/taps)
 */
data class AppListItem(
    val appLabel: String,
    val appPackage: String,
    val appActivityName: String,
    val user: UserHandle,
    var appAlias: String, // TODO make immutable by refining data flow
    val priority: Double,
) : Comparable<AppListItem> {
    // TODO rename. Visible name?
    val name = appLabel.ifEmpty { appAlias }

    /** Speed up the sort and search */
    private val collationKey = collator.getCollationKey(name)

    /**
     * Compare by `priority`, then by name.
     * It works like `ORDER BY rank, priority` in sql
     */
    override fun compareTo(other: AppListItem): Int {
        priority.compareTo(other.priority).also { if (it != 0) return it}
        return collationKey.compareTo(other.collationKey)
    }
}
