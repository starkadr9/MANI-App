package com.simplemobiletools.calendar.pro.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.pro.databinding.FragmentMonthsHolderBinding
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.getMonthCode
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LunisolarCalendar
import com.simplemobiletools.calendar.pro.helpers.MONTHLY_VIEW
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyViewPager
import org.joda.time.DateTime

class MonthFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private lateinit var viewPager: MyViewPager
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = MONTHLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMonthsHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentMonthsViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getMonths(currentDayCode)
        val monthlyAdapter = MyMonthPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        
        // Find the correct center position for lunar calendar
        var centerPosition = codes.size / 2 // Default fallback
        
        if (requireContext().config.useLunisolarCalendar) {
            val startDate = Formatter.getDateTimeFromCode(currentDayCode)
            val targetLunar = LunisolarCalendar.gregorianToLunar(startDate.year, startDate.monthOfYear, startDate.dayOfMonth)
            
            if (targetLunar.lunarDay != 0) {
                // Find which position in our generated list corresponds to the current lunar month
                for (i in codes.indices) {
                    val codeDate = Formatter.getDateTimeFromCode(codes[i])
                    val codeLunar = LunisolarCalendar.gregorianToLunar(codeDate.year, codeDate.monthOfYear, codeDate.dayOfMonth)
                    
                    if (codeLunar.lunarYear == targetLunar.lunarYear && codeLunar.lunarMonth == targetLunar.lunarMonth) {
                        centerPosition = i
                        break
                    }
                }
            }
        }
        
        defaultMonthlyPage = centerPosition

        viewPager.apply {
            adapter = monthlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultMonthlyPage
        }
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        
        if (requireContext().config.useLunisolarCalendar) {
            // Lunisolar navigation: Year-based with proper month counting
            val startDate = Formatter.getDateTimeFromCode(code)
            val centerLunar = LunisolarCalendar.gregorianToLunar(startDate.year, startDate.monthOfYear, startDate.dayOfMonth)
            
            if (centerLunar.lunarDay != 0) {
                // Start from several years before center to fill the ViewPager
                val startYear = centerLunar.lunarYear - 15
                val endYear = centerLunar.lunarYear + 15
                var centerIndex = -1
                
                // Generate months year by year with proper counting
                for (lunarYear in startYear..endYear) {
                    val monthsInThisYear = LunisolarCalendar.getLunarMonthsInYear(lunarYear)
                    
                    // Iterate through actual months in this year (1 to 12 or 1 to 13)
                    for (lunarMonth in 1..monthsInThisYear) {
                        // Get the first day of this specific lunar month
                        val firstDay = LunisolarCalendar.lunarToGregorian(lunarYear, lunarMonth, 1)
                        if (firstDay != null) {
                            val dayCode = String.format("%04d%02d%02d", firstDay.first, firstDay.second, firstDay.third)
                            months.add(dayCode)
                            
                            // Track center position for current lunar year/month
                            if (lunarYear == centerLunar.lunarYear && lunarMonth == centerLunar.lunarMonth) {
                                centerIndex = months.size - 1
                            }
                        }
                        
                        // Stop when we have enough months
                        if (months.size >= PREFILLED_MONTHS) break
                    }
                    
                    if (months.size >= PREFILLED_MONTHS) break
                }
                
                // Set proper center position
                defaultMonthlyPage = if (centerIndex >= 0) centerIndex else months.size / 2
                return months
            }
        }
        
        // Fallback to Gregorian months
        val today = Formatter.getDateTimeFromCode(code).withDayOfMonth(1)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }
        return months
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = getCurrentDate()!!
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> datePicked(dateTime, datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode.getMonthCode() != todayDayCode.getMonthCode()

    override fun getNewEventDayCode() = if (shouldGoToTodayBeVisible()) currentDayCode else todayDayCode

    override fun printView() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            DateTime(Formatter.getDateTimeFromCode(currentDayCode).toString())
        } else {
            null
        }
    }
}
