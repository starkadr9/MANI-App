package com.simplemobiletools.calendar.pro.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.simplemobiletools.calendar.pro.BuildConfig
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.PoeticEddaActivity
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.adapters.QuickFilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.databases.EventsDatabase
import com.simplemobiletools.calendar.pro.databinding.ActivityMainBinding
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypesDialog
import com.simplemobiletools.calendar.pro.dialogs.SetRemindersDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.fragments.*
import com.simplemobiletools.calendar.pro.fragments.LunisolarMonthFragment
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.IcsImporter.ImportResult
import com.simplemobiletools.calendar.pro.jobs.CalDAVUpdateListener
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.calendar.pro.models.ListItem
import com.simplemobiletools.calendar.pro.models.ListSectionDay
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mLatestSearchQuery = ""
    private var currentFragments = ArrayList<MyFragmentHolder>()
    private var isSearchOpen = false

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredFirstDayOfWeek = 0
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredDimCompletedTasks = true
    private var mStoredHighlightWeekends = false
    private var mStoredStartWeekWithCurrentDay = false
    private var mStoredHighlightWeekendsColor = 0

    // search results have endless scrolling, so reaching the top/bottom fetches further results
    private var minFetchedSearchTS = 0L
    private var maxFetchedSearchTS = 0L
    private var searchResultEvents = ArrayList<Event>()
    private var bottomItemAtRefresh: ListItem? = null

    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // NORSE MOD: Disable all potential rating/promotional dialogs
        // This prevents any launch counting or promotional popups from the commons library
        
        // Override any potential rating dialog settings
        try {
            // Disable rating dialog via config if available
            config.wasAppRated = true
            config.appRunCount = 999  // Set high count to avoid "rate after X launches" logic
        } catch (e: Exception) {
            // Config properties might not exist in this version, ignore
        }
        
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = true)

        binding.calendarFab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        binding.calendarFab.setOnClickListener {
            if (config.allowCreatingTasks) {
                if (binding.fabExtendedOverlay.isVisible()) {
                    openNewEvent()

                    Handler().postDelayed({
                        hideExtendedFab()
                    }, 300)
                } else {
                    showExtendedFab()
                }
            } else {
                openNewEvent()
            }
        }
        binding.fabEventLabel.setOnClickListener { openNewEvent() }
        binding.fabTaskLabel.setOnClickListener { openNewTask() }

        binding.fabExtendedOverlay.setOnClickListener {
            hideExtendedFab()
        }

        binding.fabTaskIcon.setOnClickListener {
            openNewTask()

            Handler().postDelayed({
                hideExtendedFab()
            }, 300)
        }

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshCalDAVCalendars(true)
        }

        checkIsViewIntent()

        if (!checkIsOpenIntent()) {
            updateViewPager()
        }

        // NORSE MOD: Disabled rating popup and SD card check
        // checkAppOnSDCard()

        if (savedInstanceState == null) {
            checkCalDAVUpdateListener()
        }

        if (isPackageInstalled("com.simplemobiletools.calendar")) {
            ConfirmationDialog(
                activity = this,
                message = "",
                messageId = com.simplemobiletools.commons.R.string.upgraded_from_free_calendar,
                positive = com.simplemobiletools.commons.R.string.ok,
                negative = 0,
                cancelOnTouchOutside = false
            ) {}
        }

        addImportIdsToTasks {
            refreshViewPager()
        }

        // Setup Navigation Drawer
        setupNavigationDrawer()
    }

    override fun onResume() {
        super.onResume()
        
        // NORSE MOD: Additional protection against rating dialogs
        if (mStoredTextColor != getProperTextColor() || mStoredBackgroundColor != getProperBackgroundColor() || mStoredPrimaryColor != getProperPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredDimCompletedTasks != config.dimCompletedTasks
            || mStoredHighlightWeekends != config.highlightWeekends || mStoredHighlightWeekendsColor != config.highlightWeekendsColor
        ) {
            updateViewPager()
        }

        eventsHelper.getEventTypes(this, false) {
            val newShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
            if (newShouldFilterBeVisible != mShouldFilterBeVisible) {
                mShouldFilterBeVisible = newShouldFilterBeVisible
                refreshMenuItems()
            }
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredFirstDayOfWeek != config.firstDayOfWeek || mStoredUse24HourFormat != config.use24HourFormat
                || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop || mStoredStartWeekWithCurrentDay != config.startWeekWithCurrentDay
            ) {
                updateViewPager()
            }
        }

        updateStatusbarColor(getProperBackgroundColor())
        binding.apply {
            mainToolbar.setBackgroundColor(getProperPrimaryColor())
            storeStateVariables()
            updateTextColors(calendarCoordinator)
            fabExtendedOverlay.background = ColorDrawable(getProperBackgroundColor().adjustAlpha(0.8f))
            fabEventLabel.setTextColor(getProperTextColor())
            fabTaskLabel.setTextColor(getProperTextColor())

            fabTaskIcon.drawable.applyColorFilter(mStoredPrimaryColor.getContrastColor())
            fabTaskIcon.background.applyColorFilter(mStoredPrimaryColor)

            searchHolder.background = ColorDrawable(getProperBackgroundColor())
            checkSwipeRefreshAvailability()
            checkShortcuts()

            if (!isSearchOpen) {
                refreshMenuItems()
            }
        }

        setupQuickFilter()

        if (config.caldavSync) {
            updateCalDAVEvents()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            EventsDatabase.destroyInstance()
            stopCalDAVUpdateListener()
        }
    }

    fun refreshMenuItems() {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        binding.mainToolbar.menu.apply {
            findItem(R.id.filter)?.isVisible = mShouldFilterBeVisible
            findItem(R.id.refresh_caldav_calendars)?.isVisible = config.caldavSync
        }
    }

    private fun setupOptionsMenu() = binding.apply {
        // Set up main toolbar
        setSupportActionBar(mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    override fun onBackPressed() {
        android.util.Log.d("MainActivity", "onBackPressed: currentFragments.size = ${currentFragments.size}")
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            checkSwipeRefreshAvailability()
            when {
                binding.fabExtendedOverlay.isVisible() -> hideExtendedFab()
                currentFragments.size > 1 -> {
                    android.util.Log.d("MainActivity", "Removing top fragment")
                    removeTopFragment()
                }
                else -> {
                    android.util.Log.d("MainActivity", "Calling super.onBackPressed() - will exit app")
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        return when (item.itemId) {
            android.R.id.home -> {
                if (currentFragments.size > 1) {
                    onBackPressed()
                    true
                } else {
                    false
                }
            }
            R.id.change_view -> {
                showViewDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    private fun storeStateVariables() {
        mStoredTextColor = getProperTextColor()
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredBackgroundColor = getProperBackgroundColor()
        config.apply {
            mStoredFirstDayOfWeek = firstDayOfWeek
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredDimCompletedTasks = dimCompletedTasks
            mStoredHighlightWeekends = highlightWeekends
            mStoredHighlightWeekendsColor = highlightWeekendsColor
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun setupQuickFilter() {
        eventsHelper.getEventTypes(this, false) {
            val quickFilterEventTypes = config.quickFilterEventTypes
            binding.quickEventTypeFilter.adapter = QuickFilterEventTypeAdapter(this, it, quickFilterEventTypes) {
                if (config.displayEventTypes.isEmpty() && !config.wasFilteredOutWarningShown) {
                    toast(R.string.everything_filtered_out, Toast.LENGTH_LONG)
                    config.wasFilteredOutWarningShown = true
                }

                refreshViewPager()
            }
        }
    }

    // Search functionality simplified - closeSearch() method removed

    private fun checkCalDAVUpdateListener() {
        if (isNougatPlus()) {
            val updateListener = CalDAVUpdateListener()
            if (config.caldavSync) {
                if (!updateListener.isScheduled(applicationContext)) {
                    updateListener.scheduleJob(applicationContext)
                }
            } else {
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun stopCalDAVUpdateListener() {
        if (isNougatPlus()) {
            if (!config.caldavSync) {
                val updateListener = CalDAVUpdateListener()
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val newEvent = getNewEventShortcut(appIconColor)
            val shortcuts = arrayListOf(newEvent)

            if (config.allowCreatingTasks) {
                shortcuts.add(getNewTaskShortcut(appIconColor))
            }

            try {
                shortcutManager.dynamicShortcuts = shortcuts
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getNewEventShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_event)
        val newEventDrawable = resources.getDrawable(R.drawable.shortcut_event, theme)
        (newEventDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_event_background).applyColorFilter(appIconColor)
        val newEventBitmap = newEventDrawable.convertToBitmap()

        val newEventIntent = Intent(this, SplashActivity::class.java)
        newEventIntent.action = SHORTCUT_NEW_EVENT
        return ShortcutInfo.Builder(this, "new_event")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(newEventBitmap))
            .setIntent(newEventIntent)
            .build()
    }

    @SuppressLint("NewApi")
    private fun getNewTaskShortcut(appIconColor: Int): ShortcutInfo {
        val newTask = getString(R.string.new_task)
        val newTaskDrawable = resources.getDrawable(R.drawable.shortcut_task, theme)
        (newTaskDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_task_background).applyColorFilter(appIconColor)
        val newTaskBitmap = newTaskDrawable.convertToBitmap()
        val newTaskIntent = Intent(this, SplashActivity::class.java)
        newTaskIntent.action = SHORTCUT_NEW_TASK
        return ShortcutInfo.Builder(this, "new_task")
            .setShortLabel(newTask)
            .setLongLabel(newTask)
            .setIcon(Icon.createWithBitmap(newTaskBitmap))
            .setIntent(newTaskIntent)
            .build()
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, DAILY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            binding.calendarFab.beVisible()
            if (viewToOpen != LAST_VIEW) {
                config.storedView = viewToOpen
            }
            updateViewPager(dayCodeToOpen)
            return true
        }

        val eventIdToOpen = intent.getLongExtra(EVENT_ID, 0L)
        val eventOccurrenceToOpen = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        if (eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
            hideKeyboard()
            Intent(this, EventActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }

        return false
    }

    private fun checkIsViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.authority?.equals("com.android.calendar") == true || uri?.authority?.substringAfter("@") == "com.android.calendar") {
                if (uri.path!!.startsWith("/events")) {
                    ensureBackgroundThread {
                        // intents like content://com.android.calendar/events/1756
                        val eventId = uri.lastPathSegment
                        val id = eventsDB.getEventIdWithLastImportId("%-$eventId")
                        if (id != null) {
                            hideKeyboard()
                            Intent(this, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, id)
                                startActivity(this)
                            }
                        } else {
                            toast(R.string.caldav_event_not_found, Toast.LENGTH_LONG)
                        }
                    }
                } else if (uri.path!!.startsWith("/time") || intent?.extras?.getBoolean("DETAIL_VIEW", false) == true) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    // or content://0@com.android.calendar/time/1584958526435
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return
                    }
                }
            } else {
                tryImportEventsFromFile(uri!!) {
                    if (it) {
                        runOnUiThread {
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
        }
    }

    private fun showViewDialog() {
        val items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view))
        )

        RadioGroupDialog(this, items, config.storedView) {
            resetActionBarTitle()
            updateView(it as Int)
            refreshMenuItems()
        }
    }

    fun showGoToDateDialog() {
        currentFragments.last().showGoToDateDialog()
    }

    private fun printView() {
        currentFragments.last().printView()
    }

    private fun resetActionBarTitle() {
        binding.mainToolbar.title = getString(R.string.app_launcher_name)
    }

    private fun showFilterDialog() {
        SelectEventTypesDialog(this, config.displayEventTypes) {
            if (config.displayEventTypes != it) {
                config.displayEventTypes = it

                refreshViewPager()
                setupQuickFilter()
            }
        }
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        // Stub method for compatibility - go to today button has been removed
        // This method is called by fragments but we no longer use it
    }

    private fun updateCalDAVEvents() {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(showToasts = false, scheduleNextSync = true) {
                refreshViewPager()
            }
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }
        updateCalDAVEvents()
        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                calDAVChanged()
            }
        }
    }

    private fun calDAVChanged() {
        refreshViewPager()
        if (showCalDAVRefreshToast) {
            toast(R.string.refreshing_complete)
        }
        runOnUiThread {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateView(view: Int) {
        binding.calendarFab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDisplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
    }

    private fun getDateCodeToDisplay(newView: Int): String? {
        val fragment = currentFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex = viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex = viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getFirstDayOfWeek(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun updateViewPager(dayCode: String? = null) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it).commitNow()
            } catch (ignored: Exception) {
                return
            }
        }

        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(WEEK_START_DATE_TIME, fixedDayCode ?: getFirstDayOfWeek(DateTime()))
            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getFirstDayOfWeek(Formatter.getDateTimeFromCode(dayCode))
        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(dayCode)
        else -> dayCode
    }

    private fun showExtendedFab() {
        animateFabIcon(false)
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeIn()
            }
        }
    }

    private fun hideExtendedFab() {
        animateFabIcon(true)
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeOut()
            }
        }
    }

    private fun animateFabIcon(showPlus: Boolean) {
        val newDrawableId = if (showPlus) {
            com.simplemobiletools.commons.R.drawable.ic_plus_vector
        } else {
            R.drawable.ic_today_vector
        }
        val newDrawable = resources.getColoredDrawableWithColor(newDrawableId, getProperPrimaryColor())
        binding.calendarFab.setImageDrawable(newDrawable)
    }

    private fun openNewEvent() {
        hideKeyboard()
        val lastFragment = currentFragments.last()
        val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        launchNewEventIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
    }

    private fun openNewTask() {
        hideKeyboard()
        val lastFragment = currentFragments.last()
        val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        launchNewTaskIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
    }

    fun openMonthFromYearly(dateTime: DateTime) {
        if (currentFragments.last() is MonthFragmentsHolder) {
            return
        }

        val fragment = MonthFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        resetActionBarTitle()
        binding.calendarFab.beVisible()
        showBackNavigationArrow()
    }

    fun openDayFromMonthly(dateTime: DateTime) {
        android.util.Log.d("MainActivity", "openDayFromMonthly: currentFragments.size before = ${currentFragments.size}")
        if (currentFragments.last() is DayFragmentsHolder) {
            android.util.Log.d("MainActivity", "Already showing day view, returning")
            return
        }

        val fragment = DayFragmentsHolder()
        currentFragments.add(fragment)
        android.util.Log.d("MainActivity", "openDayFromMonthly: currentFragments.size after add = ${currentFragments.size}")
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        try {
            supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
            showBackNavigationArrow()
            android.util.Log.d("MainActivity", "Day view added successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error adding day view fragment", e)
        }
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> {
            if (config.useLunisolarCalendar) {
                // Use our custom lunisolar fragment
                LunisolarMonthFragment()
            } else {
                MonthFragmentsHolder()
            }
        }
        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        EVENTS_LIST_VIEW -> EventListFragment()
        else -> MonthFragmentsHolder()
    }

    private fun removeTopFragment() {
        android.util.Log.d("MainActivity", "removeTopFragment: currentFragments.size before = ${currentFragments.size}")
        supportFragmentManager.beginTransaction().remove(currentFragments.last()).commit()
        currentFragments.removeAt(currentFragments.size - 1)
        android.util.Log.d("MainActivity", "removeTopFragment: currentFragments.size after = ${currentFragments.size}")
        currentFragments.last().apply {
            refreshEvents()
        }

        binding.calendarFab.beGoneIf(currentFragments.size == 1 && config.storedView == YEARLY_VIEW)
        if (currentFragments.size > 1) {
            android.util.Log.d("MainActivity", "Still multiple fragments, showing back arrow")
            showBackNavigationArrow()
        } else {
            android.util.Log.d("MainActivity", "Back to single fragment, restoring hamburger menu")
            // Back to main view - restore hamburger menu by resetting the drawer toggle
            val toggle = ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.mainToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            binding.drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        }
    }

    private fun showBackNavigationArrow() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text

        if (text.isNotEmpty() && binding.searchHolder.isGone()) {
            binding.searchHolder.fadeIn()
        } else if (text.isEmpty()) {
            binding.searchHolder.fadeOut()
            binding.searchResultsList.adapter = null
        }

        val placeholderTextId = if (config.displayEventTypes.isEmpty()) {
            R.string.everything_filtered_out
        } else {
            com.simplemobiletools.commons.R.string.no_items_found
        }

        binding.searchPlaceholder.setText(placeholderTextId)
        binding.searchPlaceholder2.beVisibleIf(text.length == 1)
        if (text.length >= 2) {
            if (binding.searchResultsList.adapter == null) {
                minFetchedSearchTS = DateTime().minusYears(2).seconds()
                maxFetchedSearchTS = DateTime().plusYears(2).seconds()
            }

            eventsHelper.getEvents(minFetchedSearchTS, maxFetchedSearchTS, searchQuery = text) { events ->
                if (text == mLatestSearchQuery) {
                    // if we have less than MIN_EVENTS_THRESHOLD events, search again by extending the time span
                    showSearchResultEvents(events, INITIAL_EVENTS)

                    if (events.size < MIN_EVENTS_TRESHOLD) {
                        minFetchedSearchTS = 0L
                        maxFetchedSearchTS = MAX_SEARCH_YEAR

                        eventsHelper.getEvents(minFetchedSearchTS, maxFetchedSearchTS, searchQuery = text) { events ->
                            events.forEach { event ->
                                try {
                                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                                        searchResultEvents.add(0, event)
                                    }
                                } catch (ignored: ConcurrentModificationException) {
                                }
                            }

                            showSearchResultEvents(searchResultEvents, INITIAL_EVENTS)
                        }
                    }
                }
            }
        } else if (text.length == 1) {
            binding.searchPlaceholder.beVisible()
            binding.searchResultsList.beGone()
        }
    }

    private fun showSearchResultEvents(events: ArrayList<Event>, updateStatus: Int) {
        val currentSearchQuery = mLatestSearchQuery
        val filtered = try {
            events.filter {
                it.title.contains(currentSearchQuery, true) || it.location.contains(currentSearchQuery, true) || it.description.contains(
                    currentSearchQuery,
                    true
                )
            }
        } catch (e: ConcurrentModificationException) {
            return
        }

        searchResultEvents = filtered.toMutableList() as ArrayList<Event>
        runOnUiThread {
            binding.searchResultsList.beVisibleIf(filtered.isNotEmpty())
            binding.searchPlaceholder.beVisibleIf(filtered.isEmpty())
            val listItems = getEventListItems(filtered)
            val currAdapter = binding.searchResultsList.adapter
            if (currAdapter == null) {
                val eventsAdapter = EventListAdapter(this, listItems, true, this, binding.searchResultsList) {
                    hideKeyboard()
                    if (it is ListEvent) {
                        Intent(applicationContext, getActivityToOpen(it.isTask)).apply {
                            putExtra(EVENT_ID, it.id)
                            putExtra(EVENT_OCCURRENCE_TS, it.startTS)
                            startActivity(this)
                        }
                    }
                }

                binding.searchResultsList.adapter = eventsAdapter

                binding.searchResultsList.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {
                        fetchPreviousPeriod()
                    }

                    override fun updateBottom() {
                        fetchNextPeriod()
                    }
                }
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (updateStatus == UPDATE_TOP) {
                    val item = listItems.indexOfFirst { it == bottomItemAtRefresh }
                    if (item != -1) {
                        binding.searchResultsList.scrollToPosition(item)
                    }
                } else if (updateStatus == UPDATE_BOTTOM) {
                    binding.searchResultsList.smoothScrollBy(0, resources.getDimension(R.dimen.endless_scroll_move_height).toInt())
                } else {
                    val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
                    if (firstNonPastSectionIndex != -1) {
                        binding.searchResultsList.scrollToPosition(firstNonPastSectionIndex)
                    }
                }
            }
        }
    }

    private fun fetchPreviousPeriod() {
        if (minFetchedSearchTS == 0L) {
            return
        }

        val lastPosition = (binding.searchResultsList.layoutManager as MyLinearLayoutManager).findLastVisibleItemPosition()
        bottomItemAtRefresh = (binding.searchResultsList.adapter as EventListAdapter).listItems[lastPosition]

        val oldMinFetchedTS = minFetchedSearchTS - 1
        minFetchedSearchTS -= FETCH_INTERVAL
        eventsHelper.getEvents(minFetchedSearchTS, oldMinFetchedTS, searchQuery = mLatestSearchQuery) { events ->
            events.forEach { event ->
                try {
                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                        searchResultEvents.add(0, event)
                    }
                } catch (ignored: ConcurrentModificationException) {
                }
            }

            showSearchResultEvents(searchResultEvents, UPDATE_TOP)
        }
    }

    private fun fetchNextPeriod() {
        if (maxFetchedSearchTS == MAX_SEARCH_YEAR) {
            return
        }

        val oldMaxFetchedTS = maxFetchedSearchTS + 1
        maxFetchedSearchTS += FETCH_INTERVAL
        eventsHelper.getEvents(oldMaxFetchedTS, maxFetchedSearchTS, searchQuery = mLatestSearchQuery) { events ->
            events.forEach { event ->
                try {
                    if (searchResultEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                        searchResultEvents.add(0, event)
                    }
                } catch (ignored: ConcurrentModificationException) {
                }
            }

            showSearchResultEvents(searchResultEvents, UPDATE_BOTTOM)
        }
    }

    private fun checkSwipeRefreshAvailability() {
        binding.swipeRefreshLayout.isEnabled = config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!binding.swipeRefreshLayout.isEnabled) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun refreshItems() {
        refreshViewPager()
    }

    fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS(timestamp / 1000L)
        binding.calendarFab.beVisible()
        config.storedView = DAILY_VIEW
        updateViewPager(dayCode)
    }

    // events fetched from Thunderbird, https://www.thunderbird.net/en-US/calendar/holidays and
    // https://holidays.kayaposoft.com/public_holidays.php?year=2021
    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("Australia", "australia.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
            put("България", "bulgaria.ics")
            put("Canada", "canada.ics")
            put("China", "china.ics")
            put("Colombia", "colombia.ics")
            put("Česká republika", "czech.ics")
            put("Danmark", "denmark.ics")
            put("Deutschland", "germany.ics")
            put("Eesti", "estonia.ics")
            put("España", "spain.ics")
            put("Éire", "ireland.ics")
            put("France", "france.ics")
            put("Fürstentum Liechtenstein", "liechtenstein.ics")
            put("Hellas", "greece.ics")
            put("Hrvatska", "croatia.ics")
            put("India", "india.ics")
            put("Indonesia", "indonesia.ics")
            put("Ísland", "iceland.ics")
            put("Israel", "israel.ics")
            put("Italia", "italy.ics")
            put("Қазақстан Республикасы", "kazakhstan.ics")
            put("المملكة المغربية", "morocco.ics")
            put("Latvija", "latvia.ics")
            put("Lietuva", "lithuania.ics")
            put("Luxemburg", "luxembourg.ics")
            put("Makedonija", "macedonia.ics")
            put("Malaysia", "malaysia.ics")
            put("Magyarország", "hungary.ics")
            put("México", "mexico.ics")
            put("Nederland", "netherlands.ics")
            put("República de Nicaragua", "nicaragua.ics")
            put("日本", "japan.ics")
            put("Nigeria", "nigeria.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("República de Costa Rica", "costarica.ics")
            put("República Oriental del Uruguay", "uruguay.ics")
            put("République d'Haïti", "haiti.ics")
            put("România", "romania.ics")
            put("Schweiz", "switzerland.ics")
            put("Singapore", "singapore.ics")
            put("한국", "southkorea.ics")
            put("Srbija", "serbia.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("South Africa", "southafrica.ics")
            put("Sri Lanka", "srilanka.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
            put("Taiwan", "taiwan.ics")
            put("ราชอาณาจักรไทย", "thailand.ics")
            put("Türkiye Cumhuriyeti", "turkey.ics")
            put("Ukraine", "ukraine.ics")
            put("United Kingdom", "unitedkingdom.ics")
            put("United States", "unitedstates.ics")

            var i = 0
            for ((country, file) in this) {
                items.add(RadioItem(i++, country, file))
            }
        }

        return items
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.mainToolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                // Settings at the top
                R.id.nav_settings -> {
                    launchSettings()
                    true
                }
                
                // About section
                R.id.nav_about -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "about.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "About")
                    startActivity(intent)
                    true
                }
                
                // PRAXIS section
                R.id.nav_praxis_beginners_guide -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "praxis_beginners_guide.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Beginners Guide")
                    startActivity(intent)
                    true
                }
                R.id.nav_praxis_wight_listing -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "praxis_wight_listing.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Wight Listing")
                    startActivity(intent)
                    true
                }
                
                // HOLIDAYS section - Seasonal Groups
                R.id.nav_yule -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "yule_combined.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Yule")
                    startActivity(intent)
                    true
                }
                R.id.nav_summermal -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "summermal_combined.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Summermal")
                    startActivity(intent)
                    true
                }
                R.id.nav_midsummer -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "midsummer_combined.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Midsummer")
                    startActivity(intent)
                    true
                }
                R.id.nav_winter_nights -> {
                    val intent = Intent(this, PoeticEddaActivity::class.java)
                    intent.putExtra(PoeticEddaActivity.EXTRA_TEXT_FILE, "winternights_combined.txt")
                    intent.putExtra(PoeticEddaActivity.EXTRA_TITLE, "Winter Nights")
                    startActivity(intent)
                    true
                }
                
                // LORE section
                R.id.nav_lore_poetic_edda -> {
                    startActivity(Intent(this, PoeticEddaActivity::class.java))
                    true
                }
                
                else -> false
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    
    private fun showPlaceholderActivity(title: String) {
        val intent = Intent(this, PlaceholderActivity::class.java)
        intent.putExtra("title", title)
        startActivity(intent)
    }
    

}
