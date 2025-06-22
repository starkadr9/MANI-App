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
import com.simplemobiletools.calendar.pro.helpers.MONTHLY_VIEW
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
    private lateinit var calendarGrid: LinearLayout
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lunisolar_month, container, false)
        
        monthTitle = view.findViewById(R.id.month_title)
        prevButton = view.findViewById(R.id.prev_month)
        nextButton = view.findViewById(R.id.next_month)
        calendarGrid = view.findViewById(R.id.calendar_grid)
        
        // Apply theme colors
        val textColor = requireContext().getProperTextColor()
        val primaryColor = requireContext().getProperPrimaryColor()
        
        monthTitle.setTextColor(textColor)
        prevButton.applyColorFilter(textColor)
        nextButton.applyColorFilter(textColor)
        
        // Initialize with current date
        initializeCurrentMonth()
        
        prevButton.setOnClickListener { navigateToPreviousMonth() }
        nextButton.setOnClickListener { navigateToNextMonth() }
        monthTitle.setOnClickListener { 
            (activity as? MainActivity)?.showGoToDateDialog()
        }
        
        updateDisplay()
        return view
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
        // Get current Gregorian date and convert to lunar
        val now = org.joda.time.DateTime.now()
        val lunar = LunisolarCalendar.gregorianToLunar(now.year, now.monthOfYear, now.dayOfMonth)
        if (lunar.lunarDay > 0) {
            currentLunarYear = lunar.lunarYear
            currentLunarMonth = lunar.lunarMonth
        }
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
        
        val monthName = if (monthNames.isNotEmpty() && currentLunarMonth <= monthNames.size) {
            monthNames[currentLunarMonth - 1].trim()
        } else {
            "Month $currentLunarMonth"
        }
        
        val eldYear = currentLunarYear + epoch.toLong()
        monthTitle.text = "$monthName $eldYear Eld"
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
        
        // Calculate grid size
        val totalCells = monthLength + firstDayIndex
        val weeksNeeded = (totalCells + 6) / 7 // Round up
        val gridSize = weeksNeeded * 7
        
        // Build grid week by week
        var currentWeek = LinearLayout(requireContext())
        currentWeek.orientation = LinearLayout.HORIZONTAL
        currentWeek.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        for (i in 0 until gridSize) {
            if (i > 0 && i % 7 == 0) {
                calendarGrid.addView(currentWeek)
                currentWeek = LinearLayout(requireContext())
                currentWeek.orientation = LinearLayout.HORIZONTAL
                currentWeek.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            val dayView = createDayView(i, firstDayIndex, monthLength, firstDayJD)
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
                80, // Fixed height for headers
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
    
    private fun createDayView(cellIndex: Int, firstDayIndex: Int, monthLength: Int, firstDayJD: Double): TextView {
        val dayView = TextView(requireContext())
        dayView.layoutParams = LinearLayout.LayoutParams(
            0,
            120, // Smaller height for better display
            1f
        )
        dayView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        dayView.setPadding(2, 2, 2, 2)
        dayView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
        
        val textColor = requireContext().getProperTextColor()
        val backgroundColor = requireContext().getProperPrimaryColor()
        
        if (cellIndex < firstDayIndex) {
            // Previous month days
            dayView.text = ""
            dayView.alpha = 0.3f
            dayView.setBackgroundColor(backgroundColor and 0x20FFFFFF.toInt()) // Very light version
        } else if (cellIndex >= firstDayIndex + monthLength) {
            // Next month days  
            dayView.text = ""
            dayView.alpha = 0.3f
            dayView.setBackgroundColor(backgroundColor and 0x20FFFFFF.toInt()) // Very light version
        } else {
            // Current month days
            val lunarDay = cellIndex - firstDayIndex + 1
            dayView.alpha = 1.0f
            dayView.setTextColor(textColor)
            
            // Add moon phase symbol at start of month (full moon)
            if (lunarDay == 1) {
                dayView.text = "🌕\n$lunarDay"
                dayView.setBackgroundColor(0xFFE3F2FD.toInt()) // Light blue background
            } else {
                dayView.text = lunarDay.toString()
                dayView.setBackgroundColor(backgroundColor and 0x10FFFFFF.toInt()) // Very light background
            }
            
            // Add holiday highlighting for major holidays (1st, 3rd, 6th, 9th months)
            if (lunarDay <= 3) { // First 3 days of month
                when (currentLunarMonth) {
                    1 -> {
                        dayView.setBackgroundColor(0xFF4CAF50.toInt()) // Green for Yule
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nYUL$lunarDay" else "${lunarDay}\nYUL$lunarDay"
                        dayView.setTextColor(0xFFFFFFFF.toInt()) // White text
                    }
                    3 -> {
                        dayView.setBackgroundColor(0xFFFF9800.toInt()) // Orange for Sumarmal
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nSUM$lunarDay" else "${lunarDay}\nSUM$lunarDay"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                    6 -> {
                        dayView.setBackgroundColor(0xFFFFEB3B.toInt()) // Yellow for Midsummer
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nMID$lunarDay" else "${lunarDay}\nMID$lunarDay"
                        dayView.setTextColor(0xFF000000.toInt()) // Black text
                    }
                    9 -> {
                        dayView.setBackgroundColor(0xFF2196F3.toInt()) // Blue for Winter Nights
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nWIN$lunarDay" else "${lunarDay}\nWIN$lunarDay"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                }
            }
            
            // Add solstice/equinox highlighting
            val gregorianDate = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, lunarDay)
            if (gregorianDate != null) {
                val jd = LunisolarCalendar.gregorianToJulianDay(gregorianDate.first, gregorianDate.second, gregorianDate.third)
                val winterSolsticeJD = LunisolarCalendar.calculateWinterSolstice(gregorianDate.first)
                val springEquinoxJD = LunisolarCalendar.calculateSpringEquinox(gregorianDate.first)
                val summerSolsticeJD = LunisolarCalendar.calculateSummerSolstice(gregorianDate.first)
                val fallEquinoxJD = LunisolarCalendar.calculateFallEquinox(gregorianDate.first)
                
                when {
                    kotlin.math.abs(jd - winterSolsticeJD) < 0.5 -> {
                        dayView.setBackgroundColor(0xFF4CAF50.toInt()) // Green for winter solstice
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nWS" else "${lunarDay}\nWS"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                    kotlin.math.abs(jd - springEquinoxJD) < 0.5 -> {
                        dayView.setBackgroundColor(0xFF9C27B0.toInt()) // Purple for spring equinox
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nSE" else "${lunarDay}\nSE"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                    kotlin.math.abs(jd - summerSolsticeJD) < 0.5 -> {
                        dayView.setBackgroundColor(0xFFFF9800.toInt()) // Orange for summer solstice
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nSS" else "${lunarDay}\nSS"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                    kotlin.math.abs(jd - fallEquinoxJD) < 0.5 -> {
                        dayView.setBackgroundColor(0xFF795548.toInt()) // Brown for fall equinox
                        dayView.text = if (lunarDay == 1) "🌕\n${lunarDay}\nFE" else "${lunarDay}\nFE"
                        dayView.setTextColor(0xFFFFFFFF.toInt())
                    }
                }
            }
            
            // Add click functionality to navigate to day view
            dayView.setOnClickListener {
                val gregorianDate = LunisolarCalendar.lunarToGregorian(currentLunarYear, currentLunarMonth, lunarDay)
                if (gregorianDate != null) {
                    // Switch to daily view for the selected day
                    val mainActivity = activity as? MainActivity
                    mainActivity?.let { mainActivity ->
                        val dateTime = DateTime(gregorianDate.first, gregorianDate.second, gregorianDate.third, 0, 0)
                        mainActivity.openDayFromMonthly(dateTime)
                    }
                }
            }
            
            // Add border using theme colors
            val bgColor = when {
                lunarDay <= 3 && currentLunarMonth == 1 -> 0xFF4CAF50.toInt()
                lunarDay <= 3 && currentLunarMonth == 3 -> 0xFFFF9800.toInt()
                lunarDay <= 3 && currentLunarMonth == 6 -> 0xFFFFEB3B.toInt()
                lunarDay <= 3 && currentLunarMonth == 9 -> 0xFF2196F3.toInt()
                lunarDay == 1 -> 0xFFE3F2FD.toInt()
                else -> backgroundColor and 0x10FFFFFF.toInt()
            }
            
            dayView.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                setStroke(1, textColor and 0x40FFFFFF.toInt()) // Semi-transparent border
            }
        }
        
        return dayView
    }
} 