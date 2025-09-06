/**
 * The view for the list of all the installed applications.
 */

package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.hasSoftKeyboard
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.openSearch
import com.github.droidworksstudio.common.searchCustomSearchEngine
import com.github.droidworksstudio.common.searchOnPlayStore
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppCategory
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentAppDrawerBinding
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.openAppInfo

class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        binding.apply {
            val layoutParams = sidebarContainer.layoutParams as RelativeLayout.LayoutParams

            // Clear old alignment rules
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)

            // Apply new alignment based on prefs
            when (prefs.drawerAlignment) {
                Constants.Gravity.Left -> layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                Constants.Gravity.Center,
                Constants.Gravity.Right -> layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
            }

            sidebarContainer.layoutParams = layoutParams
        }

        // Retrieve the letter key code from arguments
        val letterKeyCode = arguments?.getInt("letterKeyCode", -1)
        if (letterKeyCode != null && letterKeyCode != -1) {
            val letterToChar = convertKeyCodeToLetter(letterKeyCode)
            val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
            searchTextView.text = letterToChar.toString()
        }

        val backgroundColor = getHexForOpacity(prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)

        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString())
            ?: AppDrawerFlag.LaunchApp.toString()
        val flag = AppDrawerFlag.valueOf(flagString)
        val n = arguments?.getInt("n", 0) ?: 0

        when (flag) {
            AppDrawerFlag.SetDoubleTap,
            AppDrawerFlag.SetShortSwipeRight,
            AppDrawerFlag.SetShortSwipeLeft,
            AppDrawerFlag.SetShortSwipeUp,
            AppDrawerFlag.SetShortSwipeDown,
            AppDrawerFlag.SetLongSwipeRight,
            AppDrawerFlag.SetLongSwipeLeft,
            AppDrawerFlag.SetLongSwipeUp,
            AppDrawerFlag.SetLongSwipeDown,
            AppDrawerFlag.SetClickClock,
            AppDrawerFlag.SetAppUsage,
            AppDrawerFlag.SetClickDate,
            AppDrawerFlag.SetFloating -> {
                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }
            }

            AppDrawerFlag.SetHomeApp -> {
                // Get UserManager
                val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager

                val clearApp = AppListItem(
                    activityLabel = "Clear",
                    activityPackage = emptyString(),
                    activityClass = emptyString(),
                    user = userManager.userProfiles[0], // or use Process.myUserHandle() if it makes more sense
                    profileType = "SYSTEM",
                    customLabel = "Clear",
                    customTag = emptyString(),
                    category = AppCategory.REGULAR,
                    isHeader = false // if this is meant to act like a header or special row; else use false
                )

                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }

                binding.clearHomeButton.apply {
                    val currentApp = prefs.getHomeAppModel(n)
                    if (currentApp.activityPackage.isNotEmpty() && currentApp.activityClass.isNotEmpty()) {
                        isVisible = true
                        text = getLocalizedString(R.string.clear_home_app)
                        setTextColor(prefs.appColor)
                        textSize = prefs.appSize.toFloat()
                        setOnClickListener {
                            prefs.setHomeAppModel(n, clearApp)
                            findNavController().popBackStack()
                        }
                    }
                }
            }


            else -> {}
        }

        val viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.appScrollMap.observe(viewLifecycleOwner) { map ->
            binding.azSidebar.onLetterSelected = { section ->
                map[section]?.let { index ->
                    binding.recyclerView.smoothScrollToPosition(index)
                }
            }
        }

        val gravity = when (Prefs(requireContext()).drawerAlignment) {
            Constants.Gravity.Left -> Gravity.LEFT
            Constants.Gravity.Center -> Gravity.CENTER
            Constants.Gravity.Right -> Gravity.RIGHT
        }

        val appAdapter = context?.let {
            parentFragment?.let { fragment ->
                AppDrawerAdapter(
                    it,
                    fragment,
                    flag,
                    gravity,
                    appClickListener(viewModel, flag, n),
                    appDeleteListener(),
                    this.appRenameListener(),
                    this.appTagListener(),
                    appShowHideListener(),
                    appInfoListener()
                )
            }
        }

        if (appAdapter != null) {
            adapter = appAdapter
        }

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)

        val textSize = prefs.appSize.toFloat()
        searchTextView.textSize = textSize

        if (appAdapter != null) {
            initViewModel(flag, viewModel, appAdapter)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = appAdapter

        var lastSectionLetter: String? = null

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var onTop = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val itemCount = layoutManager.itemCount
                if (itemCount == 0) return

                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

                val position = when {
                    firstVisible <= 1 -> firstVisible
                    lastVisible >= itemCount - 2 -> lastVisible
                    else -> (firstVisible + lastVisible) / 2
                }.coerceIn(0, itemCount - 1)

                val item = appAdapter?.getItemAt(position) ?: return

                val sectionLetter = when (item.category) {
                    AppCategory.PINNED -> "★"
                    else -> item.label.firstOrNull()?.uppercaseChar()?.toString() ?: return
                }

                // Skip redundant updates
                if (sectionLetter == lastSectionLetter) return
                lastSectionLetter = sectionLetter

                binding.azSidebar.setSelectedLetter(sectionLetter)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop) {
                            if (requireContext().hasSoftKeyboard()) {
                                binding.search.hideKeyboard()
                            }
                        }
                        if (onTop && !recyclerView.canScrollVertically(1)) {
                            findNavController().popBackStack()
                        }
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1)) {
                            binding.search.hideKeyboard()
                        } else if (!recyclerView.canScrollVertically(-1)) {
                            if (onTop) {
                                findNavController().popBackStack()
                            } else {
                                if (requireContext().hasSoftKeyboard()) {
                                    binding.search.showKeyboard()
                                }
                            }
                        }
                    }
                }
            }
        })

        if (prefs.hideSearchView) {
            binding.search.isVisible = false
        } else {
            when (flag) {
                AppDrawerFlag.LaunchApp -> binding.search.queryHint =
                    applyTextColor(getLocalizedString(R.string.show_apps), prefs.appColor)

                AppDrawerFlag.HiddenApps -> binding.search.queryHint =
                    applyTextColor(getLocalizedString(R.string.hidden_apps), prefs.appColor)

                AppDrawerFlag.SetHomeApp -> binding.search.queryHint =
                    applyTextColor(getLocalizedString(R.string.please_select_app), prefs.appColor)

                else -> {}
            }
        }

        binding.listEmptyHint.text = applyTextColor(getLocalizedString(R.string.drawer_list_empty_hint), prefs.appColor)

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchQuery = query

                if (!searchQuery.isNullOrEmpty()) {
                    val trimmedQuery = searchQuery.trim()

                    when {
                        trimmedQuery.startsWith("!") -> {
                            val customQuery = trimmedQuery.substringAfter("!")
                            requireContext().searchCustomSearchEngine(customQuery, prefs)
                        }

                        adapter.itemCount >= 1 -> {
                            val firstItem = adapter.getFirstInList().toString()
                            if (firstItem.equals(trimmedQuery, ignoreCase = true) || prefs.openAppOnEnter) {
                                adapter.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(trimmedQuery)
                            }
                        }

                        adapter.itemCount == 0 -> {
                            val playStoreHandled = requireContext().searchOnPlayStore(trimmedQuery)
                            if (!playStoreHandled) {
                                requireContext().openSearch(trimmedQuery)
                            }
                        }

                        trimmedQuery.startsWith("#") -> {
                            return true
                        }

                        else -> {
                            if (prefs.openAppOnEnter) {
                                adapter.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(trimmedQuery)
                            }
                        }
                    }

                    return true

                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (flag == AppDrawerFlag.SetHomeApp) {
                    binding.drawerButton.apply {
                        isVisible = !newText.isNullOrEmpty()
                        text = if (isVisible) getLocalizedString(R.string.rename) else null
                        setOnClickListener { if (isVisible) renameListener(flag, n) }
                    }
                    binding.clearHomeButton.apply {
                        isVisible = newText.isNullOrEmpty()
                    }
                }
                newText?.let {
                    appAdapter?.filter?.filter(it.trim())
                }
                return false
            }

        })
    }

    private fun applyTextColor(text: String, color: Int): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }

    private fun convertKeyCodeToLetter(keyCode: Int): Char {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> 'A'
            KeyEvent.KEYCODE_B -> 'B'
            KeyEvent.KEYCODE_C -> 'C'
            KeyEvent.KEYCODE_D -> 'D'
            KeyEvent.KEYCODE_E -> 'E'
            KeyEvent.KEYCODE_F -> 'F'
            KeyEvent.KEYCODE_G -> 'G'
            KeyEvent.KEYCODE_H -> 'H'
            KeyEvent.KEYCODE_I -> 'I'
            KeyEvent.KEYCODE_J -> 'J'
            KeyEvent.KEYCODE_K -> 'K'
            KeyEvent.KEYCODE_L -> 'L'
            KeyEvent.KEYCODE_M -> 'M'
            KeyEvent.KEYCODE_N -> 'N'
            KeyEvent.KEYCODE_O -> 'O'
            KeyEvent.KEYCODE_P -> 'P'
            KeyEvent.KEYCODE_Q -> 'Q'
            KeyEvent.KEYCODE_R -> 'R'
            KeyEvent.KEYCODE_S -> 'S'
            KeyEvent.KEYCODE_T -> 'T'
            KeyEvent.KEYCODE_U -> 'U'
            KeyEvent.KEYCODE_V -> 'V'
            KeyEvent.KEYCODE_W -> 'W'
            KeyEvent.KEYCODE_X -> 'X'
            KeyEvent.KEYCODE_Y -> 'Y'
            KeyEvent.KEYCODE_Z -> 'Z'
            else -> throw IllegalArgumentException("Invalid key code: $keyCode")
        }
    }

    private fun initViewModel(
        flag: AppDrawerFlag,
        viewModel: MainViewModel,
        appAdapter: AppDrawerAdapter
    ) {
        viewModel.hiddenApps.observe(viewLifecycleOwner, Observer {
            if (flag != AppDrawerFlag.HiddenApps) return@Observer
            it?.let { appList ->
                binding.listEmptyHint.isVisible = appList.isEmpty()
                populateAppList(appList, appAdapter)
            }
        })

        viewModel.appList.observe(viewLifecycleOwner, Observer { rawList ->
            if (flag == AppDrawerFlag.HiddenApps) return@Observer
            if (rawList == appAdapter.appsList) return@Observer

            rawList?.let {
                val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager
                val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

                val classifiedList = rawList.map { app ->
                    val isPrivate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        // Only call getLauncherUserInfo if API supports it
                        val userInfo = launcherApps.getLauncherUserInfo(app.user)
                        userInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
                    } else {
                        false
                    }

                    val isWork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Use isManagedProfile only if the API supports it
                        userManager.isManagedProfile && !isPrivate
                    } else {
                        false
                    }

                    val isSystemUser = userManager.isSystemUser

                    val profileType = when {
                        isPrivate -> "PRIVATE"
                        isWork -> "WORK"
                        isSystemUser -> "SYSTEM"
                        else -> "NULL"
                    }

                    app.copy(profileType = profileType)
                }

                AppLogger.d("classifiedList", "Classified app list: $classifiedList")

                val systemApps = classifiedList.filter { it.profileType == "SYSTEM" }
                val workApps = classifiedList.filter { it.profileType == "WORK" }
                val privateApps = classifiedList.filter { it.profileType == "PRIVATE" }

                val mergedList = mutableListOf<AppListItem>()

                if (systemApps.isNotEmpty()) {
                    if (workApps.isNotEmpty() || privateApps.isNotEmpty()) {
                        mergedList.add(
                            AppListItem(
                                "Personal apps",
                                "app.mlauncher.system",
                                "app.mlauncher.system",
                                systemApps.first().user,
                                profileType = "HEADER",
                                customLabel = emptyString(),
                                customTag = emptyString(),
                                category = AppCategory.REGULAR,
                                isHeader = true
                            )
                        )
                    }
                    mergedList.addAll(systemApps)
                }
                if (privateApps.isNotEmpty()) {
                    mergedList.add(
                        AppListItem(
                            "Private space",
                            "app.mlauncher.private",
                            "app.mlauncher.private",
                            privateApps.first().user,
                            profileType = "HEADER",
                            customLabel = emptyString(),
                            customTag = emptyString(),
                            category = AppCategory.REGULAR,
                            isHeader = true
                        )
                    )
                    mergedList.addAll(privateApps)
                }
                if (workApps.isNotEmpty()) {
                    mergedList.add(
                        AppListItem(
                            "Work profile",
                            "app.mlauncher.work",
                            "app.mlauncher.work",
                            workApps.first().user,
                            profileType = "HEADER",
                            customLabel = emptyString(),
                            customTag = emptyString(),
                            category = AppCategory.REGULAR,
                            isHeader = true
                        )
                    )
                    mergedList.addAll(workApps)
                }

                binding.listEmptyHint.isVisible = mergedList.isEmpty()
                binding.sidebarContainer.isVisible = prefs.showAZSidebar
                populateAppList(mergedList, appAdapter)
            }
        })

        viewModel.firstOpen.observe(viewLifecycleOwner) {
            if (it) binding.appDrawerTip.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireContext().hasSoftKeyboard()) {
            binding.search.showKeyboard()
        }
    }

    override fun onStop() {
        super.onStop()
        if (requireContext().hasSoftKeyboard()) {
            binding.search.hideKeyboard()
        }
    }


    private fun View.showKeyboard() {
        if (!Prefs(requireContext()).autoShowKeyboard) return
        if (Prefs(requireContext()).hideSearchView) return

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
        searchTextView.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        searchTextView.postDelayed({
            searchTextView.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(searchTextView, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun View.hideKeyboard() {
        val imm: InputMethodManager? =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(windowToken, 0)
        this.clearFocus()
    }


    private fun populateAppList(apps: List<AppListItem>, appAdapter: AppDrawerAdapter) {
        val animation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        binding.recyclerView.layoutAnimation = animation
        appAdapter.setAppList(apps.toMutableList())
    }

    private fun appClickListener(
        viewModel: MainViewModel,
        flag: AppDrawerFlag,
        n: Int = 0
    ): (appListItem: AppListItem) -> Unit =
        { appModel ->
            viewModel.selectedApp(this, appModel, flag, n)
            if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps)
                findNavController().popBackStack(R.id.mainFragment, false)
            else
                findNavController().popBackStack()
        }

    private fun appDeleteListener(): (appListItem: AppListItem) -> Unit =
        { appModel ->
            if (requireContext().isSystemApp(appModel.activityPackage))
                showShortToast(getLocalizedString(R.string.can_not_delete_system_apps))
            else {
                val appPackage = appModel.activityPackage
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = "package:$appPackage".toUri()
                requireContext().startActivity(intent)
            }

        }

    private fun appRenameListener(): (appPackage: String, appAlias: String) -> Unit =
        { appPackage, appAlias ->
            val prefs = Prefs(requireContext())
            prefs.setAppAlias(appPackage, appAlias)
            findNavController().popBackStack()
        }

    private fun appTagListener(): (appPackage: String, appTag: String, appUser: UserHandle) -> Unit =
        { appPackage, appTag, appUser ->
            val prefs = Prefs(requireContext())
            prefs.setAppTag(appPackage, appTag, appUser)
            findNavController().popBackStack()
        }

    private fun renameListener(flag: AppDrawerFlag, i: Int) {
        val name = binding.search.query.toString().trim()
        if (name.isEmpty()) return
        if (flag == AppDrawerFlag.SetHomeApp) {
            Prefs(requireContext()).setHomeAppName(i, name)
        }

        findNavController().popBackStack()
    }

    private fun appShowHideListener(): (flag: AppDrawerFlag, appListItem: AppListItem) -> Unit =
        { flag, appModel ->
            val prefs = Prefs(requireContext())
            val newSet = mutableSetOf<String>()
            newSet.addAll(prefs.hiddenApps)

            if (flag == AppDrawerFlag.HiddenApps) {
                newSet.remove(appModel.activityPackage) // for backward compatibility
                newSet.remove(appModel.activityPackage + "|" + appModel.user.hashCode()) // for backward compatibility
                newSet.remove(appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode())
            } else {
                newSet.add(appModel.activityPackage + "|" + appModel.activityClass + "|" + appModel.user.hashCode())
            }

            prefs.hiddenApps = newSet

            if (newSet.isEmpty()) findNavController().popBackStack()
        }

    private fun appInfoListener(): (appListItem: AppListItem) -> Unit =
        { appModel ->
            openAppInfo(
                requireContext(),
                appModel.user,
                appModel.activityPackage
            )
            findNavController().popBackStack(R.id.mainFragment, false)
        }
}