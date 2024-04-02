package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Editable
import android.view.Gravity.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.fuzzywuzzy.FuzzyFinder
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppModel
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.AdapterAppDrawerBinding
import com.github.droidworksstudio.mlauncher.helper.AppDetailsHelper.isSystemApp
import com.github.droidworksstudio.mlauncher.helper.dp2px
import com.github.droidworksstudio.mlauncher.helper.getHexFontColor

class AppDrawerAdapter(
    private val context: Context,
    private var flag: AppDrawerFlag,
    private val gravity: Int,
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    private val appRenameListener: (String, String) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var appFilter = createAppFilter()
    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        prefs = Prefs(parent.context)
        if (prefs.useCustomIconFont) {
            val typeface = ResourcesCompat.getFont(parent.context, R.font.roboto)
            binding.appTitle.typeface = typeface
        }
        if (prefs.followAccentColors) {
            val fontColor = getHexFontColor(parent.context)
            binding.appTitle.setTextColor(fontColor)
        }
        binding.appTitle.textSize = prefs.textSizeLauncher.toFloat()
        val padding: Int = prefs.textMarginSize
        binding.appTitle.setPadding(0, padding, 0, padding)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.size == 0) return
        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        holder.bind(flag, gravity, appModel, appClickListener, appDeleteListener, appRenameListener, appInfoListener)

        holder.appHide.setOnClickListener {
            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)
            appHideListener(flag, appModel)
        }
        autoLaunch(position)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()
                val filteredApps: MutableList<AppModel>

                if (prefs.filterStrength >= 1 ) {
                    val scoredApps = mutableMapOf<AppModel, Int>()
                    for (app in appsList) {
                        scoredApps[app] = FuzzyFinder.scoreApp(app, searchChars, Constants.FILTER_STRENGTH_MAX)
                    }

                    filteredApps = if (searchChars.isNotEmpty()) {
                        if (prefs.searchFromStart) {
                            scoredApps.filter { (app, _) -> app.name.startsWith(searchChars, ignoreCase = true) }
                                .filter { (_, score) -> score > prefs.filterStrength }
                                .map { it.key }
                                .toMutableList()
                        } else {
                            scoredApps.filterValues { it > prefs.filterStrength }
                                .keys
                                .toMutableList()
                        }
                    } else {
                        appsList.toMutableList()
                    }
                } else {
                    filteredApps = (if (searchChars.isEmpty()) appsList
                    else appsList.filter { app ->
                        if (app.appAlias.isEmpty()) {
                            FuzzyFinder.normalizeString(app.appLabel, searchChars)
                        } else {
                            FuzzyFinder.normalizeString(app.appAlias, searchChars)
                        }
                    } as MutableList<AppModel>)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredApps
                return filterResults
            }


            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                appFilteredList = results?.values as MutableList<AppModel>
                notifyDataSetChanged()
            }
        }
    }

    private fun autoLaunch(position: Int) {
        val lastMatch = itemCount == 1
        val openApp = flag == AppDrawerFlag.LaunchApp
        val autoOpenApp = prefs.autoOpenApp
        if (lastMatch && openApp && autoOpenApp) {
            try { // Automatically open the app when there's only one search result
                appClickListener(appFilteredList[position])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAppList(appsList: MutableList<AppModel>) {
        this.appsList = appsList
        this.appFilteredList = appsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (appFilteredList.size > 0)
            appClickListener(appFilteredList[0])
    }

    class ViewHolder(itemView: AdapterAppDrawerBinding) : RecyclerView.ViewHolder(itemView.root) {
        val appHide: TextView = itemView.appHide

        private val appRenameEdit: EditText = itemView.appRenameEdit
        private val appHideLayout: LinearLayout = itemView.appHideLayout
        private val appTitle: TextView = itemView.appTitle
        private val appTitleFrame: FrameLayout = itemView.appTitleFrame
        private val appInfo: TextView = itemView.appInfo
        private val appRename: TextView = itemView.appRename
        private val appDelete: TextView = itemView.appDelete

        @SuppressLint("RtlHardcoded")
        fun bind(
            appFlag: AppDrawerFlag,
            appLabelGravity: Int,
            appModel: AppModel,
            appClickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appRenameListener: (String, String) -> Unit,
            appInfoListener: (AppModel) -> Unit
        ) =
            with(itemView) {
                appHideLayout.visibility = View.GONE

                // set show/hide icon
                val drawable = if (appFlag == AppDrawerFlag.HiddenApps) { R.drawable.visibility } else { R.drawable.visibility_off }
                appHide.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0)


//                appRenameEdit.addTextChangedListener(object : TextWatcher {
//
//                    override fun afterTextChanged(s: Editable) {}
//
//                    override fun beforeTextChanged(
//                        s: CharSequence, start: Int,
//                        count: Int, after: Int
//                    ) {
//                    }
//
//                    override fun onTextChanged(
//                        s: CharSequence, start: Int,
//                        before: Int, count: Int
//                    ) {
////                        if (appRenameEdit.text.isEmpty()) {
////                            appRenameButton.text = context.getString(R.string.reset)
////                        } else if (appRenameEdit.text.toString() == appModel.appAlias) {
////                            appRenameButton.text = context.getString(R.string.cancel)
////                        } else {
////                            appRenameButton.text = context.getString(R.string.rename)
////                        }
//                    }
//                })

                val appName = appModel.appAlias.ifEmpty {
                    appModel.appLabel
                }

                appTitle.text = appName

                // set current name as default text in EditText
                appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

                // set text gravity
                val params = appTitle.layoutParams as FrameLayout.LayoutParams
                params.gravity = appLabelGravity
                appTitle.layoutParams = params

                // add icon next to app name to indicate that this app is installed on another profile
                if (appModel.user != android.os.Process.myUserHandle()) {
                    val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
                    val prefs = Prefs(context)
                    val px = dp2px(resources, prefs.textSizeLauncher)
                    icon?.setBounds(0, 0, px, px)
                    if (appLabelGravity == LEFT) {
                        appTitle.setCompoundDrawables(null, null, icon, null)
                    } else {
                        appTitle.setCompoundDrawables(icon, null, null, null)
                    }
                    appTitle.compoundDrawablePadding = 20
                } else {
                    appTitle.setCompoundDrawables(null, null, null, null)
                }

                appTitleFrame.setOnClickListener { appClickListener(appModel) }
                appTitleFrame.setOnLongClickListener {
                    val packageManager: PackageManager = context.packageManager
                    if (appModel.appPackage.isNotEmpty()) {
                        appDelete.alpha = if (isSystemApp(appModel.appPackage, packageManager)) 0.5f else 1.0f
                        appHide.text = if (appFlag == AppDrawerFlag.HiddenApps) {
                            context.getString(R.string.show)
                        } else {
                            context.getString(R.string.hide)
                        }
                        appHideLayout.visibility = View.VISIBLE
                    }
                    true
                }

                appDelete.apply {
                    setOnClickListener { appDeleteListener(appModel) }
                }

                appInfo.apply {
                    setOnClickListener { appInfoListener(appModel) }
                }

                appRename.apply {
                    setOnClickListener { appRenameListener("1","2") }
                }

                appHideLayout.setOnClickListener {
                    appHideLayout.visibility = View.GONE
                }
            }
    }
}
