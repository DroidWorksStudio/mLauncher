/**
 * Prepare the data for the app drawer, which is the list of all the installed applications.
 */

package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity.LEFT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.showKeyboard
import com.github.droidworksstudio.fuzzywuzzy.FuzzyFinder
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.AdapterAppDrawerBinding
import com.github.droidworksstudio.mlauncher.helper.IconCacheTarget
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper
import com.github.droidworksstudio.mlauncher.helper.dp2px
import com.github.droidworksstudio.mlauncher.helper.getSystemIcons
import com.github.droidworksstudio.mlauncher.helper.utils.BiometricHelper
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.helper.utils.visibleHideLayouts

class AppDrawerAdapter(
    private val context: Context,
    private val fragment: Fragment,
    internal var flag: AppDrawerFlag,
    private val gravity: Int,
    private val appClickListener: (AppListItem) -> Unit,
    private val appDeleteListener: (AppListItem) -> Unit,
    private val appRenameListener: (String, String) -> Unit,
    private val appTagListener: (String, String) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppListItem) -> Unit,
    private val appInfoListener: (AppListItem) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var appFilter = createAppFilter()
    var appsList: MutableList<AppListItem> = mutableListOf()
    var appFilteredList: MutableList<AppListItem> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding
    private lateinit var biometricHelper: BiometricHelper

    private var isBangSearch = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        prefs = Prefs(parent.context)
        biometricHelper = BiometricHelper(fragment)
        val fontColor = prefs.appColor
        binding.appTitle.setTextColor(fontColor)

        binding.appTitle.textSize = prefs.appSize.toFloat()
        val padding: Int = prefs.textPaddingSize
        binding.appTitle.setPadding(0, padding, 0, padding)
        return ViewHolder(binding)
    }

    fun getItemAt(position: Int): AppListItem? {
        return if (position in appsList.indices) appsList[position] else null
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.isEmpty() || position !in appFilteredList.indices) {
            AppLogger.d("AppListDebug", "⚠️ onBindViewHolder called but appFilteredList is empty or position out of bounds")
            return
        }

        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        AppLogger.d("AppListDebug", "🔧 Binding position=$position, label=${appModel.label}, package=${appModel.activityPackage}")

        holder.bind(flag, gravity, appModel, appClickListener, appInfoListener, appDeleteListener)

        holder.appHide.setOnClickListener {
            AppLogger.d("AppListDebug", "❌ Hide clicked for ${appModel.label} (${appModel.activityPackage})")

            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)

            AppLogger.d("AppListDebug", "📤 notifyItemRemoved at ${holder.absoluteAdapterPosition}")
            appHideListener(flag, appModel)
        }

        holder.appLock.setOnClickListener {
            val appName = appModel.activityPackage
            val currentLockedApps = prefs.lockedApps

            if (currentLockedApps.contains(appName)) {
                biometricHelper.startBiometricAuth(appModel, object : BiometricHelper.CallbackApp {
                    override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                        AppLogger.d("AppListDebug", "🔓 Auth succeeded for $appName - unlocking")
                        holder.appLock.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock_off, 0, 0)
                        holder.appLock.text = getLocalizedString(R.string.lock)
                        currentLockedApps.remove(appName)
                        prefs.lockedApps = currentLockedApps
                        AppLogger.d("AppListDebug", "🔐 Updated lockedApps: $currentLockedApps")
                    }

                    override fun onAuthenticationFailed() {
                        AppLogger.e("Authentication", getLocalizedString(R.string.text_authentication_failed))
                    }

                    override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                        val msg = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> getLocalizedString(R.string.text_authentication_cancel)
                            else -> getLocalizedString(R.string.text_authentication_error).format(errorMessage, errorCode)
                        }
                        AppLogger.e("Authentication", msg)
                    }
                })
            } else {
                AppLogger.d("AppListDebug", "🔒 Locking $appName")
                holder.appLock.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock, 0, 0)
                holder.appLock.text = getLocalizedString(R.string.unlock)
                currentLockedApps.add(appName)
            }

            // Update the lockedApps value (save the updated set back to prefs)
            prefs.lockedApps = currentLockedApps
            AppLogger.d("lockedApps", prefs.lockedApps.toString())
        }

        holder.appSaveRename.setOnClickListener {
            val name = holder.appRenameEdit.text.toString().trim()
            AppLogger.d("AppListDebug", "✏️ Renaming ${appModel.activityPackage} to $name")
            appModel.customLabel = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            AppLogger.d("AppListDebug", "🔁 notifyItemChanged at ${holder.absoluteAdapterPosition}")
            appRenameListener(appModel.activityPackage, appModel.customLabel)
        }

        holder.appSaveTag.setOnClickListener {
            val name = holder.appTagEdit.text.toString().trim()
            AppLogger.d("AppListDebug", "✏️ Tagging ${appModel.activityPackage} to $name")
            appModel.customTag = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            AppLogger.d("AppListDebug", "🔁 notifyItemChanged at ${holder.absoluteAdapterPosition}")
            appTagListener(appModel.activityPackage, appModel.customTag)
        }

        autoLaunch(position)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                isBangSearch = listOf("!", "#").any { prefix -> charSearch?.startsWith(prefix) == true }
                prefs = Prefs(context)

                val searchChars = charSearch.toString()
                val filteredApps: MutableList<AppListItem>

                if (prefs.enableFilterStrength) {
                    // Only apply FuzzyFinder scoring logic when filter strength is enabled
                    val scoredApps = mutableMapOf<AppListItem, Int>()
                    if (searchChars.startsWith("#")) {
                        val tagQuery = searchChars.substringAfter("#")
                        for (app in appsList) {
                            scoredApps[app] =
                                FuzzyFinder.scoreString(app.tag, tagQuery, Constants.MAX_FILTER_STRENGTH)
                        }
                    } else {
                        for (app in appsList) {
                            scoredApps[app] =
                                FuzzyFinder.scoreApp(app, searchChars, Constants.MAX_FILTER_STRENGTH)
                        }
                    }

                    filteredApps = if (searchChars.isNotEmpty()) {
                        if (searchChars.startsWith("#")) {
                            val tagQuery = searchChars.substringAfter("#")
                            if (prefs.searchFromStart) {
                                AppLogger.d("searchQuery", tagQuery)
                                scoredApps.filter { (app, _) ->
                                    app.tag.startsWith(tagQuery, ignoreCase = true)
                                }
                                    .filter { (_, score) -> score > prefs.filterStrength }
                                    .map { it.key }
                                    .toMutableList()
                            } else {
                                AppLogger.d("searchQuery", tagQuery)
                                scoredApps.filter { (app, score) ->
                                    app.tag.contains(tagQuery, ignoreCase = true) && score > prefs.filterStrength
                                }
                                    .map { it.key }
                                    .toMutableList()
                            }
                        } else {
                            if (prefs.searchFromStart) {
                                scoredApps.filter { (app, _) ->
                                    app.label.startsWith(searchChars, ignoreCase = true)
                                }
                                    .filter { (_, score) -> score > prefs.filterStrength }
                                    .map { it.key }
                                    .toMutableList()
                            } else {
                                scoredApps.filterValues { it > prefs.filterStrength }
                                    .keys
                                    .toMutableList()
                            }
                        }
                    } else {
                        appsList.toMutableList() // No search term, return all apps
                    }
                } else {
                    // When filter strength is disabled, still apply searchFromStart if there is a search term
                    filteredApps = if (searchChars.isEmpty()) {
                        appsList.toMutableList() // No search term, return all apps
                    } else {
                        val filteredAppsList = if (prefs.searchFromStart) {
                            AppLogger.d("searchQuery", searchChars)
                            if (searchChars.startsWith("#")) {
                                val searchQuery = searchChars.substringAfter("#")
                                appsList.filter { app ->
                                    app.tag.startsWith(searchQuery, ignoreCase = true)
                                }
                            } else {
                                // Apply search from start logic if searchChars is not empty
                                appsList.filter { app ->
                                    app.label.startsWith(searchChars, ignoreCase = true)
                                }
                            }
                        } else {
                            AppLogger.d("searchQuery", searchChars)
                            if (searchChars.startsWith("#")) {
                                val searchQuery = searchChars.substringAfter("#")

                                // Apply fuzzy matching when searchFromStart is false
                                appsList.filter { app ->
                                    FuzzyFinder.isMatch(app.tag, searchQuery)
                                }
                            } else {
                                // Apply fuzzy matching when searchFromStart is false
                                appsList.filter { app ->
                                    FuzzyFinder.isMatch(app.label, searchChars)
                                }
                            }
                        }
                        filteredAppsList.toMutableList() // Convert to MutableList
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = filteredApps
                return filterResults
            }


            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values is MutableList<*>) {
                    appFilteredList = results.values as MutableList<AppListItem>
                    notifyDataSetChanged()
                } else {
                    return
                }
            }
        }
    }

    private fun autoLaunch(position: Int) {
        val lastMatch = itemCount == 1
        val openApp = flag == AppDrawerFlag.LaunchApp
        val autoOpenApp = prefs.autoOpenApp
        if (lastMatch && openApp && autoOpenApp) {
            try { // Automatically open the app when there's only one search result
                if (isBangSearch.not()) appClickListener(appFilteredList[position])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAppList(appsList: MutableList<AppListItem>) {
        this.appsList = appsList
        this.appFilteredList = appsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (appFilteredList.isNotEmpty())
            appClickListener(appFilteredList[0])
    }

    class ViewHolder(itemView: AdapterAppDrawerBinding) : RecyclerView.ViewHolder(itemView.root) {
        val appHide: TextView = itemView.appHide
        val appLock: TextView = itemView.appLock
        val appRenameEdit: EditText = itemView.appRenameEdit
        val appSaveRename: TextView = itemView.appSaveRename
        val appTagEdit: EditText = itemView.appTagEdit
        val appSaveTag: TextView = itemView.appSaveTag

        private val appHideLayout: LinearLayout = itemView.appHideLayout
        private val appRenameLayout: LinearLayout = itemView.appRenameLayout
        private val appTagLayout: LinearLayout = itemView.appTagLayout
        private val appRename: TextView = itemView.appRename
        private val appTag: TextView = itemView.appTag
        private val appPin: TextView = itemView.appPin
        private val appTitle: TextView = itemView.appTitle
        private val appTitleFrame: FrameLayout = itemView.appTitleFrame
        private val appClose: TextView = itemView.appClose
        private val appInfo: TextView = itemView.appInfo
        private val appDelete: TextView = itemView.appDelete

        @SuppressLint("RtlHardcoded", "NewApi")
        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appListItem: AppListItem,
            appClickListener: (AppListItem) -> Unit,
            appInfoListener: (AppListItem) -> Unit,
            appDeleteListener: (AppListItem) -> Unit
        ) =
            with(itemView) {
                val prefs = Prefs(context)
                val contextMenuFlags = prefs.getMenuFlags("CONTEXT_MENU_FLAGS", "0011111")
                appHideLayout.isVisible = false
                appRenameLayout.isVisible = false
                appTagLayout.isVisible = false

                appHide.apply {
                    isVisible = contextMenuFlags[2]
                    // set show/hide icon
                    if (flag == AppDrawerFlag.HiddenApps) {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.visibility, 0, 0)
                        text = getLocalizedString(R.string.show)
                    } else {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.visibility_off, 0, 0)
                        text = getLocalizedString(R.string.hide)
                    }
                }

                val appName = appListItem.activityPackage
                // Access the current locked apps set
                val currentLockedApps = prefs.lockedApps

                appLock.apply {
                    isVisible = contextMenuFlags[1]
                    if (currentLockedApps.contains(appName)) {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock, 0, 0)
                        text = getLocalizedString(R.string.unlock)
                    } else {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock_off, 0, 0)
                        text = getLocalizedString(R.string.lock)
                    }
                }

                val currentPinnedApps = prefs.pinnedApps

                appPin.apply {
                    isVisible = contextMenuFlags[0]
                    if (currentPinnedApps.contains(appName)) {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.pin, 0, 0)
                        text = getLocalizedString(R.string.unpin)
                    } else {
                        setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.pin_off, 0, 0)
                        text = getLocalizedString(R.string.pin)
                    }
                }

                appRename.apply {
                    isVisible = contextMenuFlags[3]
                    setOnClickListener {
                        if (appListItem.activityPackage.isNotEmpty()) {
                            appRenameEdit.hint = appListItem.activityLabel
                            appRenameLayout.isVisible = true
                            appHideLayout.isVisible = false
                            appRenameEdit.showKeyboard()
                            appRenameEdit.imeOptions = EditorInfo.IME_ACTION_DONE
                        }
                    }
                }

                appTag.apply {
                    isVisible = contextMenuFlags[4]
                    setOnClickListener {
                        if (appListItem.activityPackage.isNotEmpty()) {
                            appTagEdit.hint = appListItem.activityLabel
                            appTagLayout.isVisible = true
                            appHideLayout.isVisible = false
                            appTagEdit.showKeyboard()
                            appTagEdit.imeOptions = EditorInfo.IME_ACTION_DONE
                        }
                    }
                }

                appRenameEdit.apply {
                    addTextChangedListener(object : TextWatcher {

                        override fun afterTextChanged(s: Editable) {}

                        override fun beforeTextChanged(
                            s: CharSequence, start: Int,
                            count: Int, after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence, start: Int,
                            before: Int, count: Int
                        ) {
                            if (appRenameEdit.text.isEmpty()) {
                                appSaveRename.text = getLocalizedString(R.string.reset)
                            } else if (appRenameEdit.text.toString() == appListItem.customLabel) {
                                appSaveRename.text = getLocalizedString(R.string.cancel)
                            } else {
                                appSaveRename.text = getLocalizedString(R.string.rename)
                            }
                        }
                    })
                    // set current name as default text in EditText
                    text = Editable.Factory.getInstance().newEditable(appListItem.label)
                }

                appTagEdit.apply {
                    addTextChangedListener(object : TextWatcher {

                        override fun afterTextChanged(s: Editable) {}

                        override fun beforeTextChanged(
                            s: CharSequence, start: Int,
                            count: Int, after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence, start: Int,
                            before: Int, count: Int
                        ) {
                            if (appTagEdit.text.toString() == appListItem.customTag) {
                                appSaveTag.text = getLocalizedString(R.string.cancel)
                            } else {
                                appSaveTag.text = getLocalizedString(R.string.tag)
                            }
                        }
                    })
                    // set current name as default text in EditText
                    text = Editable.Factory.getInstance().newEditable(appListItem.customTag)
                }

                appTitle.text = appListItem.label

                // set text gravity
                val params = appTitle.layoutParams as FrameLayout.LayoutParams
                params.gravity = appLabelGravity
                appTitle.layoutParams = params

                // add icon next to app name to indicate that this app is installed on another profile
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val isPrivateSpace =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        launcherApps.getLauncherUserInfo(appListItem.user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
                    } else {
                        PrivateSpaceManager(context).isPrivateSpaceSupported()
                    }
                val isWorkProfile =
                    appListItem.user != android.os.Process.myUserHandle() && !isPrivateSpace

                val packageName = appListItem.activityPackage
                val packageManager = context.packageManager

                var hasIconEnabled = false
                var myIcon: Drawable? = null

                if (packageName.isNotBlank() && prefs.iconPackAppList != Constants.IconPacks.Disabled) {
                    val iconPackPackage = prefs.customIconPackAppList
                    // Get app icon or fallback drawable
                    val icon: Drawable? = try {
                        if (iconPackPackage.isNotEmpty() && prefs.iconPackAppList == Constants.IconPacks.Custom) {
                            if (IconPackHelper.isReady()) {
                                IconPackHelper.getCachedIcon(
                                    context,
                                    packageName,
                                    IconCacheTarget.APP_LIST
                                )
                                // Use the icon if not null
                            } else {
                                packageManager.getApplicationIcon(packageName)
                            }
                        } else {
                            packageManager.getApplicationIcon(packageName)
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                        // Handle exception gracefully, fall back to the system icon
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Handle any other exceptions gracefully, fallback to the system icon
                        packageManager.getApplicationIcon(packageName)
                    }

                    val defaultIcon = packageManager.getApplicationIcon(packageName)
                    val nonNullDrawable: Drawable = icon ?: defaultIcon

                    // Recolor the icon with the dominant color
                    val appNewIcon: Drawable? = getSystemIcons(
                        context,
                        prefs,
                        IconCacheTarget.APP_LIST,
                        nonNullDrawable
                    )

                    // Set the icon size to match text size and add padding
                    val iconSize = (prefs.appSize * 1.4).toInt()  // Base size from preferences
                    val iconPadding = (iconSize / 1.2).toInt() //

                    appNewIcon?.setBounds(0, 0, iconSize, iconSize)
                    nonNullDrawable.setBounds(
                        0,
                        0,
                        ((iconSize * 1.8).toInt()),
                        ((iconSize * 1.8).toInt())
                    )

                    // Set drawable position based on alignment
                    when (prefs.drawerAlignment) {
                        Constants.Gravity.Left -> {
                            appTitle.setCompoundDrawables(
                                appNewIcon ?: nonNullDrawable,
                                null,
                                null,
                                null
                            )
                            appTitle.compoundDrawablePadding = iconPadding
                            hasIconEnabled = true
                            myIcon = appNewIcon ?: nonNullDrawable
                        }

                        Constants.Gravity.Right -> {
                            appTitle.setCompoundDrawables(
                                null,
                                null,
                                appNewIcon ?: nonNullDrawable,
                                null
                            )
                            appTitle.compoundDrawablePadding = iconPadding
                            hasIconEnabled = true
                            myIcon = appNewIcon ?: nonNullDrawable
                        }

                        else -> appTitle.setCompoundDrawables(null, null, null, null)
                    }
                } else {
                    appTitle.setCompoundDrawables(null, null, null, null)
                }
                if (isWorkProfile) {
                    val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
                    val px = dp2px(resources, prefs.appSize)
                    icon?.setBounds(0, 0, px, px)
                    if (appLabelGravity == LEFT) {
                        if (hasIconEnabled) appTitle.setCompoundDrawables(myIcon, null, icon, null)
                        else appTitle.setCompoundDrawables(null, null, icon, null)
                    } else {
                        if (hasIconEnabled) appTitle.setCompoundDrawables(icon, null, myIcon, null)
                        else appTitle.setCompoundDrawables(icon, null, null, null)
                    }
                    appTitle.compoundDrawablePadding = 20
                } else if (isPrivateSpace) {
                    val icon = AppCompatResources.getDrawable(context, R.drawable.ic_unlock)
                    val px = dp2px(resources, prefs.appSize)
                    icon?.setBounds(0, 0, px, px)
                    if (appLabelGravity == LEFT) {
                        if (hasIconEnabled) appTitle.setCompoundDrawables(myIcon, null, icon, null)
                        else appTitle.setCompoundDrawables(null, null, icon, null)
                    } else {
                        if (hasIconEnabled) appTitle.setCompoundDrawables(icon, null, myIcon, null)
                        else appTitle.setCompoundDrawables(icon, null, null, null)
                    }
                    appTitle.compoundDrawablePadding = 20
                }


                val padding = dp2px(resources, 24)
                appTitle.updatePadding(left = padding, right = padding)

                val sidebarContainer = (context as? Activity)?.findViewById<View>(R.id.sidebar_container)!!

                appTitleFrame.apply {
                    setOnClickListener {
                        appClickListener(appListItem)
                    }
                    setOnLongClickListener {
                        val openApp =
                            flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps
                        if (openApp) {
                            try {
                                appDelete.alpha =
                                    if (context.isSystemApp(appListItem.activityPackage)) 0.3f else 1.0f
                                appHideLayout.isVisible = true
                                sidebarContainer.isVisible = false
                                visibleHideLayouts.add(absoluteAdapterPosition)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        true
                    }
                }

                appInfo.apply {
                    isVisible = contextMenuFlags[5]
                    setOnClickListener {
                        appInfoListener(appListItem)
                    }
                }

                appDelete.apply {
                    isVisible = contextMenuFlags[6]
                    setOnClickListener {
                        appDeleteListener(appListItem)
                    }
                }

                appClose.apply {
                    setOnClickListener {
                        appHideLayout.isVisible = false
                        visibleHideLayouts.remove(absoluteAdapterPosition)
                        if (visibleHideLayouts.isEmpty()) {
                            sidebarContainer.isVisible = prefs.showAZSidebar
                        }
                    }
                }

                appPin.apply {
                    setOnClickListener {
                        val appName = appListItem.activityPackage
                        val updatedPinnedApps = prefs.pinnedApps.toMutableSet()

                        val isPinned = updatedPinnedApps.contains(appName)
                        AppLogger.d("AppListDebug", if (isPinned) "📌 Unpinning $appName" else "📌 Pinning $appName")

                        if (isPinned) {
                            updatedPinnedApps.remove(appName)
                            appPin.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.pin_off, 0, 0)
                            appPin.text = getLocalizedString(R.string.pin)
                        } else {
                            updatedPinnedApps.add(appName)
                            appPin.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.pin, 0, 0)
                            appPin.text = getLocalizedString(R.string.unpin)
                        }

                        prefs.pinnedApps = updatedPinnedApps.toSet()
                        AppLogger.d("AppListDebug", "✅ Updated pinnedApps: ${prefs.pinnedApps}")
                    }
                }
            }
    }
}
