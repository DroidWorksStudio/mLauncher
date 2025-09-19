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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.hasSoftKeyboard
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.searchCustomSearchEngine
import com.github.droidworksstudio.common.searchOnPlayStore
import com.github.droidworksstudio.common.showShortToast
import com.github.droidworksstudio.mlauncher.MainViewModel
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppCategory
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Constants.AppDrawerFlag
import com.github.droidworksstudio.mlauncher.data.ContactListItem
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.FragmentAppDrawerBinding
import com.github.droidworksstudio.mlauncher.helper.emptyString
import com.github.droidworksstudio.mlauncher.helper.getHexForOpacity
import com.github.droidworksstudio.mlauncher.helper.hasContactsPermission
import com.github.droidworksstudio.mlauncher.helper.openAppInfo
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.ui.adapter.AppDrawerAdapter
import com.github.droidworksstudio.mlauncher.ui.adapter.ContactDrawerAdapter

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var appsAdapter: AppDrawerAdapter
    private lateinit var contactsAdapter: ContactDrawerAdapter

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

            searchSwitcher.setOnClickListener { switchMenus() }
            menuView.displayedChild = 0
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

        val profileType = arguments?.getString("profileType", null)

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
                    category = AppCategory.REGULAR
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

        val combinedScrollMaps = MediatorLiveData<Pair<Map<String, Int>, Map<String, Int>>>()

        combinedScrollMaps.addSource(viewModel.appScrollMap) { appMap ->
            combinedScrollMaps.value = Pair(appMap, viewModel.contactScrollMap.value ?: emptyMap())
        }
        combinedScrollMaps.addSource(viewModel.contactScrollMap) { contactMap ->
            combinedScrollMaps.value = Pair(viewModel.appScrollMap.value ?: emptyMap(), contactMap)
        }


        combinedScrollMaps.observe(viewLifecycleOwner) { (appMap, contactMap) ->
            binding.azSidebar.onLetterSelected = { section ->
                when (binding.menuView.displayedChild) {
                    0 -> appMap[section]?.let { index ->
                        binding.appsRecyclerView.smoothScrollToPosition(index)
                    }

                    1 -> contactMap[section]?.let { index ->
                        binding.contactsRecyclerView.smoothScrollToPosition(index)
                    }
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

        val contactAdapter = context?.let {
            parentFragment?.let { fragment ->
                ContactDrawerAdapter(
                    it,
                    gravity,
                    contactClickListener(viewModel, n)
                )
            }
        }


        when (binding.menuView.displayedChild) {
            0 -> appAdapter?.let { appsAdapter = it }
            1 -> contactAdapter?.let { contactsAdapter = it }
        }

        val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)

        val textSize = prefs.appSize.toFloat()
        searchTextView.textSize = textSize

        if (appAdapter != null && contactAdapter != null) {
            initViewModel(flag, viewModel, appAdapter, contactAdapter, profileType)
        }

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.appsRecyclerView.adapter = appAdapter
        binding.contactsRecyclerView.adapter = contactAdapter

        var lastSectionLetter: String? = null

        binding.appsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

        binding.contactsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

                val item = contactAdapter?.getItemAt(position) ?: return

                val sectionLetter = item.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: return

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
            val appListButtonFlags = prefs.getMenuFlags("APPLIST_BUTTON_FLAGS", "00")
            when (flag) {
                AppDrawerFlag.LaunchApp -> {
                    when (profileType) {
                        "WORK" -> binding.search.queryHint = getLocalizedString(R.string.show_work_apps)
                        "PRIVATE" -> binding.search.queryHint = getLocalizedString(R.string.show_private_apps)
                        else -> binding.search.queryHint = getLocalizedString(R.string.show_apps)
                    }
                    binding.workApps.apply {
                        isVisible = ((prefs.getProfileCounter("WORK") > 0) && profileType != "WORK")
                        setOnClickListener {
                            findNavController().navigate(
                                R.id.action_appListFragment_to_appListFragment,
                                bundleOf("flag" to flag.toString(), "n" to view.id, "profileType" to "WORK")
                            )
                        }
                    }

                    binding.privateApps.apply {
                        isVisible = ((prefs.getProfileCounter("PRIVATE") > 0) && !PrivateSpaceManager(requireContext()).isPrivateSpaceLocked() && profileType != "PRIVATE")
                        setOnClickListener {
                            findNavController().navigate(
                                R.id.action_appListFragment_to_appListFragment,
                                bundleOf("flag" to flag.toString(), "n" to view.id, "profileType" to "PRIVATE")
                            )
                        }
                    }

                    binding.systemApps.apply {
                        isVisible = ((prefs.getProfileCounter("SYSTEM") > 0) && profileType != "SYSTEM")
                        setOnClickListener {
                            findNavController().navigate(
                                R.id.action_appListFragment_to_appListFragment,
                                bundleOf("flag" to flag.toString(), "n" to view.id, "profileType" to "SYSTEM")
                            )
                        }
                    }
                    binding.internetSearch.apply {
                        isVisible = appListButtonFlags[0]
                        setOnClickListener {
                            val query = binding.search.query.toString().trim()
                            if (query.isEmpty()) return@setOnClickListener
                            requireContext().searchCustomSearchEngine(query, prefs)
                        }
                    }
                    binding.searchSwitcher.apply {
                        if (hasContactsPermission(context)) {
                            isVisible = appListButtonFlags[1]
                        } else {
                            binding.menuView.displayedChild = 0
                        }
                    }
                }

                AppDrawerFlag.HiddenApps -> {
                    binding.search.queryHint = getLocalizedString(R.string.hidden_apps)
                }

                AppDrawerFlag.SetHomeApp -> {
                    binding.search.queryHint = getLocalizedString(R.string.please_select_app)
                }

                else -> {}
            }
        }

        binding.listEmptyHint.text = applyTextColor(getLocalizedString(R.string.drawer_list_empty_hint), prefs.appColor)

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchQuery = query?.trim()

                if (!searchQuery.isNullOrEmpty()) {

                    // Hashtag shortcut
                    if (searchQuery.startsWith("#")) return true

                    when (binding.menuView.displayedChild) {
                        0 -> { // appsAdapter
                            val firstItem = appAdapter?.getFirstInList()
                            if (firstItem.equals(searchQuery, ignoreCase = true) || prefs.openAppOnEnter) {
                                appAdapter?.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(searchQuery)
                            }
                        }

                        1 -> { // contactsAdapter
                            val firstItem = contactAdapter?.getFirstInList()
                            if (firstItem.equals(searchQuery, ignoreCase = true) || prefs.openAppOnEnter) {
                                contactAdapter?.launchFirstInList()
                            } else {
                                requireContext().searchOnPlayStore(searchQuery)
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
                    when (binding.menuView.displayedChild) {
                        0 -> appAdapter?.filter?.filter(it.trim())
                        1 -> contactAdapter?.filter?.filter(it.trim())
                    }
                }
                return false
            }
        })
    }

    fun switchMenus() {
        binding.apply {
            menuView.showNext()
            when (menuView.displayedChild) {
                0 -> {
                    setAppViewDetails()
                }

                1 -> {
                    setContactViewDetails()
                }
            }
        }
    }

    private fun setAppViewDetails() {
        binding.apply {
            searchSwitcher.setImageResource(R.drawable.ic_contacts)
            search.queryHint = getLocalizedString(R.string.show_apps)
            search.setQuery("", false)
        }
    }

    private fun setContactViewDetails() {
        binding.apply {
            searchSwitcher.setImageResource(R.drawable.ic_apps)
            search.queryHint = getLocalizedString(R.string.show_contacts)
            search.setQuery("", false)
        }
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
        appAdapter: AppDrawerAdapter,
        contactAdapter: ContactDrawerAdapter,
        profileFilter: String? = null // 🔹 pass "PRIVATE", "WORK", "SYSTEM", or null for all
    ) {
        viewModel.hiddenApps.observe(viewLifecycleOwner, Observer {
            if (flag != AppDrawerFlag.HiddenApps) return@Observer
            it?.let { appList ->
                binding.listEmptyHint.isVisible = appList.isEmpty()
                populateAppList(appList, appAdapter)
            }
        })

        // Observe contacts
        viewModel.contactList.observe(viewLifecycleOwner, Observer { rawContactList ->
            if (rawContactList == contactAdapter.contactsList) return@Observer
            if (binding.menuView.displayedChild != 0) return@Observer

            AppLogger.d("rawContactList", "Contact list: $rawContactList")

            rawContactList?.let {
                binding.listEmptyHint.isVisible = it.isEmpty()
                binding.sidebarContainer.isVisible = prefs.showAZSidebar
                populateContactList(it, contactAdapter)
            }
        })


        // Observe apps
        viewModel.appList.observe(viewLifecycleOwner, Observer { rawAppList ->
            if (flag == AppDrawerFlag.HiddenApps) return@Observer
            if (rawAppList == appAdapter.appsList) return@Observer
            if (binding.menuView.displayedChild != 0) return@Observer

            AppLogger.d("rawAppList", "Contact list: $rawAppList")

            rawAppList?.let {
                val userManager = requireContext().getSystemService(Context.USER_SERVICE) as UserManager
                val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

                val classifiedList = rawAppList.map { app ->
                    val isPrivate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        val userInfo = launcherApps.getLauncherUserInfo(app.user)
                        userInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
                    } else false

                    val isWork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        userManager.isManagedProfile && !isPrivate
                    } else false

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

                // 🔹 Filter by requested profile type
                val filteredList = when (profileFilter) {
                    "PRIVATE" -> classifiedList.filter { it.profileType == "PRIVATE" }
                    "WORK" -> classifiedList.filter { it.profileType == "WORK" }
                    "SYSTEM" -> classifiedList.filter { it.profileType == "SYSTEM" }
                    else -> classifiedList // no filter, show all
                }

                // 🔹 Build merged list with headers, but only from filtered apps
                val systemApps = filteredList.filter { it.profileType == "SYSTEM" }
                val workApps = filteredList.filter { it.profileType == "WORK" }
                val privateApps = filteredList.filter { it.profileType == "PRIVATE" }

                val mergedList = mutableListOf<AppListItem>()

                if (systemApps.isNotEmpty()) {
                    mergedList.addAll(systemApps)
                }

                if (privateApps.isNotEmpty()) {
                    mergedList.addAll(privateApps)
                }

                if (workApps.isNotEmpty()) {
                    mergedList.addAll(workApps)
                }

                // UI updates
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
        binding.appsRecyclerView.layoutAnimation = animation
        appAdapter.setAppList(apps.toMutableList())
    }

    private fun populateContactList(contacts: List<ContactListItem>, contactAdapter: ContactDrawerAdapter) {
        val animation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
        binding.contactsRecyclerView.layoutAnimation = animation
        contactAdapter.setContactList(contacts.toMutableList())
    }

    private fun appClickListener(
        viewModel: MainViewModel,
        flag: AppDrawerFlag,
        n: Int = 0
    ): (appListItem: AppListItem) -> Unit = { appModel ->
        viewModel.selectedApp(this, appModel, flag, n)
        if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps)
            findNavController().popBackStack(R.id.mainFragment, false)
        else
            findNavController().popBackStack()
    }

    private fun appDeleteListener(): (appListItem: AppListItem) -> Unit = { appModel ->
        if (requireContext().isSystemApp(appModel.activityPackage))
            showShortToast(getLocalizedString(R.string.can_not_delete_system_apps))
        else {
            val appPackage = appModel.activityPackage
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = "package:$appPackage".toUri()
            requireContext().startActivity(intent)
        }

    }

    private fun appRenameListener(): (appPackage: String, appAlias: String) -> Unit = { appPackage, appAlias ->
        val prefs = Prefs(requireContext())
        prefs.setAppAlias(appPackage, appAlias)
        findNavController().popBackStack()
    }

    private fun appTagListener(): (appPackage: String, appTag: String, appUser: UserHandle) -> Unit = { appPackage, appTag, appUser ->
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

    private fun appShowHideListener(): (flag: AppDrawerFlag, appListItem: AppListItem) -> Unit = { flag, appModel ->
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

    private fun appInfoListener(): (appListItem: AppListItem) -> Unit = { appModel ->
        openAppInfo(
            requireContext(),
            appModel.user,
            appModel.activityPackage
        )
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    // Handles click on a contact item
    private fun contactClickListener(
        viewModel: MainViewModel,
        n: Int = 0
    ): (contactItem: ContactListItem) -> Unit = { contactModel ->
        viewModel.selectedContact(this, contactModel, n)
        // Close the drawer or fragment after selection
        findNavController().popBackStack()
    }
}