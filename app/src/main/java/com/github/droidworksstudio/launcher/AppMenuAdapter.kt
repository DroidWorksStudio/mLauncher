package com.github.droidworksstudio.launcher

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.databinding.ActivityMainBinding
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import com.github.droidworksstudio.launcher.utils.AppUtils
import com.github.droidworksstudio.launcher.utils.UIUtils
import com.google.android.material.textfield.TextInputEditText


class AppMenuAdapter(
    private val activity: MainActivity,
    binding: ActivityMainBinding,
    private var apps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>,
    private val itemClickListener: OnItemClickListener,
    private val shortcutListener: OnShortcutListener,
    private val itemLongClickListener: OnItemLongClickListener,
    launcherApps: LauncherApps
) :
    RecyclerView.Adapter<AppMenuAdapter.AppViewHolder>() {

    // If the menu is opened to select shortcuts, the below variable is set
    var shortcutIndex: Int = 0
    var shortcutTextView: TextView? = null

    private val sharedPreferenceManager = SharedPreferenceManager(activity)
    private val uiUtils = UIUtils(activity)
    private val appUtils = AppUtils(activity, launcherApps)
    private var appActionMenu = AppActionMenu(activity, binding, launcherApps, activity.findViewById(R.id.searchView))

    interface OnItemClickListener {
        fun onItemClick(appInfo: LauncherActivityInfo, userHandle: UserHandle)
    }

    interface OnShortcutListener {
        fun onShortcut(appInfo: LauncherActivityInfo, userHandle: UserHandle, textView: TextView, userProfile: Int, shortcutView: TextView, shortcutIndex: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(
            textView: TextView,
            actionMenuLayout: LinearLayout
        )
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val listItem: FrameLayout = itemView.findViewById(R.id.listItem)
        val textView: TextView = listItem.findViewById(R.id.appName)
        val actionMenuLayout: LinearLayout = listItem.findViewById(R.id.actionMenu)
        val editView: LinearLayout = listItem.findViewById(R.id.renameView)
        val editText: TextInputEditText = editView.findViewById(R.id.appNameEdit)

        init {
            textView.setOnClickListener {
                val position = bindingAdapterPosition
                val app = apps[position].first

                // If opened to select a shortcut, set the shortcut instead of launching the app
                if (shortcutTextView != null) {
                    shortcutListener.onShortcut(app, apps[position].second, textView, apps[position].third, shortcutTextView!!, shortcutIndex)
                } else {
                    itemClickListener.onItemClick(app, apps[position].second)
                }
            }

            textView.setOnLongClickListener {
                val position = bindingAdapterPosition

                val app = apps[position].first

                // If opened to select a shortcut, set the shortcut instead of opening the action menu
                if (shortcutTextView != null) {
                    shortcutListener.onShortcut(app, apps[position].second, textView, apps[position].third, shortcutTextView!!, shortcutIndex)
                    return@setOnLongClickListener true
                } else {

                    itemLongClickListener.onItemLongClick(
                        textView,
                        actionMenuLayout
                    )
                    return@setOnLongClickListener true
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
        holder.actionMenuLayout.visibility = View.INVISIBLE
        holder.editView.visibility = View.INVISIBLE
        val app = apps[position]

        if (sharedPreferenceManager.isAppPinned(app.first.componentName.flattenToString(), app.third)) {
            if (app.third != 0) {
                holder.textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(activity.resources, R.drawable.pin, null), null, ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null)
            } else {
                holder.textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(activity.resources, R.drawable.pin, null), null, ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null)
            }
            holder.textView.compoundDrawables[0].colorFilter = BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
        }
        // Set initial drawables
        else if (app.third != 0) {
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_work_app, null), null, ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null)
            holder.textView.compoundDrawables[0].colorFilter =
                BlendModeColorFilter(sharedPreferenceManager.getTextColor(), BlendMode.SRC_ATOP)
        } else {
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null, ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_empty, null), null)
        }

        uiUtils.setAppAlignment(holder.textView, holder.editText)

        uiUtils.setAppSize(holder.textView, holder.editText)

        uiUtils.setItemSpacing(holder.textView)

        uiUtils.setTextFont(holder.listItem)
        holder.textView.setTextColor(sharedPreferenceManager.getTextColor())

        // Update the application information (allows updating apps to work)
        val isAppInstalled = appUtils.getAppInfo(
            app.first.applicationInfo.packageName,
            app.third
        ) != null

        // Set app name on the menu. If the app has been uninstalled, replace it with "Removing" until the app menu updates.
        if (isAppInstalled) {
            holder.textView.text = sharedPreferenceManager.getAppName(
                app.first.componentName.flattenToString(),
                app.third,
                app.first.label
            )

            holder.editText.setText(holder.textView.text)

            // Remove the uninstall icon for system apps
            if (app.first.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                holder.actionMenuLayout.findViewById<TextView>(R.id.uninstall).visibility =
                    View.GONE
            } else {
                holder.actionMenuLayout.findViewById<TextView>(R.id.uninstall).visibility =
                    View.VISIBLE
            }
        } else {
            holder.textView.text = activity.getString(R.string.removing)
        }

        holder.textView.visibility = View.VISIBLE

        if (isAppInstalled) {
            appActionMenu.setActionListeners(
                holder.textView,
                holder.editView,
                holder.actionMenuLayout,
                app.first,
                app.second,
                app.third,
            )
        }

        ViewCompat.addAccessibilityAction(holder.textView, activity.getString(R.string.close_app_menu)) { _, _ ->
            activity.backToHome()
            true
        }

        if (sharedPreferenceManager.areContactsEnabled()) {
            ViewCompat.addAccessibilityAction(holder.textView, activity.getString(R.string.switch_to_contacts)) { _, _ ->
                activity.switchMenus()
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateApps(newApps: List<Triple<LauncherActivityInfo, UserHandle, Int>>) {
        apps = newApps.toMutableList()
        notifyDataSetChanged()
    }
}