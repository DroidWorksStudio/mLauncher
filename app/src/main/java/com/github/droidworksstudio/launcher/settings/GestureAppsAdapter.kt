package com.github.droidworksstudio.launcher.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.utils.UIUtils

class GestureAppsAdapter(
    private val context: Context,
    var apps: MutableList<Triple<LauncherActivityInfo, UserHandle, Int>>,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<GestureAppsAdapter.AppViewHolder>() {

    private val sharedPreferenceManager = SharedPreferenceManager(context)
    private val uiUtils = UIUtils(context)

    interface OnItemClickListener {
        fun onItemClick(appInfo: LauncherActivityInfo, profile: Int)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listItem: FrameLayout = itemView.findViewById(R.id.listItem)
        val textView: TextView = listItem.findViewById(R.id.appName)

        init {
            textView.setOnClickListener {
                val position = bindingAdapterPosition
                val app = apps[position].first
                itemClickListener.onItemClick(app, apps[position].third)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item_layout, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        if (app.third != 0) {
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_work_app, null
                ), null, null, null
            )
        } else {
            holder.textView.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_empty, null
                ), null, null, null
            )
        }

        uiUtils.setAppAlignment(holder.textView)

        uiUtils.setAppSize(holder.textView)

        uiUtils.setItemSpacing(holder.textView)

        // Does not need to be specially updated since it's in a separate activity and thus reloads when opened again
        holder.textView.text = sharedPreferenceManager.getAppName(
            app.first.componentName.flattenToString(),
            app.third,
            app.first.label
        )

        holder.textView.visibility = View.VISIBLE
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