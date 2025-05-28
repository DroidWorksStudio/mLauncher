package com.github.droidworksstudio.launcher

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import com.github.droidworksstudio.launcher.utils.UIUtils

class ContactsAdapter(
    private val activity: MainActivity,
    private var contacts: MutableList<Pair<String, Int>>,
    private val contactClickListener: OnContactClickListener,
    private val contactShortcutListener: OnContactShortcutListener,
) :
    RecyclerView.Adapter<ContactsAdapter.AppViewHolder>() {

    var shortcutIndex: Int = 0
    var shortcutTextView: TextView? = null

    private val uiUtils = UIUtils(activity)
    private val sharedPreferenceManager = SharedPreferenceManager(activity)

    interface OnContactClickListener {
        fun onContactClick(contactId: Int)
    }

    interface OnContactShortcutListener {
        fun onContactShortcut(contactId: Int, contactName: String, shortcutView: TextView, shortcutIndex: Int)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val listItem: FrameLayout = itemView.findViewById(R.id.listItem)
        val textView: TextView = listItem.findViewById(R.id.appName)

        init {
            textView.setOnClickListener {
                if (shortcutTextView != null) {
                    val position = bindingAdapterPosition
                    val contact = contacts[position]
                    contactShortcutListener.onContactShortcut(contact.second, contact.first, shortcutTextView!!, shortcutIndex)
                } else {
                    val position = bindingAdapterPosition
                    val contact = contacts[position]
                    contactClickListener.onContactClick(contact.second)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item_layout, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val contact = contacts[position]
        holder.textView.setCompoundDrawablesWithIntrinsicBounds(
            ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null, ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null
        )

        uiUtils.setAppAlignment(holder.textView)

        uiUtils.setAppSize(holder.textView)

        uiUtils.setItemSpacing(holder.textView)

        uiUtils.setTextFont(holder.listItem)
        holder.textView.setTextColor(sharedPreferenceManager.getTextColor())

        holder.textView.text = contact.first

        holder.textView.visibility = View.VISIBLE

        ViewCompat.addAccessibilityAction(holder.textView, activity.getString(R.string.close_app_menu)) { _, _ ->
            activity.backToHome()
            true
        }

        if (sharedPreferenceManager.areContactsEnabled()) {
            ViewCompat.addAccessibilityAction(holder.textView, activity.getString(R.string.switch_to_apps)) { _, _ ->
                activity.switchMenus()
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateContacts(newContacts: List<Pair<String, Int>>) {
        contacts = newContacts.toMutableList()
        notifyDataSetChanged()
    }
}