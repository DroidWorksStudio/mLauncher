package com.github.droidworksstudio.mlauncher.data

import com.github.droidworksstudio.mlauncher.helper.emptyString
import java.text.Collator

val contactCollator: Collator = Collator.getInstance()

/**
 * Represents a contact item for lists such as:
 * 1. Contact drawer
 *
 * @property displayName
 * The name of the contact (as stored in the Contacts provider).
 *
 * @property phoneNumber
 * The primary phone number (can be empty if none).
 *
 * @property email
 * The primary email address (optional).
 *
 * @property customLabel
 * A user-defined label (nickname) that overrides the display name.
 *
 * @property category
 * Used to separate contacts into groups (e.g., FAVORITE, RECENT, REGULAR).
 *
 * @property isHeader
 * Flag to mark items that are section headers in the list.
 */
data class ContactListItem(
    val displayName: String,
    val phoneNumber: String,
    val email: String,
    var customLabel: String,
    var customTag: String,
    var category: ContactCategory = ContactCategory.REGULAR,
    val isHeader: Boolean = false
) : Comparable<ContactListItem> {

    val label: String = customLabel.ifEmpty { displayName }
    val tag = customTag.ifEmpty { emptyString() }

    /** Speed up sort and search */
    private val collationKey = contactCollator.getCollationKey(label)

    override fun compareTo(other: ContactListItem): Int =
        collationKey.compareTo(other.collationKey)
}

/**
 * Categories to classify contacts in the list
 */
enum class ContactCategory {
    FAVORITE, RECENT, REGULAR
}
