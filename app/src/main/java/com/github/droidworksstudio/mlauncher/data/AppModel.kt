package com.github.droidworksstudio.mlauncher.data

import android.os.UserHandle
import java.text.Collator

val collator: Collator = Collator.getInstance()

// TODO rename the class ? AppListItem
// TODO: rename fields: cut off the `app` prefix
data class AppModel(
        val appLabel: String,
        val appPackage: String,
        val appActivityName: String,
        val user: UserHandle,
        var appAlias: String, // TODO why var?
) : Comparable<AppModel> {
    val name = appLabel.ifEmpty { appAlias }

    /** Speed up the sort and search */
    private val collationKey = collator.getCollationKey(name)

    /**
     * TODO sort by priority, name
     * TODO doc the priority
     *  - how it's used
     *  - valid values
     *  - default value
     */
    override fun compareTo(other: AppModel): Int = collationKey.compareTo(other.collationKey)
}
