package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.helpers.Config
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LunisolarCalendar
import com.simplemobiletools.calendar.pro.helpers.LunisolarHolidays
import com.simplemobiletools.calendar.pro.helpers.MONTHLY_VIEW
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.eventsDB
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import org.joda.time.DateTime

class LunisolarMonthFragment : MyFragmentHolder() {
    override val viewType = MONTHLY_VIEW
    
    private var currentLunarYear = 2025
    private var currentLunarMonth = 1
    
    private lateinit var monthTitle: TextView
    private lateinit var prevButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var todayButton: TextView
    private lateinit var calendarGrid: LinearLayout
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lunisolar_month, container, false)
        
        monthTitle = view.findViewById(R.id.month_title)
        prevButton = view.findViewById(R.id.prev_month)
        nextButton = view.findViewById(R.id.next_month)
        todayButton = view.findViewById(R.id.today_button)
        calendarGrid = view.findViewById(R.id.calendar_grid)
        
        // Apply theme colors
        val textColor = requireContext().getProperTextColor()
        val primaryColor = requireContext().getProperPrimaryColor()
        
        monthTitle.setTextColor(textColor)
        todayButton.setTextColor(textColor)
        prevButton.applyColorFilter(textColor)
        nextButton.applyColorFilter(textColor)
        
        // Initialize with current date
        initializeCurrentMonth()
        
        prevButton.setOnClickListener { navigateToPreviousMonth() }
        nextButton.setOnClickListener { navigateToNextMonth() }
        todayButton.setOnClickListener { goToToday() }
        monthTitle.setOnClickListener {
            // Debug: Log lunar to gregorian conversion for this month
            val debug = LunisolarCalendar.debugLunarToGregorian(currentLunarYear, currentLunarMonth)
            android.util.Log.d("LunisolarDebug", debug)
        }
        
        updateDisplay()
        return view
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh display in case settings changed
        updateDisplay()
    }
    
    // Required by MyFragmentHolder
    override fun shouldGoToTodayBeVisible() = true
    override fun refreshEvents() {}
    override fun goToToday() {
        initializeCurrentMonth()
        updateDisplay()
    }
    override fun showGoToDateDialog() {}
    override fun getNewEventDayCode(): String {
        val firstDay = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, 1)
        return if (firstDay != null) {
            String.format("%04d%02d%02d", firstDay.first, firstDay.second, firstDay.third)
        } else {
            Formatter.getTodayCode()
        }
    }
    override fun printView() {}
    override fun getCurrentDate(): DateTime? {
        val firstDay = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, 1)
        return if (firstDay != null) {
            DateTime(firstDay.first, firstDay.second, firstDay.third, 0, 0)
        } else {
            DateTime.now()
        }
    }
    
    private fun initializeCurrentMonth() {
        val today = DateTime.now()
        val lunarDate = LunisolarCalendar.gregorianToLunar(today.year, today.monthOfYear, today.dayOfMonth)
        currentLunarYear = lunarDate.lunarYear
        currentLunarMonth = lunarDate.lunarMonth
    }
    
    private fun navigateToPreviousMonth() {
        currentLunarMonth--
        if (currentLunarMonth < 1) {
            currentLunarYear--
            currentLunarMonth = LunisolarCalendar.getLunarMonthsInYear(currentLunarYear)
        }
        updateDisplay()
    }
    
    private fun navigateToNextMonth() {
        val monthsInCurrentYear = LunisolarCalendar.getLunarMonthsInYear(currentLunarYear)
        currentLunarMonth++
        if (currentLunarMonth > monthsInCurrentYear) {
            currentLunarYear++
            currentLunarMonth = 1
        }
        updateDisplay()
    }
    
    private fun updateDisplay() {
        updateTitle()
        buildCalendarGrid()
    }
    
    private fun updateTitle() {
        val config = Config(requireContext())
        val monthNames = config.lunisolarMonthNames.split(",")
        val epoch = config.lunisolarEpoch
        val useEldYears = config.useEldYears
        
        val monthName = if (monthNames.isNotEmpty() && currentLunarMonth <= monthNames.size) {
            monthNames[currentLunarMonth - 1]
        } else {
            "Month $currentLunarMonth"
        }
        
        val yearDisplay = if (useEldYears) {
            val eldYear = currentLunarYear + epoch
            "$eldYear Eld"
        } else {
            "$currentLunarYear AD"
        }
        
        monthTitle.text = "$monthName $yearDisplay"
    }
    
    private fun buildCalendarGrid() {
        calendarGrid.removeAllViews()
        
        // Add weekday headers
        addWeekdayHeaders()
        
        // Get month data
        val monthLength = LunisolarCalendar.getLunarMonthLength(currentLunarYear, currentLunarMonth)
        val firstDayGregorian = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, 1)
        
        if (firstDayGregorian == null) return
        
        val firstDayJD = LunisolarCalendar.gregorianToJulianDay(firstDayGregorian.first, firstDayGregorian.second, firstDayGregorian.third)
        val firstDayOfWeek = LunisolarCalendar.calculateWeekday(firstDayGregorian.first, firstDayGregorian.second, firstDayGregorian.third)
        val firstDayIndex = firstDayOfWeek.ordinal // 0=Sunday, 1=Monday, etc.
        
        // Always use 42-day grid (6 weeks x 7 days)
        val gridSize = 42
        
        // Pre-fetch all holidays for the years to avoid repeated calculations
        val currentYearHolidays = LunisolarHolidays.getHolidaysForYear(firstDayGregorian.first)
        val prevYearHolidays = LunisolarHolidays.getHolidaysForYear(firstDayGregorian.first - 1)
        val nextYearHolidays = LunisolarHolidays.getHolidaysForYear(firstDayGregorian.first + 1)
        val allHolidays = currentYearHolidays + prevYearHolidays + nextYearHolidays
        
        val currentYearAstroEvents = LunisolarHolidays.getAstronomicalEventsForYear(firstDayGregorian.first)
        val prevYearAstroEvents = LunisolarHolidays.getAstronomicalEventsForYear(firstDayGregorian.first - 1)
        val nextYearAstroEvents = LunisolarHolidays.getAstronomicalEventsForYear(firstDayGregorian.first + 1)
        val allAstroEvents = currentYearAstroEvents + prevYearAstroEvents + nextYearAstroEvents

        // Calculate previous and next month data for preview
        val (prevYear, prevMonth) = if (currentLunarMonth == 1) {
            Pair(currentLunarYear - 1, LunisolarCalendar.getLunarMonthsInYear(currentLunarYear - 1))
        } else {
            Pair(currentLunarYear, currentLunarMonth - 1)
        }
        
        val (nextYear, nextMonth) = if (currentLunarMonth == LunisolarCalendar.getLunarMonthsInYear(currentLunarYear)) {
            Pair(currentLunarYear + 1, 1)
        } else {
            Pair(currentLunarYear, currentLunarMonth + 1)
        }
        
        val prevMonthLength = LunisolarCalendar.getLunarMonthLength(prevYear, prevMonth)

        // Build grid week by week
        var currentWeek = LinearLayout(requireContext())
        currentWeek.orientation = LinearLayout.HORIZONTAL
        currentWeek.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        
        for (i in 0 until gridSize) {
            if (i > 0 && i % 7 == 0) {
                calendarGrid.addView(currentWeek)
                currentWeek = LinearLayout(requireContext())
                currentWeek.orientation = LinearLayout.HORIZONTAL
                currentWeek.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            
            val dayView = createDayView(i, firstDayIndex, monthLength, firstDayJD, allHolidays, allAstroEvents, 
                                     prevYear, prevMonth, prevMonthLength, nextYear, nextMonth)
            currentWeek.addView(dayView)
        }
        
        if (currentWeek.childCount > 0) {
            calendarGrid.addView(currentWeek)
        }
    }
    
    private fun addWeekdayHeaders() {
        val headerRow = LinearLayout(requireContext())
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        val textColor = requireContext().getProperTextColor()
        val weekdays = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (day in weekdays) {
            val headerView = TextView(requireContext())
            headerView.text = day
            headerView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            headerView.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                1f
            )
            headerView.setPadding(4, 8, 4, 8)
            headerView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            headerView.setTextColor(textColor)
            headerView.setTypeface(null, android.graphics.Typeface.BOLD)
            
            // Use theme-appropriate background
            headerView.setBackgroundColor(requireContext().getProperPrimaryColor())
            
            headerRow.addView(headerView)
        }
        
        calendarGrid.addView(headerRow)
    }
    
    private fun createDayView(
        cellIndex: Int,
        firstDayIndex: Int,
        monthLength: Int,
        firstDayJD: Double,
        holidays: List<LunisolarHolidays.Holiday>,
        astroEvents: List<LunisolarHolidays.Holiday>,
        prevYear: Int,
        prevMonth: Int, 
        prevMonthLength: Int,
        nextYear: Int,
        nextMonth: Int
    ): TextView {
        val dayView = TextView(requireContext())
        dayView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )
        dayView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        dayView.setPadding(4, 8, 4, 8)
        dayView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
        dayView.minHeight = 80 // Minimum height for event dots
        
        val textColor = requireContext().getProperTextColor()
        val backgroundColor = requireContext().getProperPrimaryColor()
        
        var gregorianDate: Triple<Int, Int, Int>? = null
        var displayText = ""
        var isCurrentMonth = false
        var isToday = false
        var lunarDay = 0
        var holiday: LunisolarHolidays.Holiday? = null
        var astroEvent: LunisolarHolidays.Holiday? = null
        
        when {
            cellIndex < firstDayIndex -> {
                // Previous month days
                val prevDayNum = prevMonthLength - (firstDayIndex - cellIndex - 1)
                displayText = prevDayNum.toString()
                dayView.alpha = 0.4f
                gregorianDate = LunisolarCalendar.lunarToGregorian(prevYear, prevMonth, prevDayNum)
            }
            cellIndex >= firstDayIndex + monthLength -> {
                // Next month days
                val nextDayNum = cellIndex - firstDayIndex - monthLength + 1
                displayText = nextDayNum.toString()
                dayView.alpha = 0.4f
                gregorianDate = LunisolarCalendar.lunarToGregorian(nextYear, nextMonth, nextDayNum)
            }
            else -> {
                // Current month days
                lunarDay = cellIndex - firstDayIndex + 1
                displayText = lunarDay.toString()
                isCurrentMonth = true
                dayView.alpha = 1.0f
                gregorianDate = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, lunarDay)
            }
        }
        
        dayView.setTextColor(textColor)
        
        if (gregorianDate != null) {
            val currentDate = DateTime(gregorianDate.first, gregorianDate.second, gregorianDate.third, 0, 0)
            
            // Check if today
            val today = DateTime.now()
            isToday = gregorianDate.first == today.year && 
                     gregorianDate.second == today.monthOfYear && 
                     gregorianDate.third == today.dayOfMonth
            
            // Find holidays (check all 3 days for multi-day holidays)
            holiday = holidays.find { holidayEvent ->
                val day1 = holidayEvent.startDate.toLocalDate()
                val day2 = holidayEvent.startDate.plusDays(1).toLocalDate()
                val day3 = holidayEvent.startDate.plusDays(2).toLocalDate()
                currentDate.toLocalDate() == day1 || currentDate.toLocalDate() == day2 || currentDate.toLocalDate() == day3
            }
            
            // Find astronomical events
            astroEvent = astroEvents.find { it.startDate.toLocalDate() == currentDate.toLocalDate() }
            
            // Check for events using the event system
            val dayCode = Formatter.getDayCodeFromDateTime(currentDate)
            val hasEvents = false // TODO: Implement proper event checking integration with lunisolar calendar
            
            // Apply styling based on priority: today > holiday > astro event > normal
            when {
                isToday -> {
                    dayView.setBackgroundColor(requireContext().getProperPrimaryColor())
                    dayView.setTextColor(0xFFFFFFFF.toInt())
                }
                holiday != null -> {
                    val holidayColor = when (holiday.abbreviation) {
                        "YUL" -> 0xFF4CAF50.toInt() // Green for Yule
                        "SUM" -> 0xFFFF9800.toInt() // Orange for Sumarmal  
                        "MID" -> 0xFFFFEB3B.toInt() // Yellow for Midsummer
                        "WIN" -> 0xFF3F51B5.toInt() // Blue for Winter Nights
                        else -> 0xFFE91E63.toInt()  // Pink for other holidays
                    }
                    dayView.setBackgroundColor(holidayColor)
                    dayView.setTextColor(0xFFFFFFFF.toInt())
                }
                astroEvent != null -> {
                    dayView.setBackgroundColor(0xFF9C27B0.toInt()) // Purple for astronomical events
                    dayView.setTextColor(0xFFFFFFFF.toInt())
                }
            }
            
            // Create compound text with day number, holiday info, and event indicator
            var fullText = displayText
            
            // Add holiday identifier if present
            if (holiday != null) {
                val holidayDayNumber = LunisolarHolidays.getHolidayDayNumber(currentDate) ?: 1
                fullText += "\n${holiday.abbreviation}$holidayDayNumber"
            }
            
            // Add astronomical event identifier if present  
            if (astroEvent != null) {
                fullText += "\n${astroEvent.abbreviation}"
            }
            
            // Add event dot indicator if has events
            if (hasEvents) {
                fullText += "\n●" // Add dot indicator for events
            }
            
            dayView.text = fullText
        } else {
            dayView.text = displayText
        }
        
        return dayView
    }
} 