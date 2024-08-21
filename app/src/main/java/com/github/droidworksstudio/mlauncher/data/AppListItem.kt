package com.github.droidworksstudio.mlauncher.data

import android.os.UserHandle
import java.text.Collator

val collator: Collator = Collator.getInstance()

/**
 * We create instances in 3 different places:
 * 1. app drawer
 * 2. recent apps
 * 3. home screen (for the list and for swipes/taps)
 *
 * @property activityLabel
 * label of the activity (`LauncherActivityInfo.label`)
 *
 * @property activityPackage
 * Package name of the activity (`LauncherActivityInfo.applicationInfo.packageName`)
 *
 * @property activityClass
 * (`LauncherActivityInfo.componentName.className`)
 *
 * @property priority user-defined value which affects ordering.
 * Items with higher priority appear higher in the list.
 * Default priority is 0.
 * You can playfully fine-tune the order by adding a fractional part to priority: 6.5, 10.125.
 * You can also put items to the bottom of the list by setting a negative priority: -1, -0.5
 *
 * @property user
 * userHandle is needed to resolve and start an activity.
 * And also we mark with a special icon the apps which belong to a managed user.
 *
 * @property customLabel
 * When user renames an app, we store the rename in Preferences.
 *
 * @property label
 * Use this property to render the list item.
 * It's either the original activity label (`activityLabel`) or a user-defined label (`definedLabel`).
 */

data class AppListItem(
    val activityLabel: String,
    val activityPackage: String,
    val activityClass: String,
    val user: UserHandle,
    var customLabel: String, // TODO make immutable by refining data flow
    val priority: Double,
) : Comparable<AppListItem> {
    val label = activityLabel.ifEmpty { customLabel }

    /** Speed up sort and search */
    private val collationKey = collator.getCollationKey(label)

    /**
     * Compare by `priority`, then by name.
     * It works like `ORDER BY rank, priority` in sql
     */
    override fun compareTo(other: AppListItem): Int {
        val byPriority by lazy { priority.compareTo(other.priority) }
        val byTitle by lazy { collationKey.compareTo(other.collationKey) }

        return if (byPriority != 0) byPriority else byTitle
    }
}
